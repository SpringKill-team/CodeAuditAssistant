package org.skgroup.securityinspector.rules.files.read

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.jetbrains.annotations.NotNull
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

/**
 * 检查任意文件读取漏洞
 */
class ReadFile : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.ReadFile")
        private val READFILE_METHOD_SINKS: Map<String, List<String>> = mapOf(
            "java.lang.Class" to listOf("getResourceAsStream"),
            "org.apache.commons.io.FileUtils" to listOf(
                "readFileToByteArray",
                "readFileToString",
                "readLines"
            ),
            "java.nio.file.Files" to listOf(
                "readAllBytes",
                "readAllLines"
            ),
            "java.io.BufferedReader" to listOf("readLine")
        )
    }

    @NotNull
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, READFILE_METHOD_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.hasFullQualifiedName(expression, "java.io.FileInputStream")
                    || SecExpressionUtils.hasFullQualifiedName(expression, "java.io.FileReader")
                ) {
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
