package org.skgroup.securityinspector.rules.dos

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class SystemDOS : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.SystemEXITDOS")
        private val EXIT_METHODS = mapOf(
            "java.lang.System" to listOf("exit"),
            "java.lang.Shutdown" to listOf("exit"),
            "java.lang.Runtime" to listOf("exit")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (isSystemExitCall(expression)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
        }
    }

    private fun isSystemExitCall(expression: PsiMethodCallExpression): Boolean {
        return EXIT_METHODS.any { (className, methodNames) ->
            methodNames.any { methodName ->
                SecExpressionUtils.hasFullQualifiedName(expression, className, methodName)
            }

        }
    }
}
