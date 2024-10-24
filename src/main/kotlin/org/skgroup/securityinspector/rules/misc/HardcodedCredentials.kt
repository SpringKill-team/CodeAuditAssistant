package org.skgroup.securityinspector.rules.misc

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.codeInspection.ProblemHighlightType
import me.gosimple.nbvcxz.Nbvcxz
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils


class HardcodedCredentials : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.HardCodedCredential")
        private val pattern = Regex("passwd|pass|password|pwd|secret|token", RegexOption.IGNORE_CASE)
        private val connPwdPattern = Regex("password=(.*?)($|&)", RegexOption.IGNORE_CASE)
        private const val entropyThreshold = 50.0
        private const val truncate = 16

        private val HARDCODE_METHOD_SINKS = mapOf(
            "java.util.Hashtable" to listOf("put"),
            "java.sql.DriverManager" to listOf("getConnection")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitLocalVariable(variable: PsiLocalVariable) {
                checkHardcodedCredentials(variable.name, variable.initializer, holder, variable)
            }

            override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
                val varName = (expression.lExpression as? PsiReferenceExpression)?.qualifiedName
                checkHardcodedCredentials(varName, expression.rExpression, holder, expression)
            }

            override fun visitField(field: PsiField) {
                checkHardcodedCredentials(field.name, field.initializer, holder, field)
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                HARDCODE_METHOD_SINKS.forEach { (className, methods) ->
                    methods.forEach { methodName ->
                        if (SecExpressionUtils.hasFullQualifiedName(expression, className, methodName)) {
                            handleMethodCall(expression, holder)
                        }
                    }
                }
            }
        }
    }

    private fun checkHardcodedCredentials(
        varName: String?,
        initializer: PsiExpression?,
        holder: ProblemsHolder,
        element: PsiElement
    ) {
        if (varName != null && pattern.containsMatchIn(varName)) {
            val value = initializer?.let { SecExpressionUtils.getLiteralInnerText(it) }
            if (value != null && isHighEntropyString(value) && isASCII(value)) {
                holder.registerProblem(element, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
            }
        }
    }

    private fun handleMethodCall(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val methodName = expression.methodExpression.referenceName
        val args = expression.argumentList.expressions

        when (methodName) {
            "put" -> handleHashtablePut(expression, args, holder)
            "getConnection" -> handleDriverManagerGetConnection(expression, args, holder)
        }
    }

    private fun handleHashtablePut(
        expression: PsiMethodCallExpression,
        args: Array<PsiExpression>,
        holder: ProblemsHolder
    ) {
        if (args.size == 2 && args[1] is PsiLiteralExpression) {
            val key = SecExpressionUtils.getText(args[0], true)
            if (key != null && pattern.containsMatchIn(key)) {
                val value = SecExpressionUtils.getLiteralInnerText(args[1])
                if (value != null && isHighEntropyString(value) && isASCII(value)) {
                    holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }
        }
    }

    private fun handleDriverManagerGetConnection(
        expression: PsiMethodCallExpression,
        args: Array<PsiExpression>,
        holder: ProblemsHolder
    ) {
        when (args.size) {
            1 -> {
                val connUrl = SecExpressionUtils.getLiteralInnerText(args[0] as? PsiLiteralExpression)
                if (connUrl != null && connPwdPattern.containsMatchIn(connUrl)) {
                    holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }
            3 -> {
                if (args[2] is PsiLiteralExpression) {
                    holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }
        }
    }

    private fun isHighEntropyString(v: String): Boolean {
        val truncatedValue = if (truncate < v.length) v.substring(0, truncate) else v
        return Nbvcxz().estimate(truncatedValue).entropy > entropyThreshold
    }

    /**
     * 判断该字符串是否以ascii组成
     * @param text String
     * @return boolean
     */
    private fun isASCII(text: String): Boolean {
        return text.all { it.toInt() <= 128 }
    }
}
