package org.skgroup.securityinspector.rules.redirect

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class JavaxRedirect : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.JavaxRedirect")

        private val JAVAXREDIRECT_METHOD_SINKS = mapOf(
            "javax.servlet.http.HttpServletResponse" to listOf("sendRedirect"),
            "javax.servlet.RequestDispatcher" to listOf("getRequestDispatcher"),
            "javax.servlet.http.HttpServletRequest" to listOf("getRequestDispatcher"),
            "javax.ws.rs.core.Response" to listOf("seeOther", "temporaryRedirect")
        )

        private val JAVAXREDIRECT_NEWEXPRESSION_SINKS = listOf(
            "org.springframework.web.servlet.ModelAndView"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, JAVAXREDIRECT_METHOD_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.isNewExpressionSink(expression, JAVAXREDIRECT_NEWEXPRESSION_SINKS)) {
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

