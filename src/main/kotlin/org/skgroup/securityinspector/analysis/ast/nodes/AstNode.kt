package org.skgroup.securityinspector.analysis.ast.nodes

import org.skgroup.securityinspector.analysis.ast.SourceSpan

/**
 * Ast node是抽象语法树的节点，用于表示代码结构
 * @author springkill
 *
 * @constructor Create empty Ast node
 */
interface AstNode {
    val nodeType: String
    val children: List<AstNode>
    val properties: MutableMap<String, Any>
    val sourceSpan: SourceSpan?
}