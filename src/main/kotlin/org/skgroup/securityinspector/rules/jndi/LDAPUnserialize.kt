package org.skgroup.securityinspector.rules.jndi

import com.intellij.codeInspection.*
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils
import org.skgroup.securityinspector.utils.SetBoolArgQuickFix

class LDAPUnserialize : BaseLocalInspectionTool() {

    companion object{
        private val MESSAGE = InspectionBundle.message("vuln.massage.LDAPUnserialize")
        private val QUICK_FIX_NAME = InspectionBundle.message("vuln.fix.LDAPUnserialize")
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitNewExpression(expression: PsiNewExpression) {
                checkSearchControls(expression, holder)
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                checkSetReturningObjFlag(expression, holder)
            }
        }
    }

    private fun checkSearchControls(expression: PsiNewExpression, holder: ProblemsHolder) {
        if (SecExpressionUtils.hasFullQualifiedName(expression, "javax.naming.directory.SearchControls")) {
            expression.argumentList?.expressions?.let { args ->
                if (args.size == 6 && args[4] is PsiLiteralExpression &&
                    (args[4] as PsiLiteralExpression).value == true
                ) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        SetBoolArgQuickFix(QUICK_FIX_NAME, false, args[4] as PsiLiteralExpression)
                    )
                }
            }
        }
    }

    private fun checkSetReturningObjFlag(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        if (SecExpressionUtils.hasFullQualifiedName(
                expression,
                "javax.naming.directory.SearchControls",
                "setReturningObjFlag"
            )
        ) {
            expression.argumentList.expressions.let { args ->
                if (args.size == 1 && args[0] is PsiLiteralExpression &&
                    (args[0] as PsiLiteralExpression).value == true
                ) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        SetBoolArgQuickFix(QUICK_FIX_NAME, false, args[0] as PsiLiteralExpression)
                    )
                }
            }
        }
    }
}

