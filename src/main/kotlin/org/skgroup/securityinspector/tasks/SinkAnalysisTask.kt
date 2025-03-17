package org.skgroup.securityinspector.tasks

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.skgroup.securityinspector.analysis.ast.ProjectIssue
import org.skgroup.securityinspector.visitors.SinkAnalysisJavaVisitor

/**
 * 类描述：SinkAnalysisTask 类用于创建Sink点查找任务。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/3/13
 */
class SinkAnalysisTask(
    project: Project,
    private val chunkSize: Int,
    private val callback: (List<ProjectIssue>) -> Unit
) : Task.Backgroundable(project, "Analyzing sink methods", true) {
    private val issues = mutableListOf<ProjectIssue>()

    override fun run(indicator: ProgressIndicator) {
        ApplicationManager.getApplication().runReadAction {
            val javaFiles = getJavaFiles(project)
            processFilesInChunks(javaFiles, indicator)
        }
    }

    private fun getJavaFiles(project: Project): Sequence<VirtualFile> {
        return FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project)).asSequence()
    }

    private fun processFilesInChunks(javaFiles: Sequence<VirtualFile>, indicator: ProgressIndicator) {
        javaFiles.chunked(chunkSize).forEachIndexed { index, chunk ->
            if (indicator.isCanceled) return

            updateProgressIndicator(indicator, index, javaFiles.count())
            processFileChunk(project, chunk, indicator)

            ApplicationManager.getApplication().invokeLater {
                callback(issues.toList())
            }
        }
    }

    private fun updateProgressIndicator(indicator: ProgressIndicator, index: Int, totalFiles: Int) {
        indicator.text = "Processing files ${index * chunkSize + 1}~${(index + 1) * chunkSize}"
        indicator.fraction = index.toDouble() / (totalFiles / chunkSize)
    }

    private fun processFileChunk(project: Project, files: List<VirtualFile>, indicator: ProgressIndicator) {
        ApplicationManager.getApplication().runReadAction {
            val manager = PsiManager.getInstance(project)
            files.forEach { virtualFile ->
                if (indicator.isCanceled || !virtualFile.isValid || virtualFile.path.contains("src/test")) return@forEach

                val psiFile = manager.findFile(virtualFile) as? PsiJavaFile ?: return@forEach
                psiFile.accept(SinkAnalysisJavaVisitor(project, virtualFile, psiFile, issues, indicator))
            }
        }
    }
}

