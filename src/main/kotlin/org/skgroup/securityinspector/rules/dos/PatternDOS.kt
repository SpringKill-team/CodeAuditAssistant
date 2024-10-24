package org.skgroup.securityinspector.rules.dos

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.siyeh.ig.psiutils.MethodCallUtils
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils
import org.jetbrains.annotations.NotNull
import java.util.regex.Pattern


class PatternDOS : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.PatternMatchesDOS")

        /**
         * 检查是否为指数型正则表达式
         */
        fun isExponentialRegex(s: String): Boolean {
            val regexPatterns = listOf(
                ".*\\([^()*+\\]]+\\]?(\\*|\\+)\\)(\\*|\\+).*",   // ([a-z]+)+
                ".*\\((\\([^()]+\\)\\?)?\\([^()*+\\]]+\\]?(\\*|\\+)\\)\\)(\\*|\\+).*",  // (([a-z])?([a-z]+))+
                ".*\\(\\([^()*+\\]]+\\]?\\)(\\*|\\+)\\)(\\*|\\+).*",  // (([a-z])+)+
                ".*\\(([^()*+\\]]+\\]?)\\|\\1+\\??\\)(\\*|\\+).*",    // (a|aa)+
                ".*\\(\\.\\*[^()*+\\]]+\\]?\\)\\{[1-9][0-9]+,?[0-9]*\\}.*"  // (.*[a-z]){n} n >= 10
            )
            return regexPatterns.any { Pattern.matches(it, s) }
        }
    }

    @NotNull
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                val methodName = MethodCallUtils.getMethodName(expression)
                if (MethodCallUtils.isCallToRegexMethod(expression) &&
                    (methodName == "compile" || methodName == "matches")) {
                    val regexArg = expression.argumentList.expressions.firstOrNull()
                    val literal = getLiteralExpression(regexArg)
                    val literalText = SecExpressionUtils.getLiteralInnerText(literal)
                    if (literal != null && literalText != null && isExponentialRegex(literalText)) {
                        holder.registerProblem(regexArg!!, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    }

                }
            }
        }
    }

    private fun getLiteralExpression(expression: PsiExpression?): PsiLiteralExpression? {
        return when (expression) {
            is PsiReferenceExpression -> {
                val resolvedElement = expression.resolve() as? PsiVariable
                resolvedElement?.initializer as? PsiLiteralExpression
            }
            is PsiLiteralExpression -> expression
            else -> null
        }
    }
}
