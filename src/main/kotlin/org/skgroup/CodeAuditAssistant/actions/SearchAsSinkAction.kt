package org.skgroup.CodeAuditAssistant.actions

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
import org.skgroup.CodeAuditAssistant.ui.service.CallGraphSearcher
import org.skgroup.CodeAuditAssistant.ui.service.CodeAuditAssistantProjectService

/**
 * 类描述：SearchAsSinkAction 类用于增加一个右键菜单，直接显示调用链。
 *
 * @author springkill
 * @version 1.0
 */
class SearchAsSinkAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)

        if (psiElement is PsiMethod) {
            searchAsSink(project, psiElement)
        }
    }

    override fun update(e: AnActionEvent) {

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

    private fun searchAsSink(project: Project?, method: PsiMethod) {
        val service = project?.getService(CodeAuditAssistantProjectService::class.java)

        val mainToolWindow = service?.mainToolWindow ?: return
        val callGraphPanel = mainToolWindow.analysisPanel
        val uiComponents = callGraphPanel.uiComponents
        uiComponents.sinkField.text = method.name
        CallGraphSearcher.search(
            uiComponents.sourceField,
            uiComponents.sinkField,
//            uiComponents.infoArea,
            uiComponents.searchResultRootNode,
            uiComponents.searchResultTreeModel,
            project
        )
    }
}
