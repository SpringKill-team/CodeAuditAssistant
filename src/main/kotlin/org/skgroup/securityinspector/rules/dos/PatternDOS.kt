package com.skgroup.securityinspector.rules.dos

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.siyeh.ig.psiutils.MethodCallUtils
import com.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import com.skgroup.securityinspector.utils.InspectionBundle
import com.skgroup.securityinspector.utils.SecExpressionUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.regex.Pattern
/**
 * 1039
 * 正则表达式拒绝服务攻击 (RegexDos)
 *
 * 当编写校验的正则表达式存在缺陷时，攻击者可以构造特殊的字符串来大量消耗服务器的资源，造成服务中断或停止。
 * ref: https://cloud.tencent.com/developer/article/1041326
 *
 * check:
 * java.util.regex.Pattern#compile args:0
 * java.util.regex.Pattern#matches args:0
 *
 * fix:
 * (1) 优化正则表达式
 * (2) 使用 com.google.re2j 库
 *
 * notes:
 * `isExponentialRegex` 方法来源于 CodeQL
 */
const val PATTERN_DOS_MESSAGE = "pattern.matches.type.msg"

class PatternDOS : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message(PATTERN_DOS_MESSAGE)

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

    /**
     * 提取字面量表达式
     */
    @Nullable
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
