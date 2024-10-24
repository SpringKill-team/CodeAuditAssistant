package org.skgroup.securityinspector.rules.misc

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class BroadCORSAllowOrigin : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.BroadCORSAllowOrigin")

        private val BROADCORSALLOWORIGIN_METHOD_SINKS = mapOf(
            "javax.servlet.http.HttpServletResponse" to listOf("setHeader", "addHeader"),
            "org.springframework.web.cors.CorsConfiguration" to listOf("addAllowedOrigin", "applyPermitDefaultValues"),
            "org.springframework.web.servlet.config.annotation.CorsRegistry" to listOf("addMapping")
        )

        private const val CORS_ANNOTATION = "org.springframework.web.bind.annotation.CrossOrigin"
        private const val ALLOWED_ORIGINS_VALUE = "*"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                if (CORS_ANNOTATION == annotation.qualifiedName) {
                    checkAnnotationForVulnerability(annotation, holder)
                }
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                BROADCORSALLOWORIGIN_METHOD_SINKS.forEach { (className, methods) ->
                    methods.forEach { methodName ->
                        if (SecExpressionUtils.hasFullQualifiedName(expression, className, methodName)) {
                            handleMethodCallExpression(expression, className, methodName, holder)
                        }
                    }
                }
            }
        }
    }

    private fun checkAnnotationForVulnerability(annotation: PsiAnnotation, holder: ProblemsHolder) {
        val nameValuePairs = annotation.parameterList.attributes
        if (nameValuePairs.isEmpty()) {
            holder.registerProblem(annotation, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        } else {
            nameValuePairs.forEach { pair ->
                val attributeName = pair.attributeName
                val value = pair.literalValue
                if ((attributeName == "value" || attributeName == "origins") && ALLOWED_ORIGINS_VALUE == value) {
                    holder.registerProblem(annotation, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }
        }
    }

    private fun handleMethodCallExpression(
        expression: PsiMethodCallExpression,
        className: String,
        methodName: String,
        holder: ProblemsHolder
    ) {
        val args = expression.argumentList.expressions

        when (className) {
            "javax.servlet.http.HttpServletResponse" -> {
                if (args.size == 2 && args[0] is PsiLiteralExpression && args[1] is PsiLiteralExpression) {
                    val headerName = (args[0] as PsiLiteralExpression).value as? String
                    val headerValue = (args[1] as PsiLiteralExpression).value as? String
                    if ("access-control-allow-origin".equals(
                            headerName,
                            ignoreCase = true
                        ) && ALLOWED_ORIGINS_VALUE == headerValue
                    ) {
                        holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    }
                }
            }

            "org.springframework.web.cors.CorsConfiguration" -> {
                if (methodName == "addAllowedOrigin" && args.size == 1) {
                    if (args[0] is PsiLiteralExpression && ALLOWED_ORIGINS_VALUE == (args[0] as PsiLiteralExpression).value) {
                        holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    } else if (args[0] is PsiReferenceExpression) {
                        val refArg = args[0] as PsiReferenceExpression
                        if ("CorsConfiguration.ALL" == refArg.qualifiedName) {
                            holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                        }
                    }
                } else if (methodName == "applyPermitDefaultValues") {
                    holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }

            "org.springframework.web.servlet.config.annotation.CorsRegistry" -> {
                if (methodName == "addMapping") {
                    var parent = expression.parent
                    var foundAllowedOriginsSetup = false

                    // 查找是否设置了 allowedOrigins
                    while (parent !is PsiCodeBlock) {
                        if (parent is PsiMethodCallExpression &&
                            SecExpressionUtils.hasFullQualifiedName(
                                parent,
                                "org.springframework.web.servlet.config.annotation.CorsRegistration",
                                "allowedOrigins"
                            )
                        ) {
                            foundAllowedOriginsSetup = true
                            val allowedOriginArgs = parent.argumentList.expressions
                            if (allowedOriginArgs.size == 1 && allowedOriginArgs[0] is PsiLiteralExpression &&
                                ALLOWED_ORIGINS_VALUE == (allowedOriginArgs[0] as PsiLiteralExpression).value
                            ) {
                                holder.registerProblem(parent, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                            }
                            break
                        }
                        parent = parent.parent
                    }

                    if (!foundAllowedOriginsSetup) {
                        holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    }
                }
            }
        }
    }
}
