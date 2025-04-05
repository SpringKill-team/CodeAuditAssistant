package org.skgroup.CodeAuditAssistant.analysis.di

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiUtil
import org.skgroup.CodeAuditAssistant.analysis.ast.nodes.MethodNode
import org.skgroup.CodeAuditAssistant.analysis.graphs.callgraph.CallGraph
import org.skgroup.CodeAuditAssistant.enums.RefMode
import org.skgroup.CodeAuditAssistant.utils.GraphUtils
import java.util.*

/**
 * DIProcessor 专门处理与依赖注入相关的逻辑
 * 识别常见 DI 注解，如 @Service, @Component, @Repository, @Autowired, @Inject 等
 * 将被容器管理的类与其依赖关系加入调用图
 * 自动关联类似 "XxxService" 与 "XxxServiceImpl" 的关系
 *
 * @param project   IDEA上下文
 * @param callGraph 需要写入依赖关系的调用图 (如果只需要类级别关系，也可新建一个 ClassGraph)
 */
class DIProcessor(
    private val project: Project,
    private val callGraph: CallGraph
) {

    /**
     * 核心处理入口
     */
    fun process() {
        // 收集所有标记了常见注解的类 (如 @Service/@Component/@Repository 等)
        val beanClasses = collectBeanClasses()

        // 收集带有 @Autowired / @Inject 的字段或构造函数
        // 并将注入关系记录到调用图
        processAutowiredInjections()

        // 3. 自动关联 XxxService 与 XxxServiceImpl
        linkServiceAndServiceImpl()

        // TODO 你也可以在此添加其他自定义处理，如
        //    - 拿到 beanClasses 做一些特殊标记
        //    - 处理 field-level @Resource 注解
        //    - 处理基于 XML 配置的 DI (如果老项目中存在)
    }

    /**
     * 收集常见 DI 注解 (如 @Service, @Component, @Repository 等) 的类
     * 返回这些类的 PsiClass 列表 (供后续使用)
     */
    private fun collectBeanClasses(): List<PsiClass> {
        val annotationsToSearch = listOf(
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Repository",
            "org.springframework.stereotype.Controller"
        )

        val result = mutableSetOf<PsiClass>()

        for (annotationFqn in annotationsToSearch) {
            val annotationClass = JavaPsiFacade.getInstance(project)
                .findClass(annotationFqn, GlobalSearchScope.allScope(project))
                ?: continue

            val annotatedClasses = AnnotatedElementsSearch.searchPsiClasses(annotationClass, GlobalSearchScope.projectScope(project))
            annotatedClasses.forEach { psiClass ->
                result.add(psiClass)
            }
        }

        return result.toList()
    }

    /**
     * 处理 @Autowired / @Inject / @Resource 等注解的字段和构造函数
     * 将这些“注入关系”加入调用图中 (或者也可以单独维护一个 class-level graph)
     */
    private fun processAutowiredInjections() {
        // 这里示例中简单处理字段上的注解
        // 实际项目中可能还需处理 setter 方法、构造方法上的注解等
        val injectionAnnotations = listOf(
            "org.springframework.beans.factory.annotation.Autowired",
            "javax.inject.Inject",
            "javax.annotation.Resource"
        )

        // 获取项目范围内所有文件，再遍历 (可优化成更精准的搜索方式)
        val psiFacade = JavaPsiFacade.getInstance(project)
        val searchScope = GlobalSearchScope.projectScope(project)

        // 找到所有包含注解的 PsiClass / PsiField / PsiMethod 等，然后根据注解进行处理
        // 这里可以做法：遍历所有类 -> 查找字段 -> 判断字段注解
        val allClasses = findAllProjectClasses(psiFacade, searchScope)

        for (cls in allClasses) {
            for (field in cls.allFields) {
                field.modifierList?.annotations?.forEach { annotation ->
                    if (injectionAnnotations.contains(annotation.qualifiedName)) {
                        // 这是一个需要注入的字段
                        // field.type 就是被注入的类型
                        // cls 就是当前类

                        // 将 cls -> field.type 关系记录到调用图
                        // 为了和现有的 method-based callGraph 统一，我们可以构造一个伪“类级方法节点”
                        val callerNode = GraphUtils.getClassNode(cls)   // 伪方法节点：类本身
                        val calleeNode = findOrCreateMethodNodeForType(field.type)

                        // 入图
                        callGraph.nodes.add(callerNode)
                        callGraph.nodes.add(calleeNode)
                        callGraph.edges
                            .getOrPut(callerNode) { mutableSetOf() }
                            .add(calleeNode)
                    }
                }
            }

            // 如果带了注入注解，如 @Autowired
            for (constructor in cls.constructors) {
                constructor.modifierList?.annotations?.forEach { annotation ->
                    if (injectionAnnotations.contains(annotation.qualifiedName)) {
                        // 将容器 -> constructor 关系入图
                        val constructorNode = GraphUtils.getMethodNode(constructor)
                        val containerMethodNode = MethodNode("Container","Container", "Framework", emptyList(), emptyList(), RefMode.CALL)
                        callGraph.nodes.add(constructorNode)
                        callGraph.nodes.add(containerMethodNode)

                        callGraph.edges
                            .getOrPut(containerMethodNode) { mutableSetOf() }
                            .add(constructorNode)
                    }
                }
            }
        }
    }

    /**
     * 自动关联类似 "XxxService" 与 "XxxServiceImpl" 的类。
     */
    private fun linkServiceAndServiceImpl() {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val allClasses = findAllProjectClasses(psiFacade, scope)

        val serviceMap = mutableMapOf<String, PsiClass>()
        val serviceImplMap = mutableMapOf<String, MutableList<PsiClass>>()

        for (cls in allClasses) {
            val name = cls.name ?: continue
            if (name.endsWith("ServiceImpl")) {
                val baseName = name.removeSuffix("ServiceImpl")
                serviceImplMap.getOrPut(baseName) { mutableListOf() }.add(cls)
            } else if (name.endsWith("Service")) {
                val baseName = name.removeSuffix("Service")
                serviceMap[baseName] = cls
            }
        }

        for ((baseName, serviceClass) in serviceMap) {
            val implList = serviceImplMap[baseName] ?: continue
            for (implCls in implList) {

                if (implCls.isInheritor(serviceClass, true)) {
                    // 建立关联 service -> impl
                    val serviceNode = GraphUtils.getClassNode(serviceClass)
                    val implNode = GraphUtils.getClassNode(implCls)

                    callGraph.nodes.add(serviceNode)
                    callGraph.nodes.add(implNode)

                    callGraph.edges.getOrPut(serviceNode) { mutableSetOf() }
                        .add(implNode)
                }
            }
        }
    }

    /**
     * Find all project classes
     * 临时写的一个方法，用于找到项目中的所有类
     *
     * @param psiFacade
     * @param scope
     * @return
     */
    private fun findAllProjectClasses(
        psiFacade: JavaPsiFacade,
        scope: GlobalSearchScope
    ): List<PsiClass> {
        val result = mutableListOf<PsiClass>()

        val psiPackage = psiFacade.findPackage("") ?: return result
        psiPackage.getClasses(scope).forEach { c -> result.add(c) }

        val queue: Queue<PsiPackage> = LinkedList()
        psiPackage.getSubPackages(scope).forEach { queue.offer(it) }

        while (queue.isNotEmpty()) {
            val pkg = queue.poll()
            pkg.getClasses(scope).forEach { c -> result.add(c) }
            pkg.getSubPackages(scope).forEach { sub -> queue.offer(sub) }
        }

        return result
    }

    /**
     * 将一个 PsiType (可能是类) 转化为 MethodNode，用于在 callGraph 中存储
     * 如果需要更准确，可以考虑将 PsiClass 做成 ClassNode，这里仅示范方法节点的复用
     */
    private fun findOrCreateMethodNodeForType(type: PsiType): MethodNode {
        val psiClass = PsiUtil.resolveClassInType(type)
        return if (psiClass != null) {
            // 直接用 psiClass 构造 MethodNode
            GraphUtils.getClassNode(psiClass)
        } else {
            // 找不到对应类，做一个兜底处理
            MethodNode(type.canonicalText ?: "UnknownType",type.canonicalText ?: "UnknownType", "DI", emptyList(), emptyList(),RefMode.CALL)
        }
    }

}
