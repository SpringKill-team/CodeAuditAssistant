package org.skgroup.securityinspector.rules.ssti

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class FreemarkeraSSTI : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("freemarker.ssti.type.msg")

        // 被检查的类和方法映射
        private val checkedMethods = mapOf(
            "freemarker.cache.StringTemplateLoader" to listOf("putTemplate"),
            "freemarker.template.Template" to emptyList()
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                checkedMethods.forEach { (className, methods) ->
                    methods.forEach { methodName ->
                        if (SecExpressionUtils.hasFullQualifiedName(expression, className, methodName)) {
                            holder.registerProblem(
                                expression,
                                MESSAGE,
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                            )
                        }
                    }
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                // 检查是否为需要检测的类的实例化
                checkedMethods.forEach { (className, methods) ->
                    if (methods.isEmpty() && SecExpressionUtils.hasFullQualifiedName(expression, className)) {
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
}
