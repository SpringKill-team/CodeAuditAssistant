package org.skgroup.securityinspector.rules.jdbc

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

import org.jetbrains.annotations.NotNull

class JDBCAttack : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.JDBCAttack")

        // 定义常见的 JDBC sinks (方法)
        private val JDBCATTACK_METHOD_SINKS = mapOf(
            "javax.sql.DataSource" to listOf("getConnection"),
            "java.sql.Driver" to listOf("connect"),
            "java.sql.DriverManager" to listOf("getConnection"),
            "org.springframework.jdbc.DataSourceBuilder" to listOf("url"),
            "org.jdbi.v3.core.Jdbi" to listOf("create", "open"),
            "com.zaxxer.hikari.HikariConfig" to listOf("setJdbcUrl"),
            "org.springframework.jdbc.datasource.AbstractDriverBasedDataSource" to listOf("setUrl"),
            "org.apache.commons.dbcp2.BasicDataSource" to listOf("setUrl"),
            "com.mchange.v2.c3p0.ComboPooledDataSource" to listOf("setJdbcUrl"),
            "org.h2.jdbcx.JdbcDataSource" to listOf("setUrl"),  // H2 数据库
            "org.h2.Driver" to listOf("connect")                // H2 驱动
        )

        // 定义常见的 JDBC sinks (类实例化)
        private val JDBCATTACK_NEWEXPRESSION_SINKS = listOf(
            "org.springframework.jdbc.datasource.DriverManagerDataSource",
            "com.zaxxer.hikari.HikariConfig",
            "org.apache.commons.dbcp2.BasicDataSource",  // Apache DBCP
            "com.mchange.v2.c3p0.ComboPooledDataSource", // C3P0
            "org.h2.jdbcx.JdbcDataSource"               // H2 数据库
        )
    }

    @NotNull
    override fun buildVisitor(@NotNull holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, JDBCATTACK_METHOD_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.isNewExpressionSink(expression, JDBCATTACK_NEWEXPRESSION_SINKS)) {
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


