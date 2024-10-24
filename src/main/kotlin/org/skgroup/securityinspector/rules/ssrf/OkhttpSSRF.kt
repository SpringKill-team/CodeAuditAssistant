package org.skgroup.securityinspector.rules.ssrf

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils
import org.jetbrains.annotations.NotNull

class OkhttpSSRF : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.OkhttpSSRF")

        private val OKHTTP_METHOD_SINKS = mapOf(
            "com.squareup.okhttp.Request\$Builder" to listOf("url"),
            "okhttp3.Request\$Builder" to listOf("url"),
            "com.squareup.okhttp.Call" to listOf("execute"),
            "okhttp3.Call" to listOf("execute"),
            "com.squareup.okhttp.OkHttpClient" to listOf("newCall"),
            "okhttp3.OkHttpClient" to listOf("newCall"),
            "com.squareup.okhttp.Request" to listOf("get", "post", "put", "delete"),
            "okhttp3.Request" to listOf("get", "post", "put", "delete", "head", "patch")
        )
    }

    override fun buildVisitor(@NotNull holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, OKHTTP_METHOD_SINKS)) {
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


