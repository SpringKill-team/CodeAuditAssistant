package org.skgroup.codeauditassistant.analysis.ast.nodes

import org.skgroup.codeauditassistant.analysis.ast.SourceSpan

/**
 * Base ast node 是基础的ast节点
 * @author springkill
 *
 * @property nodeType   节点类型
 * @property children   子节点
 * @property sourceSpan 源码位置信息
 * @constructor Create empty Base ast node
 */
abstract class BaseAstNode(
    override val nodeType: String,
    override val children: List<AstNode> = emptyList(),
    override val sourceSpan: SourceSpan? = null
) : AstNode {
    override val properties: MutableMap<String, Any> = mutableMapOf()
}