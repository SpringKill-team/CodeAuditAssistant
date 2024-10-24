package org.skgroup.securityinspector.rules.ssrf

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.inspectors.InspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class JavaURLSSRF : BaseLocalInspectionTool(), InspectionTool {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.JavaURLSSRF")

        private val JAVAURL_METHOD_SINKS = mapOf(
            "java.net.URI" to listOf("create"),
            "java.net.URL" to listOf("openStream", "openConnection"),
            "java.net.Socket" to listOf("<init>"),
            "java.net.http.HttpRequest" to listOf("newBuilder"),
            "javax.ws.rs.client.Client" to listOf("target")
        )

        private val JAVAURL_NEWEXPRESSION_SINKS = listOf(
            "java.net.URL",
            "java.net.Socket"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, JAVAURL_METHOD_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.isNewExpressionSink(expression, JAVAURL_NEWEXPRESSION_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
        }
    }

    override fun inspectFile(psiFile: PsiFile, problemsHolder: ProblemsHolder) {
        psiFile.accept(buildVisitor(problemsHolder, false))
    }
}

