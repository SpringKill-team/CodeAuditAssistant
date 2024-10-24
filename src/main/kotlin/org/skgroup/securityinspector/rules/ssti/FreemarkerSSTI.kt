package org.skgroup.securityinspector.rules.ssti

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class FreemarkerSSTI : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.FreemarkerSSTI")

        private val FREEMARKER_METHOD_SINKS = mapOf(
            "freemarker.cache.StringTemplateLoader" to listOf("putTemplate")
        )

        private val FREEMARKER_NEWEXPRESSION_SINKS = listOf(
            "freemarker.template.Template"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, FREEMARKER_METHOD_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.isNewExpressionSink(expression, FREEMARKER_NEWEXPRESSION_SINKS)) {
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

