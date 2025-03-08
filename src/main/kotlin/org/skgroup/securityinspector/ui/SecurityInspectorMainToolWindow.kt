package org.skgroup.securityinspector.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import org.skgroup.securityinspector.ui.panel.CallGraphToolWindowPanel
import org.skgroup.securityinspector.ui.panel.DecompilerToolWindowPanel
import org.skgroup.securityinspector.ui.service.SecurityInspectorProjectService
import java.awt.BorderLayout

class SecurityInspectorMainToolWindow(private val project: Project) {

    val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    val analysisPanel: CallGraphToolWindowPanel


    init {
        project.getService(SecurityInspectorProjectService::class.java).mainToolWindow = this

        val tabbedPane = JBTabbedPane()

        // 第一个标签页: 反编译
        val decompilerPanel = DecompilerToolWindowPanel(project)
        tabbedPane.addTab("Decompiler", decompilerPanel.mainPanel)

        // 第二个标签页: 代码分析
        analysisPanel = CallGraphToolWindowPanel(project)
        tabbedPane.addTab("Code Analysis", analysisPanel.mainPanel)

        mainPanel.add(tabbedPane, BorderLayout.CENTER)
    }
}

