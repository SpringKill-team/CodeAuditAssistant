package org.skgroup.securityinspector.rules.files.write

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethodCallExpression
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

import org.jetbrains.annotations.NotNull

class IOFiles : BaseLocalInspectionTool() {
    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.IOFilesWrite")

        private val IOFILES_METHOD_SINKS = mapOf(
            "java.io.FileOutputStream" to listOf("write"),
            "java.nio.file.Files" to listOf(
                "write", "writeString", "copy", "move", "createDirectory", "createFile",
                "createLink", "createSymbolicLink", "createTempDirectory", "createTempFile",
                "createDirectories"
            ),
            "org.springframework.web.multipart.MultipartFile" to listOf("transferTo"),
            "org.apache.tomcat.http.fileupload.FileItem" to listOf("FileItem"),
            "javax.servlet.http.Part" to listOf("write"),
            "java.io.PrintWriter" to listOf("print", "write", "format", "printf", "println"),
            "java.io.RandomAccessFile" to listOf("write", "writeBytes", "writeChars", "writeUTF"),
            "org.springframework.util.FileCopyUtils" to listOf("copy"),
            "java.io.FileWriter" to listOf("append", "write"),
            "java.io.Writer" to listOf("append", "write"),
            "java.io.BufferedWriter" to listOf("write"),
            "java.io.OutputStream" to listOf("write"),
            "java.io.ByteArrayOutputStream" to listOf("writeTo"),
            "java.io.BufferedOutputStream" to listOf("write"),
            "java.io.DataOutputStream" to listOf("writeByte", "writeBytes", "writeChars", "writeUTF"),
            "java.io.OutputStreamWriter" to listOf("write", "append"),
            "java.io.ObjectOutputStream" to listOf("writeObject"),
//    "java.io.PrintStream" to listOf("append", "format", "print", "printf", "println", "write", "writeBytes")
        )
    }

    @NotNull
    override fun buildVisitor(@NotNull holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, IOFILES_METHOD_SINKS)) {
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
