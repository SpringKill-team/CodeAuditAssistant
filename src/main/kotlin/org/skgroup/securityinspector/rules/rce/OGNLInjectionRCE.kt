package org.skgroup.securityinspector.rules.rce

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class OGNLInjectionRCE : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.OGNLInjectionRCE")

        private val OGNL_METHOD_SINKS = mapOf(
            "com.opensymphony.xwork2.ognl.OgnlUtil" to listOf("callMethod", "getValue", "setValue"),
            "ognl.Node" to listOf("getValue", "setValue"),
            "ognl.Ognl" to listOf("getValue", "setValue"),
            "ognl.enhance.ExpressionAccessor" to listOf("get", "set"),
            "org.apache.commons.ognl.Node" to listOf("getValue", "setValue"),
            "org.apache.commons.ognl.Ognl" to listOf("getValue", "setValue"),
            "org.apache.commons.ognl.enhance.ExpressionAccessor" to listOf("get", "set")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, OGNL_METHOD_SINKS)) {
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
