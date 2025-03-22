package org.skgroup.securityinspector.analysis.graphs.callgraph

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.analysis.di.DIProcessor
import org.skgroup.securityinspector.enums.RefMode
import org.skgroup.securityinspector.utils.GraphUtils
import java.util.ArrayDeque

/**
 * Call graph builder 是一个用于构建调用图的类
 * 通过访问 Java PSI 树，可以构建出方法之间的调用关系
 *
 * 处理Lambda表达式 (visitLambdaExpression)
 * 处理方法引用 (visitMethodReferenceExpression)
 * 识别常见依赖注入注解 (visitMethod / visitClass / visitField 等处辅助识别)
 * 识别常见 IoC 容器的 getBean 调用 (visitMethodCallExpression 时做特殊处理)
 *
 * @author springkill
 */
class CallGraphBuilder : JavaRecursiveElementVisitor() {

    // 用于存储最终的调用图，全局唯一
    private val callGraph = CallGraph()

    // 用于记录当前访问的方法调用栈
    private val currentMethodStack = ArrayDeque<MethodNode>()

//    private val diProcessor = DIProcessor(callGraph)

    /**
     * Visit method 方法用于访问一个Java方法并将其加入调用图
     * 用栈处理递归方法调用
     *
     * @param method
     */
    override fun visitMethod(method: PsiMethod) {

        //堆栈用节点，在压栈前会被替换
        var calleeStackNode = GraphUtils.getMethodNode(method)

        // 查找对该方法的所有引用，建立反向调用关系
        // TODO 这里的逻辑我自己也混乱了，可能会有问题，后面再看
        val scope = GlobalSearchScope.projectScope(method.project)
        ReferencesSearch.search(method, scope).forEach { reference ->
            val callerMethod = PsiTreeUtil.getParentOfType(reference.element, PsiMethod::class.java)
                ?: return@forEach

            val calleeMethodNode = GraphUtils.getMethodNode(method, reference)
            calleeStackNode = calleeMethodNode
            callGraph.nodes.add(calleeMethodNode)

            val callerMethodNode = GraphUtils.getMethodNode(callerMethod, reference)
            // 建立反向调用关系
            callGraph.nodes.add(callerMethodNode)
            callGraph.edges.getOrPut(callerMethodNode) { mutableSetOf() }.add(calleeMethodNode)

            handleDependencyInjectionAnnotations(method, calleeMethodNode)
        }

        // 将此方法节点加入 callGraph
        currentMethodStack.push(calleeStackNode)

        super.visitMethod(method)

        currentMethodStack.pop()
    }

    /**
     * 访问方法调用表达式
     *
     * @param expression    方法调用表达式
     */
    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        if (currentMethodStack.isEmpty()) return
        expression.methodExpression.qualifierExpression?.accept(this)

        val caller = currentMethodStack.peek()
        // 解析被调用的方法
        val resolvedMethod = expression.resolveMethod()
        // 调试日志
        if (resolvedMethod == null) {
            println("method call expression $expression can not be resolve.")
            return
        }
        val clazz: PsiClass? = resolvedMethod.containingClass
        expression.reference?.let {
            val calleeMethodNode = GraphUtils.getMethodNode(resolvedMethod, it)
            println("expression $expression resolve reference success.")
            // 将 callee 加入节点集合
            callGraph.nodes.add(calleeMethodNode)
            // 在 callGraph 中记录调用关系 (caller -> callee)
            callGraph.edges
                .getOrPut(caller) { mutableSetOf() }
                .add(calleeMethodNode)
            handleIoCContainerCall(expression, caller, calleeMethodNode)
        } ?: run {
            if (clazz != null) {
                if (clazz.isInterface) {
                    println("$clazz is an interface")
                    println("and the method call expression $expression can not resolve reference.")
                }
                if (clazz.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    println("$clazz is an abstract class")
                    println("and the method call expression $expression can not resolve reference.")
                }
            }
            if (PsiTreeUtil.getParentOfType(expression, PsiLambdaExpression::class.java) != null) {
                println("in lambda method call expression $expression can not resolve reference.")
            } else {
                println("unknown method call expression $expression can not resolve reference.")
            }
        }
        super.visitMethodCallExpression(expression)
    }

    /**
     * 访问 new 表达式
     *
     * @param expression
     */
    override fun visitNewExpression(expression: PsiNewExpression) {
        if (currentMethodStack.isEmpty()) return
//        expression.methodNewExpression.qualifierExpression?.accept(this)

        val callerMethodNode = currentMethodStack.peek()
        // 解析构造方法
        val constructor = expression.resolveConstructor()
        // 调试日志
        if (constructor == null) {
            println("new expression $constructor can not be resolve.")
            return
        }
        val clazz: PsiClass? = constructor.containingClass
        expression.reference?.let {
            val calleeMethodNode = GraphUtils.getMethodNode(constructor, it)
            // 将 callee 加入节点集合
            callGraph.nodes.add(calleeMethodNode)
            // 在 callGraph 中记录 (caller -> callee构造方法)
            callGraph.edges
                .getOrPut(callerMethodNode) { mutableSetOf() }
                .add(calleeMethodNode)
            println("expression $expression resolve reference success.")
        } ?: run {
            if (clazz != null) {
                if (clazz.isInterface) {
                    println("$clazz is an interface")
                    println("and the method call expression $expression can not resolve reference.")
                }
                if (clazz.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    println("$clazz is an abstract class")
                    println("and the method call expression $expression can not resolve referencee.")
                }
            }
            if (PsiTreeUtil.getParentOfType(expression, PsiLambdaExpression::class.java) != null) {
                println("in lambda method call expression $expression can not resolve reference.")
            } else {
                println("unknown new expression $constructor can not resolve reference.")
            }
        }
        super.visitNewExpression(expression)
    }

    /**
     * 访问 lambda 表达式
     *
     * @param expression
     */
    override fun visitLambdaExpression(expression: PsiLambdaExpression) {
        super.visitLambdaExpression(expression)

        // TODO 如果需要将 Lambda 表达式本身视为一个“方法节点”，可以考虑建模为内部匿名类的形式
        if (currentMethodStack.isEmpty()) {
            return
        }

        // 在 Lambda 内部继续递归访问，比如对 Lambda 体内的方法调用进行追踪
        expression.body?.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(lambdaCall: PsiMethodCallExpression) {
                super.visitMethodCallExpression(lambdaCall)
                val caller = currentMethodStack.peek()
                val resolvedMethod = lambdaCall.resolveMethod() ?: return
                lambdaCall.children.forEach { child ->
                    val calleeMethodNode = child.reference?.let {
                        GraphUtils.getMethodNode(resolvedMethod, it)
                    } ?: run {
                        GraphUtils.getLambdaMethodNode(resolvedMethod)
                    }
                    callGraph.nodes.add(caller)
                    calleeMethodNode?.let {
                        callGraph.nodes.add(it)
                        callGraph.edges
                            .getOrPut(caller) { mutableSetOf() }
                            .add(it)
                    }
                }
            }

            override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression) {
                super.visitMethodReferenceExpression(expression)
                // 对 Lambda 中的 method reference 也进行处理
                handleMethodReference(expression)
            }
        })
    }

    /**
     * 访问方法引用 (Foo::bar)
     */
    override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression) {
        super.visitMethodReferenceExpression(expression)
        if (currentMethodStack.isEmpty()) {
            return
        }
        handleMethodReference(expression)
    }

    /**
     * 处理方法引用的逻辑，提取所引用的方法并加入调用图
     */
    private fun handleMethodReference(expression: PsiMethodReferenceExpression) {
        val caller = currentMethodStack.peek()
        val resolvedElement = expression.resolve() ?: return
        if (resolvedElement is PsiMethod) {
            // TODO 还有一个地方处理了reference,要记得合并一下
            val calleeMethodNode = expression.reference?.let { GraphUtils.getMethodNode(resolvedElement, it) }
            calleeMethodNode?.let {
                callGraph.nodes.add(it)
                callGraph.edges
                    .getOrPut(caller) { mutableSetOf() }
                    .add(it)
            }
        }
    }

    /**
     * Handle dependency injection annotations
     * 对常见依赖注入注解(如 @Autowired / @Inject 等)进行检测和处理
     * @param method
     * @param methodNode
     */
    private fun handleDependencyInjectionAnnotations(method: PsiMethod, methodNode: MethodNode) {
        val modifierList = method.modifierList
        val annotations = modifierList.annotations
        annotations.forEach { annotation ->
            val qualifiedName = annotation.qualifiedName ?: return@forEach
            if (qualifiedName == "org.springframework.beans.factory.annotation.Autowired"
                || qualifiedName == "javax.inject.Inject"
            // 其他的慢慢加吧
            ) {
                // 如果是构造方法或者带有此注解的方法，可能被框架在运行时调用
                val containerMethodNode =
                    MethodNode("Container", "Container", "Framework", emptyList(), emptyList(), RefMode.CALL)
                callGraph.nodes.add(containerMethodNode)

                callGraph.edges
                    .getOrPut(containerMethodNode) { mutableSetOf() }
                    .add(methodNode)
            }
        }
    }

    /**
     * 对 IoC 容器调用（如 getBean）进行处理
     * 如果调用了容器方法来获取某个类实例，可以在调用图中做额外标记
     */
    private fun handleIoCContainerCall(
        expression: PsiMethodCallExpression,
        caller: MethodNode,
        callee: MethodNode
    ) {
        val methodExpression = expression.methodExpression
        val qualifierExpression = methodExpression.qualifierExpression
        val methodName = methodExpression.referenceName

        // 测试下能不能行
        if (qualifierExpression != null && methodName == "getBean") {
            // 将容器节点与被调用方法或类做额外的链接
            val containerMethodNode =
                MethodNode("ApplicationContext", "ApplicationContext", "Spring", emptyList(), emptyList(), RefMode.CALL)
            callGraph.nodes.add(containerMethodNode)
            // caller -> container
            callGraph.edges
                .getOrPut(caller) { mutableSetOf() }
                .add(containerMethodNode)
            // container -> callee
            callGraph.edges
                .getOrPut(containerMethodNode) { mutableSetOf() }
                .add(callee)
        }
    }

    /**
     * Get call graph 方法用于获取调用图并最后处理一下
     *
     * @param project
     * @return
     */
    fun getCallGraph(project: Project): CallGraph {
        val diProcessor = DIProcessor(project, callGraph)
        ApplicationManager.getApplication().runReadAction {
            diProcessor.process()
            // 如果 process() 内部依赖 PSI 的方法，也就安全了
        }
        return callGraph
    }

}
