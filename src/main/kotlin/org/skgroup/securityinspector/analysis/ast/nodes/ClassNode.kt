package org.skgroup.securityinspector.analysis.ast.nodes

import org.skgroup.securityinspector.analysis.ast.SourceSpan

/**
 * Class node 是用于表示类节点
 * @author springkill
 *
 * @property name       类名
 * @property packageName 包名
 * @property methods    方法列表
 * @property sourceSpan  源码位置信息
 * @constructor Create empty Class node
 */
data class ClassNode(
    val name: String,
    val packageName: String,
    val methods: List<MethodNode>,
    override val sourceSpan: SourceSpan? = null
) : BaseAstNode(
    nodeType = "Class",
    children = methods,
    sourceSpan = sourceSpan
)