package org.skgroup.codeauditassistant.ui.renderer

/**
 * 类描述：SinkTreeCellRenderer 类用于 SinkTree的渲染。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/4/7
 */
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.skgroup.codeauditassistant.utils.IconUtil
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class SinkTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as? DefaultMutableTreeNode ?: return
        val userObject = node.userObject.toString()

        // 根据节点层级设置图标
        when {
            node.parent == null -> {
                icon = IconUtil.sinkIcon
                append(userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            node.parent == tree.model.root -> {
                icon = AllIcons.Nodes.Folder
                append(userObject.substringBefore(" ("))
            }

            leaf -> {
                icon = AllIcons.General.Warning
                append(userObject.substringBefore(" ("))
            }
        }
    }
}
