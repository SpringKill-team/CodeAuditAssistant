package org.skgroup.securityinspector.utils

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.InspectionTool
import org.skgroup.securityinspector.rules.dos.NettyResponseSplitting
import org.skgroup.securityinspector.rules.ssrf.JavaURLSSRF

class InspectionRunner {

    private val inspectionTools: List<InspectionTool> = listOf(
        NettyResponseSplitting(),
        JavaURLSSRF()
    )

    fun scanDirectory(
        virtualFile: VirtualFile,
        project: Project,
        resultCallback: (String) -> Unit
    ) {
        val psiManager = PsiManager.getInstance(project)
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val fileTypeManager = FileTypeManager.getInstance()

        // 如果是目录，递归处理其子文件
        if (virtualFile.isDirectory) {
            virtualFile.children.forEach { child ->
                scanDirectory(child, project, resultCallback)
            }
        } else {
            ApplicationManager.getApplication().runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                if (document != null) {
                    PsiDocumentManager.getInstance(project).commitDocument(document)
                }
                val contentRoot = projectFileIndex.getContentRootForFile(virtualFile)
                if (contentRoot != null) {
                    val psiFile = psiManager.findFile(virtualFile)
                    psiFile?.let {
                        if (fileTypeManager.isFileOfType(virtualFile, fileTypeManager.getFileTypeByExtension("java"))) {
                            val problems = runInspectionsOnPsiFile(psiFile, project)
                            problems.forEach { problem ->
                                val lineNumber = problem.psiElement.textRange.startOffset
                                resultCallback("${virtualFile.path}:$lineNumber - ${problem.descriptionTemplate}")
                            }
                        }
                    }
                }
            }
        }
    }
    /*fun scanDirectory(
        virtualFile: VirtualFile,
        project: Project,
        resultCallback: (String) -> Unit
    ) {
        val psiManager = PsiManager.getInstance(project)
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val fileTypeManager = FileTypeManager.getInstance()

        if (virtualFile.isDirectory) {
            virtualFile.children.forEach { child ->
                scanDirectory(child, project, resultCallback)
            }
        } else {
            ApplicationManager.getApplication().runReadAction {
                val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                if (document != null) {
                    PsiDocumentManager.getInstance(project).commitDocument(document)
                }
                if (projectFileIndex.isInSourceContent(virtualFile)) {
                    val psiFile = psiManager.findFile(virtualFile)
                    psiFile?.let {
                        if (fileTypeManager.isFileOfType(virtualFile, fileTypeManager.getFileTypeByExtension("java"))) {
                            val problems = runInspectionsOnPsiFile(psiFile, project)
                            problems.forEach { problem ->
                                val lineNumber = problem.psiElement.textRange.startOffset
                                resultCallback("${virtualFile.path}:$lineNumber - ${problem.descriptionTemplate}")
                            }
                        }
                    }
                }
            }
        }
    }*/


    private fun runInspectionsOnPsiFile(psiFile: PsiFile, project: Project): List<ProblemDescriptor> {
        val problemDescriptors = mutableListOf<ProblemDescriptor>()
        val inspectionManager = InspectionManager.getInstance(project)
        val problemsHolder = ProblemsHolder(inspectionManager, psiFile, false)

        inspectionTools.forEach { tool ->
            tool.inspectFile(psiFile, problemsHolder)
            println("Running inspection: ${tool.javaClass.simpleName} on file: ${psiFile.name}")
        }

        return problemsHolder.results.toList()
    }
}


