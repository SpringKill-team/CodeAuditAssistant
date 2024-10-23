package org.skgroup.securityinspector.SSRF

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

/**
 * 1067
 * SSRF Sinks in Google Guava's Resources class
 * com/google/common/io/Resources asByteSource (Ljava/net/URL;) 1 false
 * com/google/common/io/Resources asCharSource (Ljava/net/URL;Ljava/nio/charset/Charset;) 1 false
 * com/google/common/io/Resources copy (Ljava/net/URL;Ljava/io/OutputStream;) 1 false
 * com/google/common/io/Resources readLines * 1 false
 * com/google/common/io/Resources toByteArray (Ljava/net/URL;) 1 false
 * com/google/common/io/Resources toString (Ljava/net/URL;Ljava/nio/charset/Charset;) 1 false
 */
class GoogleIOSSRF : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("google.ssrf.msg")

        // 将 SSRF sink 方法名存为列表，方便扩展
        private val SSRF_METHODS = listOf(
            "asByteSource",
            "asCharSource",
            "copy",
            "readLines",
            "toByteArray",
            "toString"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (isSSRFMethod(expression)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
        }
    }

    // 检查是否是 SSRF sink 方法
    private fun isSSRFMethod(expression: PsiMethodCallExpression): Boolean {
        val methodName = expression.methodExpression.referenceName ?: return false
        return SecExpressionUtils.hasFullQualifiedName(expression, "com.google.common.io.Resources", methodName)
                && SSRF_METHODS.contains(methodName)
    }
}
