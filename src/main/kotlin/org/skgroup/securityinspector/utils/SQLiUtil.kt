package com.skgroup.securityinspector.utils

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.regex.Pattern

object SQLiUtil {

    private val whereInEndPattern = Pattern.compile("(where|and|or)\\s+\\S+?\\s+in\\s*\\(?\\s*$", Pattern.CASE_INSENSITIVE)
    private val likeEndPattern = Pattern.compile("\\S+?\\s+like\\s+('|\")%?$", Pattern.CASE_INSENSITIVE)
    private val placeholderPattern = Pattern.compile("%(\\d\\$\\d{0,5})?s", Pattern.CASE_INSENSITIVE)
    private val dollarVarPattern = Pattern.compile("\\$\\{(\\S+?)\\}")

    /**
     * 检测SQL拼接点列表是否存在注入风险
     * @param SQLJoinStrList List<List<String>>
     * @return Boolean
     */
    fun hasVulOnSQLJoinStrList(@NotNull SQLJoinStrList: List<List<String>>): Boolean {
        return SQLJoinStrList.any { sqlJoinStr ->
            when (sqlJoinStr.size) {
                2 -> hasVulOnSQLJoinStr(sqlJoinStr[0], sqlJoinStr[1], null)
                3 -> hasVulOnSQLJoinStr(sqlJoinStr[0], sqlJoinStr[1], sqlJoinStr[2])
                else -> false
            }
        }
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
