package org.skgroup.securityinspector.rules.ssrf

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class SpringSSRF : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.SpringSSRF")

        private val SPRINGSSRF_METHOD_SINKS = mapOf(
            "org.springframework.web.reactive.function.client.WebClient" to listOf("create", "baseUrl"),
            "org.springframework.web.client.RestTemplate" to listOf(
                "getForEntity", "exchange", "execute", "getForObject",
                "postForEntity", "postForObject", "put", "headForHeaders",
                "optionsForAllow", "delete"
            ),
            "org.apache.http.client.HttpClient" to listOf("execute"),
            "org.apache.http.impl.client.CloseableHttpClient" to listOf("execute"),
            "java.net.URL" to listOf("openConnection"),
            "java.net.HttpURLConnection" to listOf("connect", "setRequestMethod")
        )

        private val SPRINGSSRF_NEWEXPRESSIONS_SINKS = listOf(
            "org.apache.http.client.methods.HttpGet",
            "org.apache.http.client.methods.HttpPost",
            "org.apache.http.client.methods.HttpPut",
            "org.apache.http.client.methods.HttpDelete",
            "java.net.URL"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, SPRINGSSRF_METHOD_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.isNewExpressionSink(expression, SPRINGSSRF_NEWEXPRESSIONS_SINKS)) {
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

