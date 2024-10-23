package com.skgroup.securityinspector.rules.dos

import com.intellij.codeInsight.completion.ml.JavaCompletionFeatures
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import com.skgroup.securityinspector.utils.InspectionBundle
import com.skgroup.securityinspector.utils.SecExpressionUtils
import org.jetbrains.annotations.Nls

/**
 * 1051: Netty响应拆分攻击
 *
 * ref:
 * (1) https://github.com/github/codeql/blob/main/java/ql/src/Security/CWE/CWE-113/NettyResponseSplitting.java
 * (2) http://www.infosecwriters.com/Papers/DCrab_HTTP_Response.pdf
 */
const val NETTY_RESPONSE_MESSAGE = "netty.response.splitting.msg"
const val NETTY_RESPONSE_FIX = "netty.response.splitting.fix"

class NettyResponseSplitting : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message(NETTY_RESPONSE_MESSAGE)
        private val QUICK_FIX_NAME = InspectionBundle.message(NETTY_RESPONSE_FIX)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitNewExpression(expression: PsiNewExpression) {
                checkForProblem(
                    expression,
                    "io.netty.handler.codec.http.DefaultHttpHeaders",
                    0,
                    holder
                )
                checkForProblem(
                    expression,
                    "io.netty.handler.codec.http.DefaultHttpResponse",
                    2,
                    holder
                )
            }
        }
    }

    /**
     * 通用方法：检查表达式是否有问题，并注册问题
     */
    private fun checkForProblem(
        expression: PsiNewExpression,
        qualifiedName: String,
        argIndex: Int,
        holder: ProblemsHolder
    ) {
        if (SecExpressionUtils.hasFullQualifiedName(expression, qualifiedName)) {
            expression.argumentList?.expressions?.let { args ->
                if (args.size > argIndex && args[argIndex] is PsiLiteralExpression &&
                    JavaCompletionFeatures.JavaKeyword.FALSE == (args[argIndex] as PsiLiteralExpression).value
                ) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        NettyResponseSplittingQuickFix(argIndex)
                    )
                }
            }
        }
    }

    /**
     * 快速修复类
     */
    class NettyResponseSplittingQuickFix(private val fixArgIndex: Int) : LocalQuickFix {

        override fun getFamilyName(): String {
            return QUICK_FIX_NAME
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? PsiNewExpression ?: return
            val args = expression.argumentList?.expressions ?: return
            if (args.size > fixArgIndex && args[fixArgIndex] is PsiLiteralExpression) {
                val problemExpression = args[fixArgIndex] as PsiLiteralExpression
                val factory = JavaPsiFacade.getElementFactory(project)
                problemExpression.replace(factory.createExpressionFromText("true", null))
            }
        }
    }
}
