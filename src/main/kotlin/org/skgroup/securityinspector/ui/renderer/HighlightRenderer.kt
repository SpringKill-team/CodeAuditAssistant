package org.skgroup.securityinspector.ui.renderer

import com.intellij.ui.JBColor
import org.skgroup.securityinspector.enums.SinkCallMode
import org.skgroup.securityinspector.enums.SinkCallMode.*
import java.awt.Color
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * 类描述：HighlightRenderer 类用于创建一个渲染器，用于高亮显示。
 *
 * @author springkill
 * @version 1.0
 */
class HighlightRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        // 获取当前行的数据
        val model = table?.model as? DefaultTableModel
        val rowData = model?.getValueAt(row, column) as? SinkCallMode

        when (rowData) {
            SINGLE_SINK -> {
                background = JBColor(Color(204, 255, 204), Color(102, 153, 102))
                foreground = JBColor.BLACK
            }
            REACH_SOURCE -> {
                background = JBColor(Color(255, 204, 204), Color(232, 108, 108))
                foreground = JBColor.BLACK
            }
            HAS_CALL -> {
                background = JBColor(Color(225, 229, 160), Color(240, 255, 96))
                foreground = JBColor.BLACK
            }
            null -> {
                background = table?.background ?: JBColor.WHITE
                foreground = table?.foreground ?: JBColor.BLACK
            }
        }

        return component
    }
}
