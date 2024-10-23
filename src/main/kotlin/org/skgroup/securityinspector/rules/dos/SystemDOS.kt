package com.skgroup.securityinspector.rules.dos

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import com.skgroup.securityinspector.utils.InspectionBundle
import com.skgroup.securityinspector.utils.SecExpressionUtils

/**
 * 1038: 检查系统退出方法，防止系统退出导致DOS攻击
 */
const val SYSTEM_DOS_MESSAGE = "system.exit.type.msg"

class SystemDOS : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message(SYSTEM_DOS_MESSAGE)
        private val EXIT_METHODS = listOf(
            "java.lang.System" to "exit",
            "java.lang.Shutdown" to "exit",
            "java.lang.Runtime" to "exit"
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

    /**
     * 检查方法调用是否是系统退出相关的方法
     */
    private fun isSystemExitCall(expression: PsiMethodCallExpression): Boolean {
        return EXIT_METHODS.any { (className, methodName) ->
            SecExpressionUtils.hasFullQualifiedName(expression, className, methodName)
        }
    }
}
