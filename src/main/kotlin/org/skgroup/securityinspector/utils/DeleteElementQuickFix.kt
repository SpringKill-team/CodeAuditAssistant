package org.skgroup.securityinspector.utils

import com.intellij.codeInsight.daemon.impl.quickfix.DeleteElementFix
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class DeleteElementQuickFix(
    @NotNull element: PsiElement,
    @NotNull @Nls text: String
) : DeleteElementFix(element, text) {

    override fun invoke(
        @NotNull project: Project,
        @NotNull file: PsiFile,
        @Nullable editor: Editor?,
        @NotNull startElement: PsiElement,
        @NotNull endElement: PsiElement
    ) {
        super.invoke(project, file, editor, startElement, endElement)
    }
}