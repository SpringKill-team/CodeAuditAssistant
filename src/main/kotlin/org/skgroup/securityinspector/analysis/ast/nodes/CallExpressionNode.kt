package org.skgroup.securityinspector.analysis.ast.nodes

import org.skgroup.securityinspector.analysis.ast.SourceSpan

/**
 * Call expression node是表示方法调用的节点
 * @author springkill
 *
 * @property caller         调用者
 * @property methodName     方法名
 * @property argumentTypes  参数类型
 * @property sourceSpan     源码位置信息
 * @constructor Create empty Call expression node
 */
data class CallExpressionNode(
    val caller: String?,
    val methodName: String,
    val argumentTypes: List<String>,
    override val sourceSpan: SourceSpan? = null
) : BaseAstNode(
    nodeType = "CallExpression",
    sourceSpan = sourceSpan
)