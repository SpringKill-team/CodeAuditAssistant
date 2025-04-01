package org.skgroup.CodeAuditAssistant.ui.renderer

import com.intellij.openapi.vfs.VirtualFile
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/**
 * 类描述：FirstColumnRenderer 类用于创建一个渲染器，渲染sinkfinder第一列的内容。
 *
 * @author springkill
 * @version 1.0
 */
class FirstColumnRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        if (column == 0) {
            val file = value as? VirtualFile
            if (file != null) {
                text = file.name
            }
        }

        return component
    }
}
