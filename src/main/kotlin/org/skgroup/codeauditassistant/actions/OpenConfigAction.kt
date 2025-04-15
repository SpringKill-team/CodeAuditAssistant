package org.skgroup.codeauditassistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.skgroup.codeauditassistant.i18n.CAMessage
import org.skgroup.codeauditassistant.ui.component.ConfigDialog

/**
 * 类描述：OpenConfigAction 类用于。
 *
 * @author springkill
 * @version 1.0
 */
class OpenConfigAction : AnAction(CAMessage.message("action.openconfig")) {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = ConfigDialog()
        dialog.show()
    }
}
