package org.skgroup.CodeAuditAssistant.ui.component

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.*

/**
 * 类描述：ModuleSelectorDialog 类用于选择module。
 *
 * @author springkill
 * @version 1.0
 */
class ModuleSelectorDialog(project: Project) : DialogWrapper(project) {
    private val moduleComboBox: ComboBox<Module>

    init {
        title = "Select Module"
        // 获取所有模块
        val modules = ModuleManager.getInstance(project).modules
        val moduleNames = modules.map { it }.toTypedArray()

        // 初始化下拉菜单
        moduleComboBox = ComboBox(moduleNames)
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(JBLabel("Select a module:"), BorderLayout.NORTH)
        panel.add(moduleComboBox, BorderLayout.CENTER)
        return panel
    }

    fun getSelectedModuleName(): Module {
        return moduleComboBox.selectedItem as Module
    }
}
