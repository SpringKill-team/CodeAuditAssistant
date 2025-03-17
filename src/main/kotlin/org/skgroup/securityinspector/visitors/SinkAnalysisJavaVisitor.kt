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
import org.skgroup.securityinspector.enums.SubVulnerabilityType
import org.skgroup.securityinspector.utils.SinkList
import org.skgroup.securityinspector.utils.SinkUtil.checkHardcodedCredentials
import org.skgroup.securityinspector.utils.SinkUtil.patternDosMatcher

/**
 * 类描述：SinkAnalysisVisitor 类用于创建sink点访问者。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/3/13
 */
class SinkAnalysisJavaVisitor(
    val project: Project,
    val virtualFile: VirtualFile,
    val psiFile: PsiJavaFile,
    val issues: MutableList<ProjectIssue>,
    val indicator: ProgressIndicator
) : JavaRecursiveElementWalkingVisitor() {
    override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
        if (!call.isValid || indicator.isCanceled) return

        val methodName = call.methodExpression.referenceName ?: return
        val className = call.resolveMethod()?.containingClass?.qualifiedName ?: return

        var sinkMatch = SinkList.ALL_SUB_VUL_DEFINITIONS.firstOrNull { callSink ->
            callSink.methodSinks[className]?.contains(methodName) == true
        } ?: return

        sinkMatch = when (sinkMatch.subType) {
            SubVulnerabilityType.PATTERN_DOS -> patternDosMatcher(call, sinkMatch) ?: return
            SubVulnerabilityType.HARDCODED_CREDENTIALS -> checkHardcodedCredentials(call) ?: return
            else -> sinkMatch
        }

        addIssue(call, className, methodName, sinkMatch)
    }

    override fun visitNewExpression(new: PsiNewExpression) {
        if (!new.isValid || indicator.isCanceled) return

        val methodName = "<init>"
        val className = new.classReference?.qualifiedName ?: return

        val sinkMatch = SinkList.ALL_SUB_VUL_DEFINITIONS.firstOrNull { conSink ->
            conSink.constructorSinks.contains(className)
        } ?: return

        addIssue(new, className, methodName, sinkMatch)
    }

    override fun visitLocalVariable(variable: PsiLocalVariable) {
        checkHardcodedCredentials(variable.name, variable.initializer)
    }

    override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
        val varName = (expression.lExpression as? PsiReferenceExpression)?.qualifiedName
        checkHardcodedCredentials(varName, expression.rExpression)
    }

    override fun visitField(field: PsiField) {
        checkHardcodedCredentials(field.name, field.initializer)
    }

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
