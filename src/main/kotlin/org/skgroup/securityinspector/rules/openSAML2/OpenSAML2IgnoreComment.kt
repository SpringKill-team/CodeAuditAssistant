package org.skgroup.securityinspector.rules.openSAML2

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils
import com.siyeh.ig.psiutils.ExpressionUtils
import org.skgroup.securityinspector.utils.SetBoolArgQuickFix

class OpenSAML2IgnoreComment : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.OpenSAML2IgnoreComments")
        private val QUICK_FIX_NAME = InspectionBundle.message("vuln.fix.OpenSAML2IgnoreComments")

        private val OPENSAML2_METHOD_SINKS = mapOf(
            "org.opensaml.xml.parse.StaticBasicParserPool" to listOf("setIgnoreComments"),
            "org.opensaml.xml.parse.BasicParserPool" to listOf("setIgnoreComments")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, OPENSAML2_METHOD_SINKS)) {
                    expression.argumentList?.expressions?.firstOrNull()?.let { arg0 ->
                        val literalArg = arg0 as? PsiLiteralExpression ?: return
                        if (ExpressionUtils.isLiteral(literalArg, false)) {
                            holder.registerProblem(
                                expression,
                                MESSAGE,
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                SetBoolArgQuickFix(QUICK_FIX_NAME, true, literalArg)
                            )
                        }
                    }
                }
            }
        }
    }
}

