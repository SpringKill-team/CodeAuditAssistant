package org.skgroup.securityinspector.sinkrules.rce

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class RuntimeRCE : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.RuntimeRCE")

        private val RUNTIME_METHOD_SINKS = mapOf(
            "java.lang.Runtime" to listOf("exec"),
            "java.lang.ProcessBuilder" to listOf()
        )

        private val RUNTIME_NEWEXPRESSION_SINKS = listOf(
            "java.lang.ProcessBuilder"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, RUNTIME_METHOD_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
                val prj = expression.project
                fun getAllJavaPsiFiles(project: Project): List<PsiFile> {
                    val psiFiles = mutableListOf<PsiFile>()
                    val javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java")
                    val psiManager = PsiManager.getInstance(project)

                    VfsUtilCore.visitChildrenRecursively(project.baseDir, object : VirtualFileVisitor<Any>() {
                        override fun visitFile(file: VirtualFile): Boolean {
                            if (!file.isDirectory && file.fileType == javaFileType) {
                                psiManager.findFile(file)?.let {
                                    psiFiles.add(it)
                                }
                            }
                            return true
                        }
                    })

                    return psiFiles
                }
                getAllJavaPsiFiles(prj)
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.isNewExpressionSink(expression, RUNTIME_NEWEXPRESSION_SINKS)) {
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

