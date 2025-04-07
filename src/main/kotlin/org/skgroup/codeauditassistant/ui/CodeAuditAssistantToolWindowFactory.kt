package org.skgroup.codeauditassistant.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.skgroup.codeauditassistant.i18n.CAMessage
import org.skgroup.codeauditassistant.ui.component.CallGraphUIComponents

class CodeAuditAssistantToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 实例化主面板
        val mainToolWindow = CodeAuditAssistantMainToolWindow(project)

        // 用 ContentFactory 创建一个 Content
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainToolWindow.mainPanel, CAMessage.message("toolwindow.codeauditassistant"), false)

        // 把 Content 添加到 ToolWindow
        toolWindow.contentManager.addContent(content)

        CallGraphUIComponents(project).addIssueProblemsTabToProblemsView()
    }
}
