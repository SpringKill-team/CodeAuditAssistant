package org.skgroup.securityinspector.inspectors

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.psi.*
import org.apache.commons.codec.digest.MurmurHash3
import org.skgroup.securityinspector.utils.SecExpressionUtils
import org.skgroup.securityinspector.visitors.BaseFixElementWalkingVisitor

abstract class BaseLocalInspectionTool : AbstractBaseJavaLocalInspectionTool() {

    protected fun checkVariableUseFix(
        assignElem: PsiElement?,
        resolvedElem: PsiElement?,
        visitor: BaseFixElementWalkingVisitor
    ): Boolean {
        val method: PsiMethod? = SecExpressionUtils.getParentOfMethod(assignElem)
        if (method != null) {
            method.accept(visitor)
            return visitor.isFix()
        }

        val initializer: PsiClassInitializer? = SecExpressionUtils.getParentOfClassInitializer(assignElem)
        if (initializer != null) {
            initializer.accept(visitor)
            return visitor.isFix()
        }

        if (resolvedElem is PsiField) {
            val field: PsiField = resolvedElem
            return if (field.hasModifierProperty(PsiModifier.STATIC)) {
                checkStaticInitializersHasFix(field.parent as PsiClass, visitor)
            } else {
                checkConstructorHasFix(field.parent as PsiClass, visitor)
            }
        }

        return false
    }

    private fun checkConstructorHasFix(aClass: PsiClass, visitor: BaseFixElementWalkingVisitor): Boolean {
        val initializers: Array<PsiClassInitializer> = aClass.initializers
        for (initializer in initializers) {
            if (!initializer.hasModifierProperty(PsiModifier.STATIC)) {
                initializer.accept(visitor)
                if (visitor.isFix()) {
                    return true
                }
            }
        }

        val constructors: Array<PsiMethod> = aClass.constructors
        for (constructor in constructors) {
            constructor.accept(visitor)
            if (visitor.isFix()) {
                return true
            }
        }
        return false
    }

    private fun checkStaticInitializersHasFix(aClass: PsiClass, visitor: BaseFixElementWalkingVisitor): Boolean {
        val initializers: Array<PsiClassInitializer> = aClass.initializers
        for (initializer in initializers) {
            if (initializer.hasModifierProperty(PsiModifier.STATIC)) {
                initializer.accept(visitor)
                if (visitor.isFix()) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        fun getVulnSign(element: PsiElement): Int {
            return getVulnSign(
                SecExpressionUtils.getElementFQName(element),
                element.text
            )
        }

        fun getVulnSign(fqname: String, elementText: String): Int {
            return MurmurHash3.hash32(String.format("%s|%s", fqname, elementText))
        }
    }
}