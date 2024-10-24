package org.skgroup.securityinspector.inspectors

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

interface InspectionTool {
    /**
     * 针对给定的 PsiFile 运行检查器
     * @param psiFile 目标 PsiFile
     * @param problemsHolder 用于收集问题
     */
    fun inspectFile(psiFile: PsiFile, problemsHolder: ProblemsHolder)
}