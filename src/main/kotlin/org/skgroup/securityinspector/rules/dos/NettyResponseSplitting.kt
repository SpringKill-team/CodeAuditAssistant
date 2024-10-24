package org.skgroup.securityinspector.rules.dos

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils
import org.skgroup.securityinspector.inspectors.InspectionTool

class NettyResponseSplitting : BaseLocalInspectionTool(), InspectionTool {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.NettyResponseSplittingRisk")
        private val QUICK_FIX_NAME = InspectionBundle.message("vuln.fix.NettyResponseSplittingRisk")
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

    private fun checkForProblem(
        expression: PsiNewExpression,
        qualifiedName: String,
        argIndex: Int,
        holder: ProblemsHolder
    ) {
        if (SecExpressionUtils.hasFullQualifiedName(expression, qualifiedName)) {
            expression.argumentList?.expressions?.let { args ->
                if (args.size > argIndex && args[argIndex] is PsiLiteralExpression &&
                    (args[argIndex] as PsiLiteralExpression).value == false
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

    override fun inspectFile(psiFile: PsiFile, problemsHolder: ProblemsHolder) {
        psiFile.accept(buildVisitor(problemsHolder, false))
    }
}
