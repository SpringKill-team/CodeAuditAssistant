package com.skgroup.securityinspector.rules.sqli

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.psiutils.MethodCallUtils
import com.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import com.skgroup.securityinspector.utils.SQLiUtil
import com.skgroup.securityinspector.utils.SecExpressionUtils

import java.util.regex.Pattern

abstract class BaseSQLi : BaseLocalInspectionTool() {

    companion object {
        private val SQL_PATTERN = Pattern.compile(
            "^\\s*(select|delete|update|insert)\\s+.*?(from|into|set)\\s+.*?where.*",
            Pattern.CASE_INSENSITIVE
        )

        private val EXCLUDED_METHOD_NAMES = setOf(
            "log", "trace", "debug", "info", "alarm", "warn", "error", "fatal",
            "ok", "succ", "fail", "print"
        )
    }

    /**
     * 判断指定字符串是否为SQL语句
     * @param str String
     * @return boolean
     */
    protected fun isSql(str: String): Boolean {
        return SQL_PATTERN.matcher(str).find()
    }

    /**
     * 按 needle 拆分字符串后，判断拆分数组是否有拼接SQL注入风险
     * @param content String
     * @param pattern Pattern
     * @return boolean
     */
    protected fun hasEvalAdditive(content: String, pattern: Pattern): Boolean {
        val matcher = pattern.matcher(content)
        val prefixes = mutableListOf<String>()
        while (matcher.find()) {
            prefixes.add(content.substring(0, matcher.start()))
        }
        return hasEvalAdditive(prefixes)
    }

    /**
     * 判断数组是否有拼接SQL注入风险
     * @param prefixes List<String>
     * @return boolean
     */
    protected fun hasEvalAdditive(prefixes: List<String>): Boolean {
        return prefixes.any { SQLiUtil.hasVulOnSQLJoinStr(it, null, null) }
    }

    /**
     * 判断是否忽略某些方法调用（如日志方法）
     * @param expression PsiExpression
     * @return boolean
     */
    protected fun ignoreMethodName(expression: PsiExpression): Boolean {
        var methodCallExpression = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression::class.java)
        if (methodCallExpression != null &&
            SecExpressionUtils.hasFullQualifiedName(methodCallExpression, "java.lang.String", "format")) {
            methodCallExpression = PsiTreeUtil.getParentOfType(methodCallExpression, PsiMethodCallExpression::class.java)
        }

        methodCallExpression?.let {
            val methodName = MethodCallUtils.getMethodName(it)?.lowercase() ?: return false
            return EXCLUDED_METHOD_NAMES.any { excluded -> methodName.contains(excluded) }
        }

        return false
    }
}
