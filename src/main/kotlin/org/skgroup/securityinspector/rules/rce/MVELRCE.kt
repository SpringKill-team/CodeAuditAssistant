package org.skgroup.securityinspector.rules.rce

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class MVELRCE : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.MVELRCE")

        private val MVEL_METHOD_SINKS = mapOf(
            "org.mvel2.MVEL" to listOf(
                "eval", "evalToBoolean", "evalToString",
                "executeAllExpression", "executeExpression", "executeSetExpression"
            ),
            "org.mvel2.MVELRuntime" to listOf("execute"),
            "org.mvel2.compiler.Accessor" to listOf("getValue"),
            "org.mvel2.compiler.CompiledAccExpression" to listOf("getValue"),
            "org.mvel2.compiler.CompiledExpression" to listOf("getDirectValue"),
            "org.mvel2.compiler.ExecutableStatement" to listOf("getValue"),
            "org.mvel2.jsr223.MvelCompiledScript" to listOf("eval"),
            "org.mvel2.jsr223.MvelScriptEngine" to listOf("eval", "evaluate"),
            "org.mvel2.templates.TemplateRuntime" to listOf("eval", "execute")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, MVEL_METHOD_SINKS)) {
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
