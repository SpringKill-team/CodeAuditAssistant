package org.skgroup.securityinspector.utils

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.ObjectUtils

class SetBoolArgQuickFix(
    private val quickFixName: String,
    private val newValue: Boolean,
    arg: PsiLiteralExpression? = null
) : LocalQuickFix {

    private var argPointer: SmartPsiElementPointer<PsiLiteralExpression>? = arg?.let {
        SmartPointerManager.createPointer(it)
    }

    override fun getFamilyName(): String {
        return quickFixName
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val targetArg = argPointer?.element ?: ObjectUtils.tryCast(descriptor.psiElement, PsiLiteralExpression::class.java)
            ?.let { SmartPointerManager.createPointer(it).element } ?: return

        val factory = JavaPsiFacade.getElementFactory(project)
        val newArg = factory.createExpressionFromText(newValue.toString(), null)
        targetArg.replace(newArg)
    }
}
