package org.skgroup.securityinspector.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.ObjectUtils
import com.siyeh.ig.psiutils.ExpressionUtils
import com.siyeh.ig.psiutils.MethodCallUtils

object SecExpressionUtils {

    private val SQLiCareTypeStr = mutableSetOf("java.lang.String", "java.lang.StringBuilder", "java.lang.StringBuffer")

    fun resolveField(expression: PsiExpression?): PsiField? {
        var expression = PsiUtil.skipParenthesizedExprDown(expression)
        val referenceExpression = ObjectUtils.tryCast(expression, PsiReferenceExpression::class.java)
        return referenceExpression?.let { ObjectUtils.tryCast(it.resolve(), PsiField::class.java) }
    }

    fun getLiteralInnerText(expression: PsiExpression?): String? {
        val literal = ExpressionUtils.getLiteral(expression)
        return literal?.value?.toString()
    }

    fun getText(expression: PsiExpression?): String? {
        return getText(expression, false)
    }

    fun getText(expression: PsiExpression?, force: Boolean): String? {
        if (expression == null) return null

        var value = getLiteralInnerText(expression)

        if (value == null && (TypeConversionUtil.isPrimitiveAndNotNull(expression.type)
                    || PsiUtil.isConstantExpression(expression) && expression !is PsiPolyadicExpression)
        ) {
            value = expression.text
        }

        if (value == null && expression is PsiReferenceExpression) {
            val resolve = expression.resolve()
            if (resolve is PsiField) {
                val initializer = resolve.initializer
                value = initializer?.let { getText(it, false) } ?: resolve.name
            }
        }

        if (value == null && expression is PsiPolyadicExpression) {
            val sb = StringBuilder()
            expression.operands.forEach { operand ->
                val text = getText(operand, force)
                if (text == null) {
                    sb.setLength(0)
                    return@forEach
                }
                sb.append(text)
            }
            if (sb.isNotEmpty()) {
                value = sb.toString()
            }
        }

        if (force && value == null) {
            value = expression.text
        }
        return value
    }

    fun isText(expression: PsiExpression): Boolean {
        return getText(expression) != null
    }

    fun getParentOfStatement(element: PsiElement?): PsiStatement? {
        var element = element
        while (element !is PsiStatement) {
            if (element == null || element is PsiMethod) {
                return null
            }
            element = element.parent
        }
        return element
    }

    fun getParentOfField(element: PsiElement?): PsiField? {
        var element = element
        while (element !is PsiField) {
            if (element == null || element is PsiClass) {
                return null
            }
            element = element.parent
        }
        return element
    }

    fun getParentOfMethod(element: PsiElement?): PsiMethod? {
        var element = element
        while (element !is PsiMethod) {
            if (element == null) {
                return null
            }
            element = element.parent
        }
        return element
    }

    fun getParentOfClass(element: PsiElement?): PsiClass? {
        var element = element
        while (element !is PsiClass) {
            if (element == null) {
                return null
            }
            element = element.parent
        }
        return element
    }

    fun getParentOfClassInitializer(element: PsiElement?): PsiClassInitializer? {
        var element = element
        while (element !is PsiClassInitializer) {
            if (element == null) {
                return null
            }
            element = element.parent
        }
        return element
    }

    private fun getPlaceholder(project: Project): PsiLiteralExpression {
        return JavaPsiFacade.getElementFactory(project).createExpressionFromText("\"?\"", null) as PsiLiteralExpression
    }

    private fun getCustomLiteral(s: String, project: Project): PsiLiteralExpression {
        return JavaPsiFacade.getElementFactory(project)
            .createExpressionFromText("\"" + s.replace("\"", "\\\"") + "\"", null) as PsiLiteralExpression
    }

    fun isSqliCareExpression(psiExpression: PsiExpression): Boolean {
        val type = psiExpression.type
        if (type != null && !SQLiCareTypeStr.contains(type.canonicalText)) {
            return false
        }

        if (psiExpression is PsiMethodCallExpression) {
            if (hasFullQualifiedName(psiExpression, "java.lang.String", "join")) {
                val args = psiExpression.argumentList.expressions
                return args.size == 2 && args[1].type != null && args[1].type!!.presentableText.contains("<String>")
            } else if (hasFullQualifiedName(psiExpression, "org.apache.commons.lang.StringUtils", "join") ||
                hasFullQualifiedName(psiExpression, "org.apache.commons.lang3.StringUtils", "join")
            ) {
                val args = psiExpression.argumentList.expressions
                return args.size >= 1 && args[0].type != null && args[0].type!!.presentableText.contains("<String>")
            } else {
                val qualifierExp = psiExpression.methodExpression.qualifierExpression
                if (qualifierExp != null && qualifierExp.reference != null) {
                    val targetElem = qualifierExp.reference!!.resolve()
                    return targetElem !is PsiClass || !targetElem.isEnum
                }
            }
        }
        return true
    }

    fun deconPolyadicExpression(expression: PsiPolyadicExpression): List<PsiExpression> {
        val expressions = mutableListOf<PsiExpression>()

        if (expression.operationTokenType == JavaTokenType.PLUS) {
            for (operand in expression.operands) {
                val localVariable = ExpressionUtils.resolveLocalVariable(operand)
                if (localVariable != null) {
                    val localVariableType = operand.type
                    val refOperand = ObjectUtils.tryCast(operand, PsiReferenceExpression::class.java)
                    if (localVariableType != null && refOperand != null &&
                        "java.lang.String" == localVariableType.canonicalText && isConstStringConcatToReference(
                            refOperand
                        )
                    ) {
                        expressions.add(getCustomLiteral(operand.text, expression.project))
                        continue
                    }
                    if (localVariableType != null && refOperand != null && (
                                "java.lang.StringBuilder" == localVariableType.canonicalText ||
                                        "java.lang.StringBuffer" == localVariableType.canonicalText) &&
                        isConstStringBuilderToReference(refOperand)
                    ) {
                        expressions.add(getCustomLiteral(operand.text, expression.project))
                        continue
                    }

                    expressions.add(operand)
                    continue
                }

                val field = resolveField(operand)
                if (field != null) {
                    val fieldInitializer = field.initializer
                    if (fieldInitializer is PsiPolyadicExpression) {
                        expressions.addAll(deconPolyadicExpression(fieldInitializer))
                    } else if (fieldInitializer != null && isText(fieldInitializer)) {
                        expressions.add(fieldInitializer)
                    } else {
                        expressions.add(getCustomLiteral(operand.text, expression.project))
                    }
                    continue
                }

                expressions.add(operand)
            }
        }

        return expressions
    }

    private fun isConstStringBuilderToReference(ref: PsiReferenceExpression): Boolean {
        val refPoints = getReferenceOnMethodScope(ref, ref.textOffset - 1)

        for (refPoint in refPoints) {
            val refPointExp = ObjectUtils.tryCast(refPoint, PsiReferenceExpression::class.java) ?: continue
            val refParentExp = ObjectUtils.tryCast(refPointExp.parent, PsiReferenceExpression::class.java) ?: continue
            if ("append" == refParentExp.referenceName || "insert" == refParentExp.referenceName) {
                val methodCall = ObjectUtils.tryCast(refParentExp.parent, PsiMethodCallExpression::class.java)
                methodCall?.let {
                    val args = it.argumentList.expressions
                    val isConst = when {
                        "append" == refParentExp.referenceName && args.size == 1 -> isText(args[0])
                        "insert" == refParentExp.referenceName && args.size >= 2 -> isText(args[1])
                        else -> true
                    }
                    if (!isConst) return false
                }
            }
        }

        return true
    }

    private fun isConstStringConcatToReference(ref: PsiReferenceExpression): Boolean {
        val refPoints = getReferenceOnMethodScope(ref, ref.textOffset - 1)

        for (refPoint in refPoints) {
            val refPointExp = ObjectUtils.tryCast(refPoint, PsiReferenceExpression::class.java) ?: continue

            val assignExp = ObjectUtils.tryCast(refPointExp.parent, PsiAssignmentExpression::class.java)
            if (assignExp == null || !refPoint.equals(assignExp.lExpression)) {
                continue
            }

            val lExp = ObjectUtils.tryCast(assignExp.lExpression, PsiReferenceExpression::class.java) ?: continue
            val varName = lExp.referenceName ?: continue

            val rExp = assignExp.rExpression ?: continue

            if (rExp is PsiReferenceExpression && varName == rExp.referenceName) {
                continue
            }

            if (rExp is PsiPolyadicExpression) {
                for (operand in rExp.operands) {
                    if (operand is PsiReferenceExpression && varName == operand.referenceName) {
                        continue
                    }
                    if (!isText(operand)) {
                        return false
                    }
                }
            } else if (!isText(rExp)) {
                return false
            }
        }

        val origin = ref.resolve()
        if (origin is PsiLocalVariable) {
            val initializer = origin.initializer
            if (initializer != null && !isText(initializer)) {
                return false
            }
        }

        return true
    }

    private fun getReferenceOnMethodScope(reference: PsiReference, maxOffset: Int): List<PsiReference> {
        var refResults: List<PsiReference> = emptyList()
        val element = reference.resolve() ?: return refResults

        val method = getParentOfMethod(element) ?: return refResults

        refResults = ReferencesSearch.search(element, LocalSearchScope(arrayOf(method), null, true)).findAll().toList()
        if (maxOffset != -1) {
            refResults = refResults.filter { it.element.textOffset <= maxOffset }
        }
        return refResults
    }

    fun hasFullQualifiedName(methodCall: PsiMethodCallExpression, qualifiedName: String, methodName: String): Boolean {
        val methodCallName = MethodCallUtils.getMethodName(methodCall)
        if (methodName != methodCallName) {
            return false
        }

        val method = methodCall.resolveMethod() ?: return false

        val containingClass = method.containingClass ?: return false

        return qualifiedName == containingClass.qualifiedName
    }

    fun hasFullQualifiedName(newExpression: PsiNewExpression, qualifiedName: String): Boolean {
        return newExpression.classReference?.qualifiedName == qualifiedName
    }

    fun getMethodFQName(method: PsiMethod): String {
        val fqname = StringBuilder()
        val aClass = method.containingClass
        fqname.append(aClass?.qualifiedName ?: "null")
        fqname.append(" ")

        val methodReturnType = method.returnType
        fqname.append(methodReturnType?.canonicalText ?: "null")
        fqname.append(" ")

        fqname.append(method.name)
        fqname.append("(")

        val parameterList = method.parameterList
        val parameters = parameterList.parameters
        for (parameter in parameters) {
            fqname.append(parameter.type.canonicalText)
            fqname.append(" ")
            fqname.append(parameter.name)
            fqname.append(", ")
        }

        if (parameters.isNotEmpty()) {
            fqname.delete(fqname.length - 2, fqname.length)
        }

        fqname.append(")")
        return fqname.toString()
    }

    fun getElementFQName(element: PsiElement): String {
        val method = getParentOfMethod(element)
        return if (method != null) {
            getMethodFQName(method)
        } else {
            val aClass = getParentOfClass(element)
            aClass?.qualifiedName ?: "null"
        }
    }

    fun isMethodSink(expression: PsiMethodCallExpression, methodSinks: Map<String, List<String>>): Boolean {
        return methodSinks.any { (className, methodNames) ->
            methodNames.any { methodName ->
                hasFullQualifiedName(expression, className, methodName)
            }
        }
    }

    fun isNewExpressionSink(expression: PsiNewExpression, newExpressionSinks: List<String>): Boolean {
        return newExpressionSinks.any { className ->
            hasFullQualifiedName(expression, className)
        }
    }

    fun matchesClassName(psiClass: PsiClass, pattern: String): Boolean {
        return psiClass.name?.contains(pattern, true) == true
    }

    fun matchesMethodName(psiMethod: PsiMethod, pattern: String): Boolean {
        return psiMethod.name.contains(pattern, true)
    }

}
