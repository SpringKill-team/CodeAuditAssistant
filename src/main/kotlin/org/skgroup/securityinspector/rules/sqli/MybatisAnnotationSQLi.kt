package org.skgroup.securityinspector.rules.sqli

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.*

class MybatisAnnotationSQLi : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.MybatisAnnotationSQLi")
        private val QUICK_FIX_NAME = InspectionBundle.message("vuln.fix.MybatisAnnotationSQLi")

        private val checkedAnnotations = mapOf(
            "org.apache.ibatis.annotations.Select" to listOf("value"),
            "org.apache.ibatis.annotations.Delete" to listOf("value"),
            "org.apache.ibatis.annotations.Update" to listOf("value"),
            "org.apache.ibatis.annotations.Insert" to listOf("value")
        )
    }

    private val mybatisAnnotationSQLiQuickFix = MybatisAnnotationSQLiQuickFix()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                val qualifiedName = annotation.qualifiedName ?: return
                val methods = checkedAnnotations[qualifiedName] ?: return

                val psiAnnotationParameterList = annotation.parameterList
                val psiNameValuePairs = psiAnnotationParameterList.attributes

                if (psiNameValuePairs.size != 1) return

                val psiNameValuePair = psiNameValuePairs[0]
                var content: String? = psiNameValuePair.literalValue

                if (content == null) {
                    val innerElem = psiNameValuePair.value ?: return
                    content = when (innerElem) {
                        is PsiPolyadicExpression -> SecExpressionUtils.getText(innerElem, true)
                        is PsiArrayInitializerMemberValue -> innerElem.initializers.joinToString("") {
                            SecExpressionUtils.getText(it as PsiExpression, true) ?: ""
                        }

                        else -> null
                    }
                }

                psiNameValuePair.value?.let { valueElement ->
                    if (!content.isNullOrEmpty() && hasSQLi(content)) {
                        holder.registerProblem(
                            valueElement,
                            MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            mybatisAnnotationSQLiQuickFix
                        )
                    }
                }

            }
        }
    }

    private fun hasSQLi(content: String): Boolean {
        var modifiedContent = content
        if (modifiedContent.startsWith("<script>") && modifiedContent.endsWith("</script>")) {
            modifiedContent = modifiedContent.substring(8, modifiedContent.length - 9)
        }

        val matcher = SQLiUtil.dollarVarPattern.matcher(modifiedContent)
        val fragments = mutableListOf<List<String>>()
        var offset = 0
        var count = 0

        while (matcher.find(offset) && count++ < 9999) {
            val prefix = modifiedContent.substring(0, matcher.start())
            val varName = matcher.group(1)
            val suffix = modifiedContent.substring(matcher.end())

            if (ignorePosition(prefix, varName, suffix)) continue

            fragments.add(listOf(prefix, varName, suffix))
            offset = matcher.end()
        }

        return SQLiUtil.hasVulOnSQLJoinStrList(fragments)
    }

    private fun ignorePosition(prefix: String, varName: String, suffix: String): Boolean {
        return varName.startsWith("ew.")
    }

    class MybatisAnnotationSQLiQuickFix : LocalQuickFix {
        override fun getFamilyName(): String = QUICK_FIX_NAME

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            var problemElem = descriptor.psiElement
            var isFix = false

            if (problemElem is PsiLiteralExpression) {
                val content = SecExpressionUtils.getLiteralInnerText(problemElem)
                if (content != null) {
                    val newContent = replaceDollarWithHashtagOnString(content, 0)
                    val factory = JavaPsiFacade.getElementFactory(project)
                    problemElem = problemElem.replace(factory.createExpressionFromText("\"$newContent\"", problemElem))
                    isFix = !newContent.contains("$")
                }
            }

            if (!isFix) {
                val method = SecExpressionUtils.getParentOfMethod(problemElem) ?: return
                val factory = JavaPsiFacade.getElementFactory(project)
                    val comment = factory.createCommentFromText(Constants.SQL_INJECTION_HELP_COMMENT, null)
                method.addBefore(comment, method.firstChild)
            }
        }

        private fun replaceDollarWithHashtagOnString(content: String, offset: Int): String {
            val matcher = SQLiUtil.dollarVarPattern.matcher(content)
            var modifiedContent = content

            if (matcher.find(offset)) {
                var prefix = modifiedContent.substring(0, matcher.start())
                val suffix = modifiedContent.substring(matcher.end())
                val varName = matcher.group(1)
                val inner = matcher.group()

                if (!   ignorePosition(prefix, varName, suffix) && SQLiUtil.hasVulOnSQLJoinStr(prefix, varName, suffix)) {
                    val lowerPrefix = prefix.lowercase()
                    if (SQLiUtil.whereInEndPattern.matcher(lowerPrefix).find()) {
                        modifiedContent = replaceDollarWithHashtagOnString(modifiedContent, matcher.end())
                    } else if (SQLiUtil.likeEndPattern.matcher(lowerPrefix).find()) {
                        // 处理 like '%${var}' 或 like "%${var}" 情况
                        val concat = " CONCAT('%', ${inner.replace('$', '#')}, '%') "
                        prefix = prefix.trimEnd('\'', '"', '%', ' ')
                        val newSuffix = suffix.trimStart('\'', '"', '%', ' ')
                        modifiedContent = replaceDollarWithHashtagOnString(
                            prefix + concat + newSuffix,
                            prefix.length + concat.length - 1
                        )
                    } else {
                        if (prefix.endsWith("'") || prefix.endsWith("\"")) {
                            prefix = prefix.substring(0, prefix.length - 1)
                        }
                        val newSuffix = suffix.trimStart('\'', '"')
                        modifiedContent = replaceDollarWithHashtagOnString(
                            prefix + inner.replace('$', '#') + newSuffix,
                            prefix.length + inner.length
                        )
                    }
                }
            }

            return modifiedContent
        }

        private fun ignorePosition(prefix: String, varName: String, suffix: String): Boolean {
            return varName.startsWith("ew.")
        }
    }
}

