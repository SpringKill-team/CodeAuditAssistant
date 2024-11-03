package org.skgroup.securityinspector.rules.filters

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class InjectionFilter : BaseLocalInspectionTool() {

    companion object {
        private val SQLFILTER_MESSAGE = InspectionBundle.message("vuln.massage.SQLFilter")
        private val XSSFILTER_MESSAGE = InspectionBundle.message("vuln.massage.XSSFilter")
        private val MAYBE_SQL_FILTER_NAME = listOf(
            "SQLFilter", "SQLInjectionFilter", "SQLInjection"
        )
        private val MAYBE_XSS_FILTER_NAME = listOf(
            "XSSFilter", "XSSClear", "ClearXSS"
        )
        private val MAYBE_SQL_FILTER_METHODS = listOf(
            "ClearSQL", "SQLClear"
        )
        private val MAYBE_XSS_FILTER_METHODS = listOf(
            "ClearXSS", "XSSClear"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(aClass: PsiClass) {
                MAYBE_SQL_FILTER_NAME.forEach {
                    if (SecExpressionUtils.matchesClassName(aClass, it)) {
                        holder.registerProblem(
                            aClass,
                            SQLFILTER_MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                }
                MAYBE_XSS_FILTER_NAME.forEach {
                    if (SecExpressionUtils.matchesClassName(aClass, it)) {
                        holder.registerProblem(
                            aClass,
                            XSSFILTER_MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                }
            }

            //需要检查的是定义点而不是调用点，直接用methodName防止warning位置不对
            override fun visitMethod(methodName: PsiMethod) {
                MAYBE_SQL_FILTER_METHODS.forEach {
                    if (SecExpressionUtils.matchesMethodName(methodName, it)) {
                        holder.registerProblem(
                            methodName,
                            SQLFILTER_MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                }
                MAYBE_XSS_FILTER_METHODS.forEach {
                    if (SecExpressionUtils.matchesMethodName(methodName, it)) {
                        holder.registerProblem(
                            methodName,
                            XSSFILTER_MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                }
            }
        }
    }
}