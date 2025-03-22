package org.skgroup.securityinspector.ui.renderer

import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.enums.RefMode
import org.skgroup.securityinspector.utils.IconUtil
import java.awt.Component
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

/**
 * 类描述：ResultTreeRenderer 类用于结果搜索渲染器。
 *
 * @author springkill
 * @version 1.0
 */
class ResultTreeRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        (value as? DefaultMutableTreeNode)?.userObject?.let { userObject ->
            text = when (userObject) {
                is MethodNode -> {
                    val refMode = userObject.refMode
                    when (refMode) {
                        RefMode.CALL -> icon = IconUtil.callIcon
                        RefMode.DECLARATION -> icon = IconUtil.declarationIcon
                        RefMode.NEW -> icon = IconUtil.newIcon
                        else -> {}
                    }
                    "$refMode : ${userObject.name}"
                }
                is String ->{
                    icon = IconUtil.pathIcon
                    userObject
                }
                else -> userObject.toString()
            }
        }
        return this
    }
}
