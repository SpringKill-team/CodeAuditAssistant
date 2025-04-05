package org.skgroup.CodeAuditAssistant.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import org.skgroup.CodeAuditAssistant.ui.panel.CallGraphToolWindowPanel
import org.skgroup.CodeAuditAssistant.ui.panel.DecompilerToolWindowPanel
import org.skgroup.CodeAuditAssistant.ui.service.AuthStateService
import org.skgroup.CodeAuditAssistant.ui.service.CodeAuditAssistantProjectService
import java.awt.BorderLayout

class CodeAuditAssistantMainToolWindow(private val project: Project) {

    val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    val analysisPanel: CallGraphToolWindowPanel


    init {
        if (AuthStateService.instance.myState.isAuthenticated) {
            project.getService(CodeAuditAssistantProjectService::class.java).mainToolWindow = this

            val tabbedPane = JBTabbedPane()

            // 第一个标签页: 反编译
            val decompilerPanel = DecompilerToolWindowPanel(project)
            tabbedPane.addTab("Decompiler", decompilerPanel.mainPanel)

            // 第二个标签页: 代码分析
            analysisPanel = CallGraphToolWindowPanel(project)
            tabbedPane.addTab("Code Analysis", analysisPanel.mainPanel)

            mainPanel.add(tabbedPane, BorderLayout.CENTER)
        } else throw IllegalArgumentException("Not authenticated")
    }
}

