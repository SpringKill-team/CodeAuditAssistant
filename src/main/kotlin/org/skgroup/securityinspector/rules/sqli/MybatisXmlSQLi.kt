package org.skgroup.securityinspector.rules.sqli

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.ASTFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.xml.*
import com.intellij.xml.util.XmlUtil
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SQLiUtil
import java.util.regex.Matcher

class MybatisXmlSQLi : BaseLocalInspectionTool() {

    companion object {
        val MESSAGE = InspectionBundle.message("vuln.massage.MybatisXmlSQLi")
        val QUICK_FIX_NAME = InspectionBundle.message("vuln.fix.MybatisXmlSQLi")

        private val ignoreVarName = setOf(
            "orderByClause", "pageStart", "pageSize", "criterion.condition", "alias"
        )
    }

    private val mybatisXmlSQLiQuickFix = MybatisXmlSQLiQuickFix()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : XmlElementVisitor() {
            override fun visitXmlText(text: XmlText) {
                if (shouldIgnoreParentTag(text.parentTag)) return
                val document = XmlUtil.getContainingFile(text).document ?: return
                val dtd = XmlUtil.getDtdUri(document)
                if (!isMyBatisDtd(dtd)) return

                val textValue = text.value
                if (textValue.isEmpty() || !textValue.contains("\${")) return

                val matcher = SQLiUtil.dollarVarPattern.matcher(textValue)
                var offset = 0
                var count = 0

                while (matcher.find(offset) && count++ < 9999) {
                    val prefix = textValue.substring(0, matcher.start())
                    val variable = matcher.group(1)
                    val suffix = textValue.substring(matcher.end())
                    if (!ignorePosition(prefix, variable, suffix) && SQLiUtil.hasVulOnSQLJoinStr(prefix, variable, suffix)) {
                        holder.registerProblem(text, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, mybatisXmlSQLiQuickFix)
                        break
                    }
                    offset = matcher.end()
                }
            }
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

    class MybatisXmlSQLiQuickFix : LocalQuickFix {
        override fun getFamilyName(): String {
            return QUICK_FIX_NAME
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val xmlText = descriptor.psiElement as? XmlText ?: return
            fixXmlText(xmlText, 0)
        }

        private fun fixXmlText(xmlText: XmlText, offset: Int) {
            var v = xmlText.value
            val matcher = SQLiUtil.dollarVarPattern.matcher(v)
            var localOffset = offset
            var count = 0

            while (matcher.find(localOffset) && count++ < 9999) {
                var prefix = v.substring(0, matcher.start())
                var suffix = v.substring(matcher.end())
                val variable = matcher.group(1)

                if (ignorePosition(prefix, variable, suffix) || !SQLiUtil.hasVulOnSQLJoinStr(prefix, variable, suffix)) {
                    localOffset = matcher.end()
                    continue
                }

                when {
                    SQLiUtil.whereInEndPattern.matcher(prefix).find() -> handleWhereIn(xmlText, matcher, prefix, suffix, variable)
                    SQLiUtil.likeEndPattern.matcher(prefix).find() -> handleLikePattern(xmlText, prefix, suffix, variable)
                    else -> handleDefaultPattern(xmlText, matcher, prefix, suffix, variable)
                }
                break
            }
        }

        private fun handleWhereIn(xmlText: XmlText, matcher: Matcher, prefix: String, suffix: String, variable: String) {
            var trimmedPrefix = prefix.trimEnd()
            var trimmedSuffix = suffix.trimStart()

            if (trimmedPrefix.endsWith("(") && trimmedSuffix.startsWith(")")) {
                trimmedPrefix = trimmedPrefix.substring(0, trimmedPrefix.length - 1)
                trimmedSuffix = trimmedSuffix.substring(1)
            }

            val parent = xmlText.parentTag ?: return
            val parserFacade = PsiParserFacade.getInstance(xmlText.project)
            val lastWhiteSpace = xmlText.lastChild as? PsiWhiteSpace
                ?: parserFacade.createWhiteSpaceFromText("\n") as PsiWhiteSpace

            xmlText.setValue(trimmedPrefix + lastWhiteSpace.text)

            // 添加 <foreach>
            val foreach = createForeachXmlTag(variable, parent, lastWhiteSpace)
            parent.addAfter(foreach, xmlText)


            // 添加后缀
            val tagFromText = XmlElementFactory.getInstance(xmlText.project)
                .createTagFromText("<a>${lastWhiteSpace.text}$trimmedSuffix</a>")
            val textElements = tagFromText.value.textElements
            val suffixXmlText = textElements.getOrElse(0) {
                ASTFactory.composite(XmlElementType.XML_TEXT) as XmlText
            }
            parent.add(suffixXmlText)
            fixXmlText(suffixXmlText, 0)
        }

        private fun handleLikePattern(xmlText: XmlText, prefix: String, suffix: String, variable: String) {
            val concat = " CONCAT('%', #{$variable}, '%') "
            val trimmedPrefix = prefix.trimEnd('\'', '"', '%', ' ', '\n', '\r')
            val trimmedSuffix = suffix.trimStart('\'', '"', '%', ' ')
            xmlText.setValue(trimmedPrefix + concat + trimmedSuffix)
        }

        private fun handleDefaultPattern(xmlText: XmlText, matcher: Matcher, prefix: String, suffix: String, variable: String) {
            val trimmedPrefix = prefix.trimEnd('\'', '"')
            val trimmedSuffix = suffix.trimStart('\'', '"')
            xmlText.setValue("$trimmedPrefix#{$variable}$trimmedSuffix")
        }

        private fun createForeachXmlTag(varName: String, parent: XmlTag, whiteSpace: PsiWhiteSpace): XmlTag {
            return parent.createChildTag(
                "foreach",
                parent.namespace,
                "${whiteSpace.text}#{${varName}It   em}${whiteSpace.text}",
                false
            ).apply {
                setAttribute("collection", varName)
                setAttribute("item",    "${varName}Item")
                setAttribute("open", "(")
                setAttribute("separator", ",")
                setAttribute("close", ")")
            }
        }

        private fun ignorePosition(prefix: String, variable: String, suffix: String): Boolean {
            return ignoreVarName.contains(variable) || variable.startsWith("ew.")
        }
    }
}
