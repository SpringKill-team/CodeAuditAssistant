package org.skgroup.securityinspector.sinkrules.xxe

import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ExpressionUtils
import com.siyeh.ig.psiutils.MethodCallUtils
import org.skgroup.securityinspector.enums.VulnElemType
import org.skgroup.securityinspector.enums.XmlFactory
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils
import org.skgroup.securityinspector.visitors.BaseFixElementWalkingVisitor

class XXEInspector : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.XXE")
        private val QUICK_FIX_NAME = InspectionBundle.message("vuln.fix.XXE")

        private val XXE_METHOD_SINKS = mapOf(
            "javax.xml.parsers.DocumentBuilderFactory" to listOf("newInstance"),
            "javax.xml.parsers.SAXParserFactory" to listOf("newInstance"),
            "javax.xml.transform.sax.SAXTransformerFactory" to listOf("newInstance"),
            "org.xml.sax.helpers.XMLReaderFactory" to listOf("createXMLReader"),
            "javax.xml.validation.SchemaFactory" to listOf("newInstance"),
            "javax.xml.stream.XMLInputFactory" to listOf("newFactory"),
            "javax.xml.transform.TransformerFactory" to listOf("newInstance"),
            "javax.xml.validation.Schema" to listOf("newValidator"),
            "org.apache.commons.digester3.Digester" to listOf("parse")
        )

        private val XXE_NEWEXPRESSION_SINKS = mapOf(
            "org.jdom.input.SAXBuilder" to "setFeature",
            "org.dom4j.io.SAXReader" to "setFeature"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                XXE_METHOD_SINKS.forEach { (className, methods) ->
                    methods.forEach { methodName ->
                        if (SecExpressionUtils.hasFullQualifiedName(expression, className, methodName)) {
                            commonExpressionCheck(expression, getXmlFactory(className), holder)
                        }
                    }
                }
            }

            override fun visitNewExpression(expression: PsiNewExpression) {
                XXE_NEWEXPRESSION_SINKS.forEach { (className, methodName) ->
                    if (SecExpressionUtils.hasFullQualifiedName(expression, className)) {
                        commonExpressionCheck(expression, getXmlFactory(className), holder, methodName)
                    }
                }
            }

            private fun commonExpressionCheck(
                expression: PsiCallExpression,
                xmlFactory: XmlFactory?,
                holder: ProblemsHolder,
                methodName: String = "setFeature"
            ) {
                xmlFactory?.let {
                    when (val parent = expression.parent) {
                        is PsiAssignmentExpression -> assignmentExpressionCheck(holder, expression, methodName, it)
                        is PsiLocalVariable -> localVariableCheck(holder, expression, methodName, it)
                        is PsiField -> classFieldCheck(holder, expression, methodName, it)
                        is PsiTypeCastExpression -> {
                            when (val grandParent = parent.parent) {
                                is PsiAssignmentExpression -> assignmentExpressionCheck(holder, expression, methodName, it)
                                is PsiLocalVariable -> localVariableCheck(holder, expression, methodName, it)
                                is PsiField -> classFieldCheck(holder, expression, methodName, it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getXmlFactory(className: String): XmlFactory? {
        return when (className) {
            "javax.xml.parsers.DocumentBuilderFactory" -> XmlFactory.DOCUMENT_BUILDER_FACTORY
            "javax.xml.parsers.SAXParserFactory" -> XmlFactory.SAX_PARSER_FACTORY
            "org.jdom.input.SAXBuilder" -> XmlFactory.SAX_BUILDER
            "org.dom4j.io.SAXReader" -> XmlFactory.SAX_READER
            "org.xml.sax.helpers.XMLReaderFactory" -> XmlFactory.XML_READER_FACTORY
            "javax.xml.transform.sax.SAXTransformerFactory" -> XmlFactory.SAX_TRANSFORMER_FACTORY
            "javax.xml.validation.SchemaFactory" -> XmlFactory.SCHEMA_FACTORY
            "javax.xml.stream.XMLInputFactory" -> XmlFactory.XML_INPUT_FACTORY
            "javax.xml.transform.TransformerFactory" -> XmlFactory.TRANSFORMER_FACTORY
            "javax.xml.validation.Schema" -> XmlFactory.VALIDATOR_OF_SCHEMA
            else -> null
        }
    }

    private fun assignmentExpressionCheck(holder: ProblemsHolder, expression: PsiCallExpression, shouldUsedMethodName: String, xmlFactory: XmlFactory) {
        var parent = expression.parent
        if (parent is PsiTypeCastExpression) {
            parent = parent.parent
        }

        val assignmentExpression = parent as PsiAssignmentExpression
        val resolvedElem = (assignmentExpression.lExpression as PsiReferenceExpression).resolve()
        val visitor = resolvedElem?.let { DisableEntityElementVisitor(shouldUsedMethodName, xmlFactory, it) }

        if (visitor?.let { checkVariableUseFix(assignmentExpression, resolvedElem, it) } == true) {
            return
        }

        holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, XxeInspectionQuickFix(xmlFactory, VulnElemType.ASSIGNMENT_EXPRESSION))
    }

    private fun localVariableCheck(holder: ProblemsHolder, expression: PsiCallExpression, shouldUsedMethodName: String, xmlFactory: XmlFactory) {
        var parent = expression.parent
        if (parent is PsiTypeCastExpression) {
            parent = parent.parent
        }

        val localVariable = parent as PsiLocalVariable
        val visitor = DisableEntityElementVisitor(shouldUsedMethodName, xmlFactory, localVariable)
        if (checkVariableUseFix(localVariable, null, visitor)) {
            return
        }

        holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, XxeInspectionQuickFix(xmlFactory, VulnElemType.LOCAL_VARIABLE))
    }

    private fun classFieldCheck(holder: ProblemsHolder, expression: PsiCallExpression, shouldUsedMethodName: String, xmlFactory: XmlFactory) {
        var parent = expression.parent
        if (parent is PsiTypeCastExpression) {
            parent = parent.parent
        }

        val field = parent as PsiField
        val visitor = DisableEntityElementVisitor(shouldUsedMethodName, xmlFactory, field)
        if (checkVariableUseFix(null, field, visitor)) {
            return
        }

        holder.registerProblem(expression, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, XxeInspectionQuickFix(xmlFactory, VulnElemType.CLASS_FIELD))
    }

    private class DisableEntityElementVisitor(
        private val shouldUsedMethodName: String,
        private val xmlFactory: XmlFactory,
        private val refVar: PsiElement
    ) : BaseFixElementWalkingVisitor() {

        private val needSatisfiedRules: MutableMap<String, Boolean> = when (xmlFactory) {
            XmlFactory.DOCUMENT_BUILDER_FACTORY,
            XmlFactory.SAX_PARSER_FACTORY,
            XmlFactory.SAX_BUILDER,
            XmlFactory.SAX_READER,
            XmlFactory.XML_READER_FACTORY -> mutableMapOf("http://apache.org/xml/features/disallow-doctype-decl" to false)
            XmlFactory.SAX_TRANSFORMER_FACTORY,
            XmlFactory.SCHEMA_FACTORY,
            XmlFactory.XML_INPUT_FACTORY,
            XmlFactory.TRANSFORMER_FACTORY,
            XmlFactory.VALIDATOR_OF_SCHEMA -> mutableMapOf(
                "XMLConstants.ACCESS_EXTERNAL_DTD" to false,
                "XMLConstants.ACCESS_EXTERNAL_STYLESHEET" to false
            )
        }

        override fun visitElement(element: PsiElement) {
            if (element is PsiMethodCallExpression) {
                val methodCallExpression = element
                if (shouldUsedMethodName == MethodCallUtils.getMethodName(methodCallExpression)) {
                    val args = methodCallExpression.argumentList
                    if (args.expressionCount != 2) return

                    when (xmlFactory) {
                        XmlFactory.DOCUMENT_BUILDER_FACTORY,
                        XmlFactory.SAX_PARSER_FACTORY,
                        XmlFactory.SAX_BUILDER,
                        XmlFactory.SAX_READER,
                        XmlFactory.XML_READER_FACTORY -> {
                            if (ExpressionUtils.isLiteral(args.expressions[0], "http://apache.org/xml/features/disallow-doctype-decl") &&
                                ExpressionUtils.isLiteral(args.expressions[1], true)
                            ) {
                                needSatisfiedRules["http://apache.org/xml/features/disallow-doctype-decl"] = true
                            } else {
                                return
                            }
                        }
                        XmlFactory.SAX_TRANSFORMER_FACTORY,
                        XmlFactory.SCHEMA_FACTORY,
                        XmlFactory.XML_INPUT_FACTORY,
                        XmlFactory.TRANSFORMER_FACTORY,
                        XmlFactory.VALIDATOR_OF_SCHEMA -> {
                            if (args.expressions[0] is PsiReferenceExpression) {
                                when {
                                    args.expressions[0].textMatches("XMLConstants.ACCESS_EXTERNAL_DTD") &&
                                            ExpressionUtils.isEmptyStringLiteral(args.expressions[1]) -> {
                                        needSatisfiedRules["XMLConstants.ACCESS_EXTERNAL_DTD"] = true
                                    }
                                    args.expressions[0].textMatches("XMLConstants.ACCESS_EXTERNAL_STYLESHEET") &&
                                            ExpressionUtils.isEmptyStringLiteral(args.expressions[1]) -> {
                                        needSatisfiedRules["XMLConstants.ACCESS_EXTERNAL_STYLESHEET"] = true
                                    }
                                    else -> return
                                }
                            } else {
                                return
                            }
                        }
                        else -> return
                    }

                    val refQualifier = methodCallExpression.methodExpression.qualifierExpression
                    if (refQualifier != null &&
                        refQualifier.reference != null &&
                        refVar.isEquivalentTo(refQualifier.reference!!.resolve()) &&
                        needSatisfiedRules.values.all { it }
                    ) {
                        setFix(true)
                        stopWalking()
                        return
                    }
                }
                return
            }
            super.visitElement(element)
        }
    }
}