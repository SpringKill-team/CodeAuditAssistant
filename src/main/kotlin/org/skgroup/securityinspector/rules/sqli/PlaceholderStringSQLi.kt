package org.skgroup.securityinspector.rules.sqli

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ExpressionUtils
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SQLiUtil
import org.skgroup.securityinspector.utils.SecExpressionUtils

class PlaceholderStringSQLi : BaseSQLi() {

    companion object {
        internal val MESSAGE = InspectionBundle.message("vuln.massage.PlaceholderStringSQLi")

        private val PLACEHOLDER_METHOD_SINKS = mapOf(
            "java.lang.String" to listOf("format")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                PLACEHOLDER_METHOD_SINKS.forEach { (className, methods) ->
                    methods.forEach { methodName ->
                        if (!SecExpressionUtils.hasFullQualifiedName(expression, className, methodName)) {
                            return
                        }
                        val args = expression.argumentList.expressions
                        if (args.isEmpty() || ignoreMethodName(expression)) return

                        var idx = 0
                        val arg0Type = args[idx].type
                        if (arg0Type != null && "Locale" == arg0Type.presentableText) {
                            idx += 1
                        }
                        val content = getLiteralOrVariableLiteral(args[idx])

                        if (content != null &&
                            SQLiUtil.placeholderPattern.matcher(content).find() &&
                            isSql(content)
                        ) {
                            val splitContentByPlaceholder = SQLiUtil.placeholderPattern.split(content).toMutableList()
                            if (content.endsWith(splitContentByPlaceholder.last())) {
                                splitContentByPlaceholder.removeAt(splitContentByPlaceholder.size - 1)
                            }

                            val concatContent = mutableListOf<String>()
                            val sb = StringBuilder()
                            idx += 1
                            for (segment in splitContentByPlaceholder) {
                                sb.append(segment)
                                if (idx < args.size &&
                                    SecExpressionUtils.isSqliCareExpression(args[idx]) &&
                                    getLiteralOrVariableLiteral(args[idx]) == null
                                ) {
                                    concatContent.add(sb.toString())
                                }
                                sb.append(" ? ")
                                idx += 1
                            }

                            if (hasEvalAdditive(concatContent)) {
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
    }

    private fun getLiteralOrVariableLiteral(expression: PsiExpression?): String? {
        return when {
            ExpressionUtils.isLiteral(expression) -> SecExpressionUtils.getLiteralInnerText(expression)
            else -> {
                val localVariable = ExpressionUtils.resolveLocalVariable(expression)
                localVariable?.initializer?.takeIf { ExpressionUtils.isLiteral(it) }
                    ?.let { SecExpressionUtils.getLiteralInnerText(it) }
            }
        }
    }
}
