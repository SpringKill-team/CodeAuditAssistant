package org.skgroup.securityinspector.visitors

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.xml.*
import com.intellij.xml.util.XmlUtil
import org.skgroup.securityinspector.analysis.ast.ProjectIssue
import org.skgroup.securityinspector.enums.SinkCallMode
import org.skgroup.securityinspector.enums.SubVulnerabilityDefinition
import org.skgroup.securityinspector.enums.SubVulnerabilityType
import org.skgroup.securityinspector.utils.SinkList
import org.skgroup.securityinspector.utils.SinkUtil

class SinkAnalysisXMLVisitor(
    val project: Project,
    val virtualFile: VirtualFile,
    val psiFile: PsiJavaFile,
    val issues: MutableList<ProjectIssue>,
    val indicator: ProgressIndicator
) : XmlRecursiveElementVisitor() {

    private val ignoreVarName = setOf(
        "orderByClause", "pageStart", "pageSize", "criterion.condition", "alias"
    )

    override fun visitXmlText(text: XmlText) {
        if (indicator.isCanceled) return
        super.visitXmlText(text)

        // 跳过不需要检查的父标签
        if (shouldIgnoreParentTag(text.parentTag)) return

        // 验证是否为MyBatis XML文件
        val document = XmlUtil.getContainingFile(text)?.document ?: return
        val dtd = XmlUtil.getDtdUri(document)
        if (!isMyBatisDtd(dtd)) return

        // 检测${}
        val textValue = text.value
        if (textValue.isEmpty() || !textValue.contains("\${")) return

        // 匹配所有变量并检测漏洞
        val matcher = SinkUtil.dollarVarPattern.matcher(textValue)
        var offset = 0
        var count = 0

        while (matcher.find(offset) && count++ < 9999) {
            val prefix = textValue.substring(0, matcher.start())
            val variable = matcher.group(1)
            val suffix = textValue.substring(matcher.end())

            if (!ignorePosition(prefix, variable, suffix) &&
                SinkUtil.hasVulOnSQLJoinStr(prefix, variable, suffix)) {
                addIssue(
                    element = text,
                    className = "MyBatis XML Mapper",
                    methodName = text.parentTag?.name ?: "UnknownTag",
                    sinkMatch = SinkList.getSQLiDefinition()
                )
                break
            }
            offset = matcher.end()
        }
    }

    private fun shouldIgnoreParentTag(parentTag: XmlTag?): Boolean {
        return parentTag != null && (parentTag.name == "sql" || parentTag.name == "mapper")
    }

    private fun isMyBatisDtd(dtd: String?): Boolean {
        return dtd != null && dtd.contains("mybatis.org") && dtd.contains("mapper.dtd")
    }

    private fun ignorePosition(prefix: String, variable: String, suffix: String): Boolean {
        return ignoreVarName.contains(variable) || variable.startsWith("ew.")
    }

    private fun addIssue(
        element: PsiElement,
        className: String,
        methodName: String,
        sinkMatch: SubVulnerabilityDefinition
    ) {
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
        val line = document?.getLineNumber(element.textRange.startOffset)?.plus(1) ?: -1

        synchronized(issues) {
            issues.add(
                ProjectIssue(
                    virtualFile,
                    line,
                    className,
                    methodName,
                    sinkMatch.subType.parent.name,
                    sinkMatch.subType.name,
                    SinkCallMode.SINGLE_SINK
                )
            )
        }
    }

    fun SinkList.getSQLiDefinition(): SubVulnerabilityDefinition {
        return ALL_SUB_VUL_DEFINITIONS.first { it.subType == SubVulnerabilityType.MYBATIS_XML_SQLI }
    }
}