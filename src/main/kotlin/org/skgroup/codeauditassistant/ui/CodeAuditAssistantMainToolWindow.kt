package org.skgroup.codeauditassistant.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import org.skgroup.codeauditassistant.i18n.CAMessage
import org.skgroup.codeauditassistant.ui.panel.CallGraphToolWindowPanel
import org.skgroup.codeauditassistant.ui.panel.DecompilerToolWindowPanel
import org.skgroup.codeauditassistant.ui.service.CodeAuditAssistantProjectService
import java.awt.BorderLayout

class CodeAuditAssistantMainToolWindow(private val project: Project) {

    val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    val analysisPanel: CallGraphToolWindowPanel


    init {
        project.getService(CodeAuditAssistantProjectService::class.java).mainToolWindow = this

        val tabbedPane = JBTabbedPane()

        // 第一个标签页: 反编译
        val decompilerPanel = DecompilerToolWindowPanel(project)
        tabbedPane.addTab(CAMessage.message("tab.decompiler"), decompilerPanel.mainPanel)

        // 第二个标签页: 代码分析
        analysisPanel = CallGraphToolWindowPanel(project)
        tabbedPane.addTab(CAMessage.message("tab.code.analysis"), analysisPanel.mainPanel)

        mainPanel.add(tabbedPane, BorderLayout.CENTER)
    }
}
