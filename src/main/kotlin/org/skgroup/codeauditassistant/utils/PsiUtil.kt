package org.skgroup.codeauditassistant.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch

object PsiUtil {

    // 用于缓存已经处理过的类，防止重复递归
    private val processedClasses = mutableSetOf<PsiClass>()

    /**
     * 查找抽象方法或接口方法的具体实现。
     *
     * @param abstractMethod 传入的抽象方法
     * @param scope 搜索范围，默认为项目范围
     * @return 返回所有具体实现方法的列表
     */
    fun findConcreteImplementations(
        abstractMethod: PsiMethod,
        scope: GlobalSearchScope = abstractMethod.project.let { GlobalSearchScope.projectScope(it) }
    ): List<PsiMethod> {
        val implementations = mutableSetOf<PsiMethod>()
        val containingClass = abstractMethod.containingClass ?: return emptyList()

        // 每次调用前清空缓存，确保独立性
        processedClasses.clear()
        val allInheritors = mutableSetOf<PsiClass>()
        collectAllInheritors(containingClass, scope, allInheritors)

        // 只对非抽象类查找具体实现
        allInheritors.forEach { inheritor ->
            PsiClassImplUtil.findMethodsByName(inheritor, abstractMethod.name, false).forEach { method ->
                if (!method.hasModifierProperty(PsiModifier.ABSTRACT) && method.hasAnnotation("java.lang.Override")) {
                    implementations.add(method)
                }
            }
//            inheritor.findMethodsByName(abstractMethod.name, false).forEach {
//                it.let { method ->
//                    if (!method.hasModifierProperty(PsiModifier.ABSTRACT) && method.hasAnnotation("java.lang.Override")) {
//                        implementations.add(method)
//                    }
//                }
//            }
        }

        return implementations.toList()
    }

    /**
     * 递归收集所有继承者和实现者
     *
     * 这里对于接口与抽象类采用深度搜索（checkDeep = true），从而能够正确处理接口实现接口、抽象类实现接口、
     * 抽象类继承父类又实现接口等多层嵌套情况。
     */
    private fun collectAllInheritors(
        psiClass: PsiClass,
        scope: GlobalSearchScope,
        inheritors: MutableSet<PsiClass>
    ) {
        inheritors.add(psiClass)
        processedClasses.add(psiClass)
//        if (!inheritors.add(psiClass)) return
//        if (!processedClasses.add(psiClass)) return

        // 仅对接口和抽象类进行向下搜索
        if (psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            // 使用深度搜索，确保查找到所有子类/实现类
            ClassInheritorsSearch.search(psiClass, scope, true).forEach { inheritor ->
                collectAllInheritors(inheritor, scope, inheritors)
            }
        }
    }

    fun clearCache() {
        processedClasses.clear()
    }
}
