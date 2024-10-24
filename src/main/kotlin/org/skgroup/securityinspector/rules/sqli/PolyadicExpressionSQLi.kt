package org.skgroup.securityinspector.rules.sqli

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SQLiUtil
import org.skgroup.securityinspector.utils.SecExpressionUtils

class PolyadicExpressionSQLi : BaseSQLi() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.PolyadicExpressionSQLi")
        private val PLACEHOLDER_MESSAGE = PlaceholderStringSQLi.MESSAGE
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitPolyadicExpression(expression: PsiPolyadicExpression) {
                val exps = SecExpressionUtils.deconPolyadicExpression(expression)
                if (exps.isEmpty() || ignoreMethodName(expression)) {
                    return
                }

                val expStr = exps.joinToString(separator = "") { SecExpressionUtils.getText(it, true).orEmpty() }

                if (isSql(expStr)) {
                    val sqlSegments = mutableListOf<String>()
                    val sb = StringBuilder()
                    var hasVar = false

                    for (exp in exps) {
                        if (SecExpressionUtils.isSqliCareExpression(exp)) {
                            val s = SecExpressionUtils.getLiteralInnerText(exp)
                            if (s == null) {
                                if (sb.isNotEmpty()) {
                                    sqlSegments.add(sb.toString())
                                }

                                if (!SecExpressionUtils.isText(exp)) {
                                    hasVar = true
                                }

                                sb.append(" ? ")
                            } else {
                                sb.append(s)
                            }
                        } else {
                            sb.append(" ? ")
                        }
                    }

                    if (sqlSegments.isEmpty() || !hasVar || !hasEvalAdditive(sqlSegments)) {
                        if (hasPlaceholderProblem(expStr)) {
                            holder.registerProblem(
                                expression,
                                PLACEHOLDER_MESSAGE,
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                            )
                        }
                        return
                    }

                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            private fun hasPlaceholderProblem(content: String): Boolean {
                return SQLiUtil.placeholderPattern.matcher(content).find() &&
                        isSql(content) &&
                        hasEvalAdditive(content, SQLiUtil.placeholderPattern)
            }
        }
    }
}
