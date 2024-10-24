package org.skgroup.securityinspector.rules.rce

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.DeleteElementQuickFix
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils
import org.skgroup.securityinspector.visitors.BaseFixElementWalkingVisitor

class JacksonDatabindDefaultTyping : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.JacksonDatabindDefaultTyping")
        private val DEFAULT_TYPING_FIX_NAME = InspectionBundle.message("vuln.fix.JacksonDatabindDefaultTypingDefault")
        private val ANNOTATION_FIX_NAME = InspectionBundle.message("vuln.fix.JacksonDatabindDefaultTypingAnnotation")
        private val annotationQuickFix = AnnotationQuickFix()

        private val JACKSON_METHOD_SINKS = mapOf(
            "com.fasterxml.jackson.databind.ObjectMapper" to listOf("enableDefaultTyping"),
            "org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer" to listOf("setObjectMapper")
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                JACKSON_METHOD_SINKS["com.fasterxml.jackson.databind.ObjectMapper"]?.forEach { methodName ->
                    if (SecExpressionUtils.hasFullQualifiedName(expression, "com.fasterxml.jackson.databind.ObjectMapper", methodName)) {
                        val varExp = expression.methodExpression.qualifierExpression
                        val varElement = varExp?.reference?.resolve()
                        if (varElement != null) {
                            val visitor = UseToJackson2JsonRedisSerializerVisitor(varElement)
                            if (checkVariableUseFix(varElement, null, visitor)) {
                                return
                            }
                        }
                        holder.registerProblem(
                            expression,
                            MESSAGE,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            DeleteElementQuickFix(expression, DEFAULT_TYPING_FIX_NAME)
                        )
                    }
                }
            }

            override fun visitAnnotation(annotation: PsiAnnotation) {
                if ("com.fasterxml.jackson.annotation.JsonTypeInfo" == annotation.qualifiedName) {
                    val nameValuePairs = annotation.parameterList.attributes
                    nameValuePairs.forEach { nameValuePair ->
                        if ("use" == nameValuePair.attributeName &&
                            nameValuePair.value is JvmAnnotationEnumFieldValue
                        ) {
                            val annotationValue = nameValuePair.value as JvmAnnotationEnumFieldValue
                            if ("com.fasterxml.jackson.annotation.JsonTypeInfo.Id" == annotationValue.containingClassName &&
                                ("CLASS" == annotationValue.fieldName || "MINIMAL_CLASS" == annotationValue.fieldName)
                            ) {
                                holder.registerProblem(
                                    nameValuePair,
                                    MESSAGE,
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                    annotationQuickFix
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    class UseToJackson2JsonRedisSerializerVisitor(private val refVar: PsiElement) : BaseFixElementWalkingVisitor() {

        override fun visitElement(element: PsiElement) {
            if (element is PsiMethodCallExpression) {
                JACKSON_METHOD_SINKS["org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer"]?.forEach { methodName ->
                    if (SecExpressionUtils.hasFullQualifiedName(element, "org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer", methodName)) {
                        val args = element.argumentList.expressions
                        if (args.size == 1 && args[0] is PsiReferenceExpression) {
                            val refElem = args[0].reference
                            if (refElem != null && refVar.isEquivalentTo(refElem.resolve())) {
                                this.setFix(true)
                                this.stopWalking()
                                return
                            }
                        }
                    }
                }
            }
            super.visitElement(element)
        }
    }

    class AnnotationQuickFix : LocalQuickFix {

        override fun getFamilyName(): String {
            return ANNOTATION_FIX_NAME
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val nameValuePair = descriptor.psiElement as? PsiNameValuePair ?: return
            val member = nameValuePair.value
            if (member != null) {
                val splitText = member.text.split(".")
                if (splitText.size > 1 && (splitText.last() == "CLASS" || splitText.last() == "MINIMAL_CLASS")) {
                    val newText = splitText.dropLast(1) + "NAME"
                    val factory = JavaPsiFacade.getElementFactory(project)
                    member.replace(factory.createExpressionFromText(newText.joinToString("."), null))
                }
            }
        }
    }
}
