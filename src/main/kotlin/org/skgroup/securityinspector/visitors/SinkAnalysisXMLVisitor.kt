package org.skgroup.securityinspector.visitors

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.skgroup.securityinspector.analysis.ast.ProjectIssue
import org.skgroup.securityinspector.enums.SinkCallMode
import org.skgroup.securityinspector.enums.SubVulnerabilityDefinition

/**
 * 类描述：SinkAnalysisXMLVisitor 类用于。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/3/14
 */
class SinkAnalysisXMLVisitor(
    val project: Project,
    val virtualFile: VirtualFile,
    val psiFile: PsiJavaFile,
    val issues: MutableList<ProjectIssue>,
    val indicator: ProgressIndicator
) : XmlRecursiveElementVisitor() {

    private fun addIssue(
        element: PsiElement,
        className: String,
        methodName: String,
        sinkMatch: SubVulnerabilityDefinition
    ) {
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
        val line = document?.getLineNumber(element.textRange.startOffset)?.plus(1) ?: -1

        var callMode = SinkCallMode.SINGLE_SINK
        val method = (element as? PsiMethodCallExpression)?.resolveMethod()
            ?: (element as? PsiNewExpression)?.resolveMethod()
        val hasCall = method?.let {
            ReferencesSearch.search(it, ProjectScope.getProjectScope(project)).findFirst()
        } != null

        synchronized(issues) {
            if (hasCall) {
                callMode = SinkCallMode.HAS_CALL
            }
            issues.add(
                ProjectIssue(
                    virtualFile,
                    line,
                    className,
                    methodName,
                    sinkMatch.subType.parent.name,
                    sinkMatch.subType.name,
                    callMode
                )
            )
        }
    }
}
