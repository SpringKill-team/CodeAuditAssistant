package org.skgroup.securityinspector.analysis.ast.nodes

import org.skgroup.securityinspector.analysis.ast.SourceSpan

/**
 * Method node 是用于表示方法节点
 * @author springkill
 *
 * @property name       方法名
 * @property returnType 返回类型
 * @property parameters 参数
 * @property body       方法体
 * @property sourceSpan 源码位置信息
 * @constructor Create empty Method node
 */
data class MethodNode(
    val className: String,
    val name: String,
    val returnType: String,
    val parameters: List<ParameterNode>,
    val body: List<AstNode> = emptyList(),
    override val sourceSpan: SourceSpan? = null
) : BaseAstNode(
    nodeType = "MethodDeclaration",
    children = body,
    sourceSpan = sourceSpan
)