package org.skgroup.securityinspector.rules.rce

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class GroovyRCE : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.GroovyRCE")

        private val GROOVY_METHOD_SINKS = mapOf(
            "groovy.lang.GroovyClassLoader" to listOf("parseClass"),
            "groovy.lang.GroovyShell" to listOf("evaluate", "parse", "run"),
            "groovy.util.Eval" to listOf("me", "x", "xy", "xyz"),
            "org.codehaus.groovy.control.CompilationUnit" to listOf("compile"),
            "org.codehaus.groovy.tools.javac.JavaAwareCompilationUnit" to listOf("compile")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, GROOVY_METHOD_SINKS)) {
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
