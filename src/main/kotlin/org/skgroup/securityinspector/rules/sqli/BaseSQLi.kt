package org.skgroup.securityinspector.rules.sqli

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.psiutils.MethodCallUtils
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.SQLiUtil
import org.skgroup.securityinspector.utils.SecExpressionUtils

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

    protected fun isSql(str: String): Boolean {
        return SQL_PATTERN.matcher(str).find()
    }

    protected fun hasEvalAdditive(content: String, pattern: Pattern): Boolean {
        val matcher = pattern.matcher(content)
        val prefixes = mutableListOf<String>()
        while (matcher.find()) {
            prefixes.add(content.substring(0, matcher.start()))
        }
        return hasEvalAdditive(prefixes)
    }

    protected fun hasEvalAdditive(prefixes: List<String>): Boolean {
        return prefixes.any { SQLiUtil.hasVulOnSQLJoinStr(it, null, null) }
    }

    protected fun ignoreMethodName(expression: PsiExpression): Boolean {
        var methodCallExpression = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression::class.java)
        if (methodCallExpression != null &&
            SecExpressionUtils.hasFullQualifiedName(methodCallExpression, "java.lang.String", "format")
        ) {
            methodCallExpression =
                PsiTreeUtil.getParentOfType(methodCallExpression, PsiMethodCallExpression::class.java)
        }

        methodCallExpression?.let {
            val methodName = MethodCallUtils.getMethodName(it)?.lowercase() ?: return false
            return EXCLUDED_METHOD_NAMES.any { excluded -> methodName.contains(excluded) }
        }

        return false
    }
}
