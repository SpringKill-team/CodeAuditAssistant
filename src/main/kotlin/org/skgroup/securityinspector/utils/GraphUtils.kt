package org.skgroup.securityinspector.utils

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.skgroup.securityinspector.analysis.ast.SourceSpan
import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.analysis.ast.nodes.ParameterNode
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph

/**
 * Graph utils 是一些用于处理图的工具方法
 * @author springkill
 */
object GraphUtils {
    private val methodNodeCache = mutableMapOf<String, MethodNode>()

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
    fun getMethodSignature(methodNode: MethodNode): String {
        val className = methodNode.className
        val methodName = methodNode.name

        // 获取方法的参数列表
        val parameterTypes = methodNode.parameters.joinToString(separator = ", ") { param ->
            param.type
        }

        return "$className#$methodName($parameterTypes)"
    }

    /**
     * Get method node 方法用来获取方法节点，位置为方法本身
     * @param method 传入一个 PsiMethod 对象
     * @return 返回一个 MethodNode 对象
     */
    fun getMethodNode(method: PsiMethod): MethodNode {
        val returnType = method.returnType?.canonicalText ?: "void"
        val parameters = method.parameterList.parameters.map { param ->
            val name = param.name ?: "Unknown"
            val type = param.type.canonicalText
            ParameterNode(name, type)
        }

        val className = getClassName(method)
        val sourceSpan = getSourceSpan(method)
        val methodNode = MethodNode(className, method.name, returnType, parameters, sourceSpan = sourceSpan)
        val signature = getMethodSignature(methodNode)

        return methodNodeCache.getOrPut(signature) { methodNode }
    }

    /**
     * Get method node 方法用来获取方法节点，带引用位置
     * @param method 传入一个 PsiMethod 对象
     * @param reference 方法引用
     * @return 返回一个 MethodNode 对象
     */
    fun getMethodNode(method: PsiMethod, reference: PsiReference): MethodNode {
        val returnType = method.returnType?.canonicalText ?: "void"
        val parameters = method.parameterList.parameters.map { param ->
            val name = param.name ?: "Unknown"
            val type = param.type.canonicalText
            ParameterNode(name, type)
        }

        val className = getClassName(method)
        val sourceSpan = getReferenceSourceSpan(reference)
        val methodNode = MethodNode(className, method.name, returnType, parameters, sourceSpan = sourceSpan)
        val signature = getMethodSignature(methodNode)

        return methodNodeCache.getOrPut(signature) { methodNode }
    }

    /**
     * Get method node 方法用来获取方法节点，引用位置通过表达式获取
     * @param method 传入一个 PsiMethod 对象
     * @param expression 表达式用于获取位置
     * @return 返回一个 MethodNode 对象
     */
    fun getMethodNode(method: PsiMethod, expression: PsiExpression): MethodNode {
        val returnType = method.returnType?.canonicalText ?: "void"
        val parameters = method.parameterList.parameters.map { param ->
            val name = param.name ?: "Unknown"
            val type = param.type.canonicalText
            ParameterNode(name, type)
        }

        val className = getClassName(method)
        val sourceSpan = getSourceSpan(expression)
        val methodNode = MethodNode(className, method.name, returnType, parameters, sourceSpan = sourceSpan)
        val signature = getMethodSignature(methodNode)

        return methodNodeCache.getOrPut(signature) { methodNode }
    }

    /**
     * Get class name 方法用来获取类名
     * @param method 传入一个 PsiMethod 对象
     * @return 返回一个字符串，表示类名
     */
    private fun getClassName(method: PsiMethod): String {
        return method.containingClass?.qualifiedName ?: "UnknownClass"
    }


    /**
     * Get source span 方法用来获取源码位置信息
     * @param element 传入一个 PsiElement 对象
     * @return 返回一个 SourceSpan 对象
     */
    private fun getSourceSpan(element: PsiElement): SourceSpan {
        //TODO: 看看位置获取到底是怎么回事
        val containingFile = element.containingFile
        val virtualFile = containingFile.virtualFile
        val offset = element.textOffset
//        val textRange = element.textRange?:return SourceSpan(containingFile,virtualFile,0,0,0,0)
//        val document = containingFile.viewProvider.document
//        val startLine = document?.getLineNumber(textRange.startOffset) ?: 0
//        val endLine = document?.getLineNumber(textRange.endOffset) ?: 0
//        val startColumn = textRange.startOffset - (document?.getLineStartOffset(startLine) ?: 0)
//        val endColumn = textRange.endOffset - (document?.getLineStartOffset(endLine) ?: 0)
        return SourceSpan(containingFile, virtualFile, offset)
    }

    private fun getReferenceSourceSpan(reference: PsiReference): SourceSpan {
        val element = reference.element
        return getSourceSpan(element)
    }

    /**
     * Get method node 方法用来获取方法节点
     * 用于将 PsiClass 映射为一个 MethodNode。
     * 这样在 DIProcessor 或者其他地方需要把类加入调用图时，可以保持类型统一。
     *
     * 也可以改成专门的 getClassNode(...) 返回 ClassNode(但是ClassNode好像没啥用)，
     * 不过为兼容原始的 callGraph 结构，使用 MethodNode 也行。
     * @param clazz 传入一个 PsiClass 对象
     * @return 返回一个 MethodNode 对象
     */
    fun getMethodNode(clazz: PsiClass): MethodNode {
        //TODO: 处理匿名类
        var signature = clazz.qualifiedName
            ?: clazz.name
            ?: "UnknownClass"
        val sourceSpan = getSourceSpan(clazz)
        val methodNode = MethodNode(signature, "class", "void", emptyList(), sourceSpan = sourceSpan)
        signature = "[DI]" + signature

        return methodNodeCache.getOrPut(signature) { methodNode }
    }

    /**
     * 获取 lambda 表达式对应的方法节点
     * @param method 传入一个 PsiMethod 对象
     * @return 返回一个 MethodNode 对象
     */
    fun getLambdaMethodNode(method: PsiMethod): MethodNode {
        val methodNode = getMethodNode(method)
        val signature = "[Lambda]" + getMethodSignature(methodNode)
        return methodNodeCache.getOrPut(signature) { methodNode }
//        return MethodNode(signature, returnType, parameters, emptyList())
    }

    /*fun getClassInitMethodNode(diClass: PsiClass): MethodNode {
        val method = diClass.allMethods.firstOrNull { it.name == "<init>" } ?: return MethodNode(
            "[DI]Unknown",
            "void",
            emptyList(),
            emptyList()
        )
        val returnType = method.returnType?.canonicalText ?: "void"
        val parameters = method.parameterList.parameters.map { param ->
            val name = param.name ?: "Unknown"
            val type = param.type.canonicalText
            ParameterNode(name, type)
        }

        val signature = "[DI]" + getMethodSignature(method)

        return methodNodeCache.getOrPut(signature) {
            MethodNode(signature, returnType, parameters, emptyList())
        }
//        return MethodNode(signature, returnType, parameters, emptyList())
    }
*/
    /**
     * Find path 方法用来查找两个方法之间的调用路径（DFS查找）
     * @param graph 传入一个 CallGraph 对象
     * @param node 传入一个 MethodNode 对象
     * @param target 传入一个 MethodNode 对象
     * @return 返回一个 List<MethodNode> 对象
     */
    fun findPath(graph: CallGraph, node: MethodNode, target: MethodNode): List<MethodNode>? {
        val visited = mutableSetOf<MethodNode>()
        val path = mutableListOf<MethodNode>()

        fun isMatch(n1: MethodNode, n2: MethodNode): Boolean {
            return n1.className == n2.className && n1.name == n2.name
        }

        fun dfs(cur: MethodNode): Boolean {
            visited.add(cur)
            path.add(cur)
            if (isMatch(cur,target)) return true

            val nexts = graph.edges[cur].orEmpty()
            for (n in nexts) {
                if (!visited.contains(n)) {
                    if (dfs(n)) return true
                }
            }
            path.removeLast()
            return false
        }

        return if (dfs(node)) path.toList() else null
    }

    /**
     * Find all java files 方法用来查找项目中的所有 Java 文件
     * @param project 传入一个 Project 对象
     * @return 返回一个 List<PsiFile> 对象
     */
    fun findAllJavaFiles(project: Project): List<PsiFile> {
        return ApplicationManager.getApplication().runReadAction<List<PsiFile>> {
            val result = mutableListOf<PsiFile>()
            val scope = GlobalSearchScope.projectScope(project)
            val vFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)

            val psiManager = PsiManager.getInstance(project)
            for (vf: VirtualFile in vFiles) {
                val psiFile = psiManager.findFile(vf)
                if (psiFile != null && psiFile.language == JavaLanguage.INSTANCE) {
                    result.add(psiFile)
                }
            }
            result
        }
    }

    /**
     * Find scope java files 方法查找指定范围内的Java文件
     *
     * @param project
     * @param scope
     * @return
     */
    fun findScopeJavaFiles(project: Project, scope: GlobalSearchScope): List<PsiFile> {
        return ApplicationManager.getApplication().runReadAction<List<PsiFile>> {
            val result = mutableListOf<PsiFile>()
            val vFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)

            val psiManager = PsiManager.getInstance(project)
            for (vf: VirtualFile in vFiles) {
                val psiFile = psiManager.findFile(vf)
                if (psiFile != null && psiFile.language == JavaLanguage.INSTANCE) {
                    result.add(psiFile)
                }
            }
            result
        }
    }

}