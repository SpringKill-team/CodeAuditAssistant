package org.skgroup.CodeAuditAssistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.skgroup.CodeAuditAssistant.ui.component.ConfigDialog

/**
 * 类描述：OpenConfigAction 类用于。
 *
 * @author springkill
 * @version 1.0
 */
class OpenConfigAction : AnAction("Open Code Audit Assistant Config") {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = ConfigDialog()
        dialog.show()
    }
}
