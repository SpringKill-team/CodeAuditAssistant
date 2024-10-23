package org.skgroup.securityinspector.SSRF

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class JavaURLSSRF : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("java.url.ssrf.msg")

        // 列出所有可能导致 SSRF 的 sink
        private val methodSinkMap = mapOf(
            "java.net.URI" to listOf("create"),
            "java.net.URL" to listOf("openStream", "openConnection"),
            "java.net.Socket" to listOf("<init>"),
            "java.net.http.HttpRequest" to listOf("newBuilder"),
            "javax.ws.rs.client.Client" to listOf("target")
        )

        // 需要检查的类名
        private val newExpressionSinkMap = listOf("java.net.URL", "java.net.Socket")
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            // 检查方法调用的 SSRF sink
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                methodSinkMap.forEach { (className, methods) ->
                    methods.forEach { method ->
                        if (SecExpressionUtils.hasFullQualifiedName(expression, className, method)) {
                            holder.registerProblem(
                                expression,
                                MESSAGE,
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                            )
                        }
                    }
                }
            }

            // 检查对象创建表达式的 SSRF sink
            override fun visitNewExpression(expression: PsiNewExpression) {
                newExpressionSinkMap.forEach { className ->
                    if (SecExpressionUtils.hasFullQualifiedName(expression, className)) {
                        holder.registerProblem(
                            expression,
                            MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                }
            }
        }
    }
}
