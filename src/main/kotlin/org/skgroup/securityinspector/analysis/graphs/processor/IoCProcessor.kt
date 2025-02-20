//package org.skgroup.securityinspector.analysis.graphs.processor
//
//import com.intellij.psi.PsiField
//import com.intellij.psi.PsiMethod
//import com.intellij.psi.PsiClass
//import com.intellij.psi.util.PsiTreeUtil
//import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
//import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph
//import org.skgroup.securityinspector.utils.GraphUtils
//import org.skgroup.securityinspector.enums.EdgeType
//import org.skgroup.securityinspector.enums.NodeType
//
//
///**
// * 类描述：IoCProcessor 类用于。
// *
// * @author springkill
// * @version 1.0
// * @since 2025/2/16
// */
//class IoCProcessor {
//    private val injectionPoints = mutableMapOf<MethodNode, MutableSet<MethodNode>>()
//
//    fun processMethodAnnotations(method: PsiMethod, callGraph: CallGraph) {
//        if (method.hasAnnotation("org.springframework.context.annotation.Bean")) {
//            val beanNode = GraphUtils.getMethodNode(method)
//            callGraph.addSpecialNode(beanNode, NodeType.SPRING_BEAN)
//        }
//    }
//
//    fun processFieldInjection(field: PsiField, callGraph: CallGraph) {
//        if (field.hasAnnotation("org.springframework.beans.factory.annotation.Autowired")) {
//            PsiTreeUtil.getParentOfType(field, PsiClass::class.java)?.let { injectClass ->
//                field.type.resolve()?.let { targetClass ->
//                    targetClass.methods.forEach { targetMethod ->
//                        val providerNode = GraphUtils.getMethodNode(targetMethod)
//                        val consumerNode = GraphUtils.getClassNode(injectClass)
//                        injectionPoints.getOrPut(consumerNode) { mutableSetOf() }.add(providerNode)
//                    }
//                }
//            }
//        }
//    }
//
//    fun linkInjectedDependencies(callGraph: CallGraph) {
//        injectionPoints.forEach { (consumer, providers) ->
//            providers.forEach { provider ->
//                callGraph.addEdge(consumer, provider, EdgeType.IOC_INJECTION)
//            }
//        }
//    }
//}