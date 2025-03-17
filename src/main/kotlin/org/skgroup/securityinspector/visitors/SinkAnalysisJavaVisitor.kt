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

class SinkAnalysisJavaVisitor(
    val project: Project,
    val virtualFile: VirtualFile,
    val psiFile: PsiJavaFile,
    val issues: MutableList<ProjectIssue>,
    val indicator: ProgressIndicator
) : JavaRecursiveElementWalkingVisitor() {

    // 定义需要检查的MyBatis注解及其属性
    private val mybatisAnnotations = mapOf(
        "org.apache.ibatis.annotations.Select" to listOf("value"),
        "org.apache.ibatis.annotations.Delete" to listOf("value"),
        "org.apache.ibatis.annotations.Update" to listOf("value"),
        "org.apache.ibatis.annotations.Insert" to listOf("value")
    )

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

    override fun visitMethod(method: PsiMethod) {
        super.visitMethod(method)
        if (indicator.isCanceled) return

        // 检查方法上的MyBatis注解
        method.annotations.forEach { annotation ->
            val annotationFqn = annotation.qualifiedName ?: return@forEach
            val attributes = mybatisAnnotations[annotationFqn] ?: return@forEach

            attributes.forEach { attrName ->
                val attrValue = annotation.findAttributeValue(attrName)
                if (attrValue != null && containsDollarBrace(attrValue)) {
                    val className = method.containingClass?.qualifiedName ?: "UnknownClass"
                    val sinkMatch = SinkList.ALL_SUB_VUL_DEFINITIONS.firstOrNull {
                        it.subType == SubVulnerabilityType.MYBATIS_XML_SQLI
                    } ?: return@forEach
                    addIssue(annotation, className, method.name, sinkMatch)
                }
            }
        }
    }

    private fun containsDollarBrace(value: PsiAnnotationMemberValue): Boolean {
        return when (value) {
            is PsiLiteralExpression -> (value.value as? String)?.contains("\${") == true
            is PsiArrayInitializerMemberValue -> value.initializers.any { expr ->
                expr is PsiLiteralExpression && (expr.value as? String)?.contains("\${") == true
            }
            else -> false
        }
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