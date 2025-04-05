package org.skgroup.codeauditassistant.ui.renderer

import com.intellij.icons.AllIcons
import org.skgroup.codeauditassistant.analysis.ast.nodes.MethodNode
import java.awt.Component
import javax.swing.*

object MethodListRenderer {
    fun createMethodRenderer() = object : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            when (value) {
                is MethodNode -> {
                    comp.icon = AllIcons.Nodes.Method
                    comp.text = value.className + "#" + value.name
                }
                is List<*> -> renderPath(comp, value)
            }
            return comp
        }

        private fun renderPath(comp: JLabel, value: List<*>) {
            val path = value.filterIsInstance<MethodNode>()
            if (path.isNotEmpty()) {
                comp.icon = AllIcons.Nodes.ReadAccess
                comp.text = path.joinToString(" -> ") { it.name }
            } else {
                comp.icon = null
                comp.text = "[Empty Path]"
            }
        }
    }
}
