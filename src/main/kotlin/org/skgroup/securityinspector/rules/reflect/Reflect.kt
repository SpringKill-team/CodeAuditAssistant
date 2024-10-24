package org.skgroup.securityinspector.rules.reflect

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class Reflect : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.Reflect")

        private val REFLECT_METHOD_SINKS = mapOf(
            "java.lang.reflect.Method" to listOf("invoke"),
            "java.net.URLClassLoader" to listOf("newInstance"),
            "java.lang.ClassLoader" to listOf("loadClass"),
            "org.codehaus.groovy.runtime.InvokerHelper" to listOf("invokeMethod"),
            "groovy.lang.MetaClass" to listOf("invokeMethod", "invokeConstructor", "invokeStaticMethod")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, REFLECT_METHOD_SINKS)) {
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
