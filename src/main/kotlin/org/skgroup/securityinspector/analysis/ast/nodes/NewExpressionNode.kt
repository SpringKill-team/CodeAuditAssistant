package org.skgroup.securityinspector.analysis.ast.nodes

import org.skgroup.securityinspector.analysis.ast.SourceSpan

/**
 * New expression node 是表示 new 表达式的节点
 * @author springkill
 *
 * @property className      类名
 * @property argumentTypes  参数类型
 * @property sourceSpan     源码位置信息
 * @constructor Create empty New expression node
 */
data class NewExpressionNode(
    val className: String,
    val argumentTypes: List<String>,
    override val sourceSpan: SourceSpan? = null
) : BaseAstNode(
    nodeType = "NewExpression",
    sourceSpan = sourceSpan
)