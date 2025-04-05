package org.skgroup.CodeAuditAssistant.utils

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.skgroup.CodeAuditAssistant.analysis.ast.SourceSpan
import org.skgroup.CodeAuditAssistant.analysis.ast.nodes.MethodNode
import org.skgroup.CodeAuditAssistant.analysis.ast.nodes.MethodSigNode
import org.skgroup.CodeAuditAssistant.analysis.ast.nodes.ParameterNode
import org.skgroup.CodeAuditAssistant.analysis.graphs.callgraph.CallGraph
import org.skgroup.CodeAuditAssistant.enums.RefMode
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Graph utils 是一些用于处理图的工具方法
 * @author springkill
 */
object GraphUtils {

    private val methodNodeCache = mutableMapOf<String, MethodNode>()

    private val PsiMethod.className: String
        get() = containingClass?.qualifiedName ?: "UnknownClass"

    private val PsiMethod.returnTypeText: String
        get() = returnType?.canonicalText ?: "void"

    private fun PsiParameter.toParameterNode() = ParameterNode(
        name = name ?: "unknown",
        type = type.canonicalText
    )

    /**
     * Get method signature 方法用来获取方法的签名
     * 例如：com.example.Foo.bar(int, java.lang.String)
     *
     * @param method 传入一个 PsiMethod 对象
     * @return  返回一个字符串，表示方法的签名
     */
    fun getMethodSignature(method: PsiMethod): String {
        val className = method.containingClass?.qualifiedName ?: "UnknownClass"
        val methodName = method.name

        // 获取方法的参数列表
        val parameterTypes = method.parameterList.parameters.joinToString(separator = ", ") { param ->
            param.type.canonicalText
        }

        return "$className#$methodName($parameterTypes)"
    }

    /**
     * Get method signature 方法用来获取方法的签名
     *
     * @param methodNode 传入一个 MethodNode 对象
     * @return  返回一个字符串，表示方法的签名
     */
    private fun getMethodSignature(methodNode: MethodNode): String {
        val parameterTypes = methodNode.parameters.joinToString(separator = ", ") { it.type }
        return "${methodNode.className}#${methodNode.name}($parameterTypes)"
    }


    /**
     * Get method node 方法用来获取方法节点，位置为方法本身
     *
     * @param method 传入一个 PsiMethod 对象
     * @return 返回一个 MethodNode 对象
     */
    fun getMethodNode(method: PsiMethod) = createMethodNode(
        method = method,
        sourceSpan = getSourceSpan(method),
        refMode = RefMode.DECLARATION
    )

    /**
     * Get method node 方法用来获取方法节点，带引用位置
     *
     * @param method 传入一个 PsiMethod 对象
     * @param reference 方法引用
     * @return 返回一个 MethodNode 对象
     */
    fun getMethodNode(method: PsiMethod, reference: PsiReference) = createMethodNode(
        method = method,
        sourceSpan = getSourceSpan(reference.element),
        refMode = RefMode.CALL
    )

    fun getNewMethodNode(method: PsiMethod, reference: PsiReference) = createMethodNode(
        method = method,
        sourceSpan = getSourceSpan(reference.element),
        refMode = RefMode.NEW
    )

    /**
     * Get method node 方法用来获取方法节点，引用位置通过表达式获取
     *
     * @param method 传入一个 PsiMethod 对象
     * @param expression 表达式用于获取位置
     * @return 返回一个 MethodNode 对象
     */
    fun getMethodNode(method: PsiMethod, expression: PsiExpression) = createMethodNode(
        method = method,
        sourceSpan = getSourceSpan(expression),
        refMode = RefMode.CALL
    )

    fun getImplNode(method: PsiMethod, expression: PsiExpression) = createMethodNode(
        method = method,
        sourceSpan = getSourceSpan(expression),
        refMode = RefMode.IMPLEMENTATION
    )

    fun getMethodNode(method: PsiMethod, expression: PsiNewExpression) = createMethodNode(
        method = method,
        sourceSpan = getSourceSpan(expression),
        refMode = RefMode.NEW
    )

    /**
     * Create method node 方法用来创建一个方法节点
     *
     * @param method 传入一个 PsiMethod 对象
     * @param sourceSpan 传入一个 SourceSpan 对象
     * @param refMode 传入一个 RefMode 对象
     * @return 返回一个 MethodNode 对象
     */
    private fun createMethodNode(
        method: PsiMethod,
        sourceSpan: SourceSpan,
        refMode: RefMode
    ): MethodNode {
        val parameters = method.parameterList.parameters.map { it.toParameterNode() }
        val node = MethodNode(
            className = method.className,
            name = method.name,
            returnType = method.returnTypeText,
            parameters = parameters,
            sourceSpan = sourceSpan,
            refMode = refMode
        )
        return methodNodeCache.getOrPut(getMethodSignature(node)) { node }
    }

    /**
     * Get source span 方法用来获取源码位置信息
     *
     * @param element 传入一个 PsiElement 对象
     * @return 返回一个 SourceSpan 对象
     */
    private fun getSourceSpan(element: PsiElement): SourceSpan {
        val containingFile = element.containingFile
        val virtualFile = containingFile.virtualFile
        val offset = element.textOffset
        return SourceSpan(containingFile, virtualFile, offset)
    }

    /**
     * Get method node 方法用来获取方法节点
     * 用于将 PsiClass 映射为一个 MethodNode。
     * 这样在 DIProcessor 或者其他地方需要把类加入调用图时，可以保持类型统一。
     *
     *
     * @param clazz 传入一个 PsiClass 对象
     * @return 返回一个 MethodNode 对象
     */
    fun getClassNode(clazz: PsiClass): MethodNode {
        val signature = clazz.qualifiedName ?: clazz.name ?: "UnknownClass"
        return methodNodeCache.getOrPut("[DI]$signature") {
            MethodNode(
                className = signature,
                name = "class",
                returnType = "void",
                parameters = emptyList(),
                sourceSpan = getSourceSpan(clazz),
                refMode = RefMode.DECLARATION
            )
        }
    }

    /**
     * 获取 lambda 表达式对应的方法节点
     *
     * @param method 传入一个 PsiMethod 对象
     * @return 返回一个 MethodNode 对象
     */
    fun getLambdaMethodNode(method: PsiMethod): MethodNode {
        val methodNode = getMethodNode(method)
        val signature = "[Lambda]${getMethodSignature(methodNode)}"
        return methodNodeCache.getOrPut(signature) {
            methodNode.copy(name = "lambda_${methodNode.name}")
        }
    }

    /**
     * Find path 方法用来查找两个方法之间的调用路径（DFS查找）
     *
     * @param graph 传入一个 CallGraph 对象
     * @param node 传入一个 MethodNode 对象
     * @param target 传入一个 MethodNode 对象
     * @return 返回一个 List<MethodNode> 对象
     */
    fun findPath(graph: CallGraph, node: MethodNode, target: MethodNode): List<MethodNode>? {
        val visited = mutableSetOf<MethodNode>()
        val path = mutableListOf<MethodNode>()

        fun dfs(cur: MethodNode): Boolean {
            visited.add(cur)
            path.add(cur)
            if (cur.signature == target.signature) return true

            graph.edges[cur]?.forEach { neighbor ->
                if (neighbor !in visited && dfs(neighbor)) return true
            }

            path.removeLast()
            return false
        }

        return if (dfs(node)) path else null
    }

    /**
     * Create libs search scope 方法用来创建一个搜索范围，用于搜索项目中的所有 jar 包
     *
     * @param project 传入一个 Project 对象
     * @return 返回一个 GlobalSearchScope 对象
     */
    fun createLibsSearchScope(project: Project): GlobalSearchScope {
        val jars = findLibsJars(project)
        return GlobalSearchScope.filesScope(project, jars)
    }

    /**
     * Find libs jars 方法用来查找项目中的所有 jar 包
     *
     * @param project 传入一个 Project 对象
     * @return 返回一个 List<VirtualFile> 对象
     */
    private fun findLibsJars(project: Project): List<VirtualFile> {
        val libsDir = project.basePath?.let { Paths.get(it, "libs") } ?: return emptyList()
        if (!Files.exists(libsDir)) return emptyList()

        return Files.walk(libsDir)
            .filter { it.toString().endsWith(".jar") }
            .map { LocalFileSystem.getInstance().findFileByNioFile(it) }
            .toList() as List<VirtualFile>
    }

    /**
     * Find all java files 方法用来查找项目中的所有 Java 文件
     *
     * @param project 传入一个 Project 对象
     * @return 返回一个 List<PsiFile> 对象
     */
    fun findAllJavaFiles(project: Project): List<PsiFile> =
        findJavaFiles(project, GlobalSearchScope.projectScope(project))

    /**
     * Find scope java files 方法查找指定范围内的Java文件
     *
     * @param project
     * @param scope
     * @return 返回一个 List<PsiFile> 对象
     */
    fun findScopeJavaFiles(project: Project, scope: GlobalSearchScope): List<PsiFile> =
        findJavaFiles(project, scope)

    /**
     * Find java files 查找给定范围内的Java文件
     *
     * @param project
     * @param scope
     */
    private fun findJavaFiles(project: Project, scope: GlobalSearchScope) =
        ApplicationManager.getApplication().runReadAction<List<PsiFile>> {
            FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
                .mapNotNull { PsiManager.getInstance(project).findFile(it) }
                .filter { it.language == JavaLanguage.INSTANCE && !it.virtualFile.path.contains("src/test") }
        }

    fun getMethodSigNode(method: PsiMethod): MethodSigNode {
        val className = method.className ?: "UnknownClass"
        val methodAccessModifier = getAccessModifier(method)
        val methodModifier = getModifiers(method)
        val methodName = method.name ?: "UnknownMethod"
        val methodParameters = getParameters(method)
        val methodVarargs = method.isVarArgs
        val methodThrowsClause = getMethodThrowsClause(method)
        val methodReturnType = method.returnType?.canonicalText ?: "void"
        val methodAnnotations = getMethodAnnotations(method)
        val sourceSpan = getSourceSpan(method)

        return MethodSigNode(
            className,
            methodAccessModifier,
            methodModifier,
            methodName,
            methodParameters,
            methodVarargs,
            methodThrowsClause,
            methodReturnType,
            methodAnnotations,
            sourceSpan = sourceSpan
        )

    }

    private fun getParameters(method: PsiMethod): List<ParameterNode> {
        return method.parameterList.parameters.map {
            ParameterNode(
                it.name ?: "UnknownParameter",
                it.type.canonicalText
            )
        }
    }

    private fun getMethodThrowsClause(method: PsiMethod): List<String> {
        return method.throwsList.referenceElements.map { it.text }
    }

    private fun getMethodAnnotations(method: PsiMethod): List<String> {
        return method.modifierList.annotations.map { it.text } ?: emptyList()
    }

    private fun getAccessModifier(method: PsiMethod): String {
        return when {
            method.hasModifierProperty(PsiModifier.PUBLIC) -> "public"
            method.hasModifierProperty(PsiModifier.PROTECTED) -> "protected"
            method.hasModifierProperty(PsiModifier.PRIVATE) -> "private"
            else -> "package-private"
        }
    }

    private fun getModifiers(method: PsiMethod): String {
        val modifiers = mutableListOf<String>()
        if (method.hasModifierProperty(PsiModifier.STATIC)) modifiers.add("static")
        if (method.hasModifierProperty(PsiModifier.FINAL)) modifiers.add("final")
        if (method.hasModifierProperty(PsiModifier.SYNCHRONIZED)) modifiers.add("synchronized")
        return modifiers.joinToString(" ")
    }
}