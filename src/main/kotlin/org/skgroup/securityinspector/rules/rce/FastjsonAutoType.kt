package org.skgroup.securityinspector.rules.rce

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.DeleteElementQuickFix
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class FastjsonAutoType : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.FastjsonAutoType")
        private val QUICK_FIX_NAME = InspectionBundle.message("vuln.fix.FastjsonAutoType")

        private val FASTJSONAUTOTYPE_METHOD_SINKS = mapOf(
            "com.alibaba.fastjson.parser.ParserConfig" to listOf("setAutoTypeSupport")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, FASTJSONAUTOTYPE_METHOD_SINKS)) {
                    val args = expression.argumentList.expressions
                    if (args.size == 1 &&
                        args[0] is PsiLiteralExpression &&
                        (args[0] as PsiLiteralExpression).value == true
                    ) {
                        holder.registerProblem(
                            expression,
                            MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            DeleteElementQuickFix(expression, QUICK_FIX_NAME)
                        )
                    }
                }
            }
        }
    }
}
