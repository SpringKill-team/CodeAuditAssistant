package com.skgroup.securityinspector.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.ObjectUtils
import com.siyeh.ig.psiutils.ExpressionUtils
import com.siyeh.ig.psiutils.MethodCallUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.*
import java.util.stream.Collectors

object SecExpressionUtils {

    private val SQLiCareTypeStr = mutableSetOf("java.lang.String", "java.lang.StringBuilder", "java.lang.StringBuffer")

    /**
     * 从field使用点溯源到定义点
     * @param expression PsiExpression
     * @return PsiField | null
     */
    @Nullable
    fun resolveField(@Nullable expression: PsiExpression?): PsiField? {
        var expression = PsiUtil.skipParenthesizedExprDown(expression)
        val referenceExpression = ObjectUtils.tryCast(expression, PsiReferenceExpression::class.java)
        return referenceExpression?.let { ObjectUtils.tryCast(it.resolve(), PsiField::class.java) }
    }

    /**
     * 获取文本节点的内容
     * @param expression PsiExpression
     * @return String | null
     */
    @Nullable
    fun getLiteralInnerText(@Nullable expression: PsiExpression?): String? {
        val literal = ExpressionUtils.getLiteral(expression)
        return literal?.value?.toString()
    }

    /**
     * 将表达式尝试转换为文本内容
     * (1) 文本节点解析
     * (2) 基础类型 / 枚举类型
     * (3) field 字段
     * @param expression PsiExpression
     * @return String
     */
    @Nullable
    fun getText(@Nullable expression: PsiExpression?): String? {
        return getText(expression, false)
    }

    /**
     * 将表达式尝试转换为文本内容
     * (1) 文本节点解析
     * (2) 基础类型 / 枚举类型
     * (3) field 字段
     * @param expression PsiExpression
     * @param force boolean             强制转换为表达式字面值
     * @return String
     */
    @Nullable
    fun getText(@Nullable expression: PsiExpression?, force: Boolean): String? {
        if (expression == null) return null

        var value = getLiteralInnerText(expression)

        if (value == null && (TypeConversionUtil.isPrimitiveAndNotNull(expression.type)
                    || PsiUtil.isConstantExpression(expression) && expression !is PsiPolyadicExpression)) {
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

    fun isText(@NotNull expression: PsiExpression): Boolean {
        return getText(expression) != null
    }

    /**
     * 向上查找直到出现PsiStatement
     * @param element PsiElement
     * @return PsiStatement | null
     */
    @Nullable
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

    /**
     * 向上查找直到出现PsiField
     * @param element PsiElement
     * @return PsiField | null
     */
    @Nullable
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

    /**
     * 向上查找到当前的Method
     * @param element PsiElement
     * @return PsiMethod | null
     */
    @Nullable
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

    /**
     * 向上查找直到出现PsiClass
     * @param element PsiElement
     * @return PsiClass | null
     */
    @Nullable
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

    /**
     * 向上查找到当前的ClassInitializer
     * @param element PsiElement
     * @return PsiClassInitializer | null
     */
    @Nullable
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

    /**
     * 生成一个"?"文本节点
     * @param project Project
     * @return PsiLiteralExpression
     */
    private fun getPlaceholder(project: Project): PsiLiteralExpression {
        return JavaPsiFacade.getElementFactory(project).createExpressionFromText("\"?\"", null) as PsiLiteralExpression
    }

    /**
     * 生成一个自定义文本节点
     * @param project Project
     * @return PsiLiteralExpression
     */
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
                hasFullQualifiedName(psiExpression, "org.apache.commons.lang3.StringUtils", "join")) {
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

    /**
     * 解构PsiPolyadicExpression
     * 对于拼接的每一部分
     * (1) 判断为常量则输出为常量
     * (2) 判断为 field 则输出为常量
     * (-) 原样输出
     * @param expression PsiPolyadicExpression
     * @return List<PsiExpression>
     */
    fun deconPolyadicExpression(expression: PsiPolyadicExpression): List<PsiExpression> {
        val expressions = mutableListOf<PsiExpression>()

        if (expression.operationTokenType == JavaTokenType.PLUS) {
            for (operand in expression.operands) {
                val localVariable = ExpressionUtils.resolveLocalVariable(operand)
                if (localVariable != null) {
                    val localVariableType = operand.type
                    val refOperand = ObjectUtils.tryCast(operand, PsiReferenceExpression::class.java)
                    if (localVariableType != null && refOperand != null &&
                        "java.lang.String" == localVariableType.canonicalText && isConstStringConcatToReference(refOperand)) {
                        expressions.add(getCustomLiteral(operand.text, expression.project))
                        continue
                    }
                    if (localVariableType != null && refOperand != null && (
                                "java.lang.StringBuilder" == localVariableType.canonicalText ||
                                        "java.lang.StringBuffer" == localVariableType.canonicalText) &&
                        isConstStringBuilderToReference(refOperand)) {
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

        // 1. 先检查拼接点
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
                // 对于拼接，需要检查是否为  a = a + b 的场景
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

        // 2. 再检查定义点
        val origin = ref.resolve()
        if (origin is PsiLocalVariable) {
            val initializer = origin.initializer
            if (initializer != null && !isText(initializer)) {
                return false
            }
        }

        return true
    }

    /**
     * 通过一个引用节点，获取当前方法内该节点对应变量的所有引用点
     * @param reference PsiReference
     * @param maxOffset int             用偏移代表行号
     * @return List<PsiReference>
     */
    @NotNull
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

    /**
     * get FQName of a PsiMethod
     * fqname construct with <class QualifiedName> <method return Type> <method name>(<param_type> <param_name>, ...)
     * @param method PsiMethod
     * @return String
     */
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
}
