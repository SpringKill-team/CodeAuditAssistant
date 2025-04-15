package org.skgroup.codeauditassistant.analysis.ast.nodes

import org.skgroup.codeauditassistant.analysis.ast.SourceSpan

/**
 * Parameter node 是表示参数的节点
 * @author springkill
 *
 * @property name       参数名
 * @property type       参数类型
 * @property sourceSpan 源码位置信息
 * @constructor Create empty Parameter node
 */
data class ParameterNode(
    val name: String,
    val type: String,
    override val sourceSpan: SourceSpan? = null
) : BaseAstNode(
    nodeType = "Parameter",
    sourceSpan = sourceSpan
)