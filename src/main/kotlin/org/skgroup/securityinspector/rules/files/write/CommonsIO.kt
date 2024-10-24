package org.skgroup.securityinspector.rules.files.write

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class CommonsIO : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.CommonIOFileWrite")
        private val COMMONSIO_METHOD_SINKS = mapOf(
            "org.apache.commons.io.file.PathFilter" to listOf("accept"),
            "org.apache.commons.io.file.PathUtils" to listOf("copyFile", "copyFileToDirectory"),
            "org.apache.commons.io.filefilter.FileFilterUtils" to listOf("filter", "filterList", "filterSet"),
            "org.apache.commons.io.output.DeferredFileOutputStream" to listOf("writeTo"),
            "org.apache.commons.io.output.FileWriterWithEncoding" to listOf("write"),
            "org.apache.commons.io.output.LockableFileWriter" to listOf("write"),
            "org.apache.commons.io.output.XmlStreamWriter" to listOf("write"),
            "org.apache.commons.io.FileUtils" to listOf(
                "copyDirectory", "copyDirectoryToDirectory", "copyFile", "copyFileToDirectory",
                "copyInputStreamToFile", "copyToDirectory", "copyToFile", "moveDirectory",
                "moveDirectoryToDirectory", "moveFile", "moveFileToDirectory", "touch", "write",
                "writeByteArrayToFile", "writeLines", "writeStringToFile"
            ),
            "org.apache.commons.io.IOUtils" to listOf("copy"),
            "org.apache.commons.io.RandomAccessFileMode" to listOf("create"),
            "org.apache.commons.fileupload" to listOf("write")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, COMMONSIO_METHOD_SINKS)) {
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
