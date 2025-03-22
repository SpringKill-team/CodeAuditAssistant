package org.skgroup.securityinspector.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.skgroup.securityinspector.ui.service.CallGraphGenerator
import org.skgroup.securityinspector.ui.service.SecurityInspectorProjectService

/**
 * 类描述：BuildCallGraphAction 类用于为单个方法构建调用图。
 *
 * @author springkill
 * @version 1.0
 */
class BuildCallGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)

        if (psiElement is PsiMethod) {
            buildCallGraphForMethod(project, psiElement)
        }
    }

    override fun update(e: AnActionEvent) {
//        val project = e.project
//        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)

        CoroutineScope(Dispatchers.Default).launch {
            val isMethod = readAction {
                val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
                psiElement is PsiMethod
            }

            ApplicationManager.getApplication().invokeLater {
                e.presentation.isEnabledAndVisible = isMethod
            }
        }
    }

    private fun buildCallGraphForMethod(project: Project?, method: PsiMethod) {
        val service = project?.getService(SecurityInspectorProjectService::class.java)

        val mainToolWindow = service?.mainToolWindow ?: return
        val callGraphPanel = mainToolWindow.analysisPanel
        val uiComponents = callGraphPanel.uiComponents

        CallGraphGenerator.generate(
            project,
            method,
            uiComponents.progressBar,
            uiComponents,
            uiComponents.rootListModel,
        )

    }
}
