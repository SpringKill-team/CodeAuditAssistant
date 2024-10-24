package org.skgroup.securityinspector.rules.rce

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class JEXLRCE : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.JEXLRCE")

        private val JEXL_METHOD_SINKS = mapOf(
            "org.apache.commons.jexl3.Expression" to listOf("callable", "evaluate"),
            "org.apache.commons.jexl3.JexlEngine" to listOf("getProperty", "setProperty"),
            "org.apache.commons.jexl3.JexlExpression" to listOf("callable", "evaluate"),
            "org.apache.commons.jexl3.JexlScript" to listOf("callable", "evaluate"),
            "org.apache.commons.jexl3.JxltEngine\$Expression" to listOf("evaluate", "prepare"),
            "org.apache.commons.jexl3.JxltEngine\$Template" to listOf("evaluate"),
            "org.apache.commons.jexl3.Script" to listOf("callable", "execute"),
            "org.apache.commons.jexl2.Expression" to listOf("callable", "evaluate"),
            "org.apache.commons.jexl2.JexlEngine" to listOf("getProperty", "setProperty"),
            "org.apache.commons.jexl2.JexlExpression" to listOf("callable", "evaluate"),
            "org.apache.commons.jexl2.JexlScript" to listOf("callable", "execute"),
            "org.apache.commons.jexl2.Script" to listOf("callable", "execute"),
            "org.apache.commons.jexl2.UnifiedJEXL\$Expression" to listOf("evaluate", "prepare"),
            "org.apache.commons.jexl2.UnifiedJEXL\$Template" to listOf("evaluate")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, JEXL_METHOD_SINKS)) {
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
