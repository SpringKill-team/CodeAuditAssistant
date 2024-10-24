package org.skgroup.securityinspector.rules.ssti

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class VelocitySSTI : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.VelocitySSTI")

        private val VELOCITY_METHOD_SINKS = mapOf(
            "org.apache.velocity.app.Velocity" to listOf("evaluate", "mergeTemplate"),
            "org.apache.velocity.app.VelocityEngine" to listOf("evaluate", "mergeTemplate"),
            "org.apache.velocity.runtime.RuntimeServices" to listOf("evaluate", "parse"),
            "org.apache.velocity.runtime.RuntimeSingleton" to listOf("parse"),
            "org.apache.velocity.runtime.resource.util.StringResourceRepository" to listOf("putStringResource")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, VELOCITY_METHOD_SINKS)) {
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
