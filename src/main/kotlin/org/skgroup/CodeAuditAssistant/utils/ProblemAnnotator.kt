package org.skgroup.CodeAuditAssistant.utils

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression

/**
 * 类描述：ProblemAnnotator 类用于 替换原本的问题告警注册。
 *
 * @author springkill
 * @version 1.0
 */
class ProblemAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element is PsiMethodCallExpression) {
            val calleeMethodNode = element.resolveMethod()?.let { GraphUtils.getMethodNode(it, element) }
            SinkList.ALL_SUB_VUL_DEFINITIONS.forEach{ sink ->
                if (calleeMethodNode != null) {
                    if(calleeMethodNode.className in sink.methodSinks.keys){
                        sink.methodSinks.get(calleeMethodNode.className)?.forEach { sinkMethod ->
                            if(calleeMethodNode.name in sinkMethod) {
                                holder.newAnnotation(
                                    HighlightSeverity.WARNING,
                                    sink.subType.name
                                    ).create()
                            }
                        }
                    }
                }
            }
        }
    }
}
