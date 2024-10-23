package org.skgroup.securityinspector.SSRF

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils


class ApacheSSRF : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("apache.ssrf.msg")

        // 定义具有潜在SSRF风险的类方法
        private val methodSinks = mapOf(
            "org.apache.http.client.fluent.Request" to listOf("Get", "Post"),
            "org.apache.http.client.methods.HttpRequestBase" to listOf("setURI"),
            "org.apache.http.client.methods.RequestBuilder" to listOf("get", "post", "put", "delete", "options", "head", "trace", "patch")
        )

        // 定义具有潜在SSRF风险的类构造函数
        private val constructorSinks = listOf(
            "org.apache.commons.httpclient.methods.GetMethod",
            "org.apache.commons.httpclient.methods.PostMethod",
            "org.apache.http.client.methods.HttpGet",
            "org.apache.http.client.methods.HttpHead",
            "org.apache.http.client.methods.HttpPost",
            "org.apache.http.client.methods.HttpPut",
            "org.apache.http.client.methods.HttpDelete",
            "org.apache.http.client.methods.HttpOptions",
            "org.apache.http.client.methods.HttpTrace",
            "org.apache.http.client.methods.HttpPatch",
            "org.apache.http.message.BasicHttpRequest",
            "org.apache.http.message.BasicHttpEntityEnclosingRequest",
            "java.net.HttpURLConnection"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (isMethodSink(expression)) {
                    holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (isConstructorSink(expression)) {
                    holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }

            /**
             * 检查方法调用是否属于已知的SSRF Sink
             */
            private fun isMethodSink(expression: PsiMethodCallExpression): Boolean {
                methodSinks.forEach { (className, methods) ->
                    if (methods.any { method -> SecExpressionUtils.hasFullQualifiedName(expression, className, method) }) {
                        return true
                    }
                }
                return false
            }

            /**
             * 检查类的构造方法是否属于已知的SSRF Sink
             */
            private fun isConstructorSink(expression: PsiNewExpression): Boolean {
                return constructorSinks.any { className ->
                    SecExpressionUtils.hasFullQualifiedName(expression, className)
                }
            }
        }
    }
}
