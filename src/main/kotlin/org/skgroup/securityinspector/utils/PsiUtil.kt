package org.skgroup.securityinspector.utils

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.searches.ClassInheritorsSearch

object PsiUtil {
    fun findConcreteImplementations(abstractMethod: PsiMethod): List<PsiMethod> {
        val implementations = mutableListOf<PsiMethod>()
        val containingClass = abstractMethod.containingClass ?: return emptyList()

        // 查找所有子类或实现类
        val inheritors = if (containingClass.isInterface) {
            ClassInheritorsSearch.search(containingClass, abstractMethod.useScope, true).toList()
        } else {
            ClassInheritorsSearch.search(containingClass).toList()
        }

        inheritors.forEach { implClass ->
            implClass.findMethodBySignature(abstractMethod, true)?.let {
                if (!it.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    implementations.add(it)
                }
            }
        }

        return implementations
    }
}