package org.skgroup.securityinspector.utils

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.siyeh.ig.psiutils.ExpressionUtils
import me.gosimple.nbvcxz.Nbvcxz
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.skgroup.securityinspector.analysis.ast.ProjectIssue
import org.skgroup.securityinspector.enums.SubVulnerabilityDefinition
import org.skgroup.securityinspector.enums.SubVulnerabilityType
import org.skgroup.securityinspector.tasks.SinkAnalysisTask
import java.util.regex.Pattern

/**
 * 类描述：SinkUtil 类是Sink点查找工具类
 *
 * @author springkill
 * @version 1.0
 */

object SinkUtil {

    val regexPatterns = listOf(
        ".*\\([^()*+\\]]+\\]?(\\*|\\+)\\)(\\*|\\+).*",   // ([a-z]+)+
        ".*\\((\\([^()]+\\)\\?)?\\([^()*+\\]]+\\]?(\\*|\\+)\\)\\)(\\*|\\+).*",  // (([a-z])?([a-z]+))+
        ".*\\(\\([^()*+\\]]+\\]?\\)(\\*|\\+)\\)(\\*|\\+).*",  // (([a-z])+)+
        ".*\\(([^()*+\\]]+\\]?)\\|\\1+\\??\\)(\\*|\\+).*",    // (a|aa)+
        ".*\\(\\.\\*[^()*+\\]]+\\]?\\)\\{[1-9][0-9]+,?[0-9]*\\}.*"  // (.*[a-z]){n} n >= 10
    )

    val pattern = Regex("passwd|pass|password|pwd|secret|token", RegexOption.IGNORE_CASE)
    val connPwdPattern = Regex("password=(.*?)($|&)", RegexOption.IGNORE_CASE)
    const val entropyThreshold = 50.0
    const val truncate = 16
    internal val dollarVarPattern = Pattern.compile("\\$\\{(\\S+?)\\}")

    fun collectProjectIssues(
        project: Project,
        chunkSize: Int = 50,
        callback: (List<ProjectIssue>) -> Unit,
    ) {
        DumbService.getInstance(project).runWhenSmart {
            val task = SinkAnalysisTask(project, chunkSize, callback)
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, EmptyProgressIndicator())
        }
    }

    fun patternDosMatcher(
        expression: PsiMethodCallExpression,
        callSink: SubVulnerabilityDefinition
    ): SubVulnerabilityDefinition? {
        val regexArg = expression.argumentList.expressions.firstOrNull()
        val literal = getLiteralExpression(regexArg)
        val literalText = getLiteralInnerText(literal)
        if (literal != null && literalText != null && isExponentialRegex(literalText)) {
            return callSink
        }
        return null
    }

    fun getLiteralExpression(expression: PsiExpression?): PsiLiteralExpression? {
        return when (expression) {
            is PsiReferenceExpression -> {
                val resolvedElement = expression.resolve() as? PsiVariable
                resolvedElement?.initializer as? PsiLiteralExpression
            }

            is PsiLiteralExpression -> expression
            else -> null
        }
    }

    fun getLiteralInnerText(expression: PsiExpression?): String? {
        val literal = ExpressionUtils.getLiteral(expression)
        return literal?.value?.toString()
    }

    fun isExponentialRegex(s: String): Boolean {
        return regexPatterns.any { Pattern.matches(it, s) }
    }

    fun checkHardcodedCredentials(expression: PsiMethodCallExpression): SubVulnerabilityDefinition? {
        val methodName = expression.methodExpression.referenceName
        val args = expression.argumentList.expressions

        when (methodName) {
            "put" -> return handleHashtablePut(args)
            "getConnection" -> return handleDriverManagerGetConnection(args)
        }
        return null
    }

    fun handleHashtablePut(
        args: Array<PsiExpression>
    ): SubVulnerabilityDefinition? {
        if (args.size == 2 && args[1] is PsiLiteralExpression) {
            val key = getText(args[0], true)
            if (key != null && pattern.containsMatchIn(key)) {
                val value = getLiteralInnerText(args[1])
                if (value != null && isHighEntropyString(value) && isASCII(value)) {
                    return SubVulnerabilityDefinition(
                        SubVulnerabilityType.HARDCODED_CREDENTIALS,
                        emptyMap(), emptySet(), true
                    )
                }
            }
        }
        return null
    }

    fun handleDriverManagerGetConnection(
        args: Array<PsiExpression>
    ): SubVulnerabilityDefinition? {
        when (args.size) {
            1 -> {
                val connUrl = getLiteralInnerText(args[0] as? PsiLiteralExpression)
                if (connUrl != null && connPwdPattern.containsMatchIn(connUrl)) {
                    return SubVulnerabilityDefinition(
                        SubVulnerabilityType.HARDCODED_CREDENTIALS,
                        emptyMap(), emptySet(), true
                    )
                }
            }

            3 -> {
                if (args[2] is PsiLiteralExpression) {
                    return SubVulnerabilityDefinition(
                        SubVulnerabilityType.HARDCODED_CREDENTIALS,
                        emptyMap(), emptySet(), true
                    )
                }
            }
        }
        return null
    }

    fun checkHardcodedCredentials(
        varName: String?,
        initializer: PsiExpression?,
    ): SubVulnerabilityDefinition? {
        if (varName != null && pattern.containsMatchIn(varName)) {
            val value = initializer?.let { getLiteralInnerText(it) }
            if (value != null && isHighEntropyString(value) && isASCII(value)) {
                return SubVulnerabilityDefinition(
                    SubVulnerabilityType.HARDCODED_CREDENTIALS,
                    emptyMap(), emptySet(), true
                )
            }
        }
        return null
    }

    fun isHighEntropyString(v: String): Boolean {
        val truncatedValue = if (truncate < v.length) v.substring(0, truncate) else v
        return Nbvcxz().estimate(truncatedValue).entropy > entropyThreshold
    }

    fun isASCII(text: String): Boolean {
        return text.all { it.toInt() <= 128 }
    }

    fun getText(expression: PsiExpression?, force: Boolean): String? {
        if (expression == null) return null

        var value = getLiteralInnerText(expression)

        if (value == null && (TypeConversionUtil.isPrimitiveAndNotNull(expression.type)
                    || PsiUtil.isConstantExpression(expression) && expression !is PsiPolyadicExpression)
        ) {
            value = expression.text
        }

        if (value == null && expression is PsiReferenceExpression) {
            val resolve = expression.resolve()
            if (resolve is PsiField) {
                val initializer = resolve.initializer
                value = initializer?.let { getText(it, false) } ?: resolve.name
            }
        }

        if (value == null && expression is PsiPolyadicExpression) {
            val sb = StringBuilder()
            expression.operands.forEach { operand ->
                val text = getText(operand, force)
                if (text == null) {
                    sb.setLength(0)
                    return@forEach
                }
                sb.append(text)
            }
            if (sb.isNotEmpty()) {
                value = sb.toString()
            }
        }

        if (force && value == null) {
            value = expression.text
        }
        return value
    }

    /**
     * 判断单个SQL拼接点是否有注入风险
     * @param prefix String
     * @param sqlVariable String
     * @param suffix String
     * @return Boolean
     */
    fun hasVulOnSQLJoinStr(@NotNull prefix: String, @Nullable sqlVariable: String?, @Nullable suffix: String?): Boolean {
        val fragments = prefix.split(Regex("[\\s|(]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 检查常见的 SQL 关键字及其上下文
        if (fragments.isEmpty() || endsWithComparisonOperator(fragments.last())) {
            return true
        }

        return fragments.reversed().any { frag ->
            when (frag.lowercase()) {
                "where", "set" -> suffixHasComparisonOperator(suffix)
                "values" -> true
                "from", "into", "join", "select", "update" -> return false
                else -> false
            }
        }
    }

    /**
     * 检查片段是否以比较运算符（=, >=, <=）结尾
     * @param fragment String
     * @return Boolean
     */
    private fun endsWithComparisonOperator(fragment: String): Boolean {
        return fragment.endsWith("=") || fragment.endsWith(">=") || fragment.endsWith("<=")
    }

    /**
     * 检查后缀是否包含比较运算符
     * @param suffix String?
     * @return Boolean
     */
    private fun suffixHasComparisonOperator(suffix: String?): Boolean {
        return suffix?.trim()?.let {
            it.startsWith("=") || it.startsWith(">") || it.startsWith("<")
        } == true
    }

}