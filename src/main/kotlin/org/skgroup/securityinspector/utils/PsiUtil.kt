package org.skgroup.securityinspector.utils

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.searches.ClassInheritorsSearch

/**
 * PsiUtil 是一个工具类，提供了一些有用的 Psi 相关的方法
 *
 * @author springkill
 * @version 1.0
 */
object PsiUtil {

    /**
     * Find Concrete Implementations 用于查找抽象方法和接口的具体实现。
     *
     * @param abstractMethod 传入一个抽象方法
     * @return 返回一个列表，包含了所有的具体实现
     */
    fun findConcreteImplementations(abstractMethod: PsiMethod): List<PsiMethod> {
        val implementations = mutableListOf<PsiMethod>()
        val containingClass = abstractMethod.containingClass ?: return emptyList()

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