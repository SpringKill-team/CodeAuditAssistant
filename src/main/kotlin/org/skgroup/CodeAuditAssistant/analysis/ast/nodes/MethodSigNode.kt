package org.skgroup.CodeAuditAssistant.analysis.ast.nodes

import org.skgroup.CodeAuditAssistant.analysis.ast.SourceSpan

/**
 * 类描述：MethodSegNode 类用于保存定位方法的全部有用信息。
 *
 * @author springkill
 * @version 1.0
 */
data class MethodSigNode(
    val className: String,
    val methodAccessModifier: String,
    val methodModifier: String,
    val methodName: String,
    val methodParams: List<ParameterNode>,
    val methodVarargs: Boolean,
    val methodThrowsClause: List<String>,
    val methodReturnType: String,
    val methodAnnotations: List<String>,
    val body: List<AstNode> = emptyList(),
    override val sourceSpan: SourceSpan? = null
) : BaseAstNode(
    nodeType = "MethodSignature",
    children = body,
    sourceSpan = sourceSpan
)
