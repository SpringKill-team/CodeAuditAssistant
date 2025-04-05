package org.skgroup.CodeAuditAssistant.analysis.ast.nodes

import org.skgroup.CodeAuditAssistant.analysis.ast.SourceSpan
import org.skgroup.CodeAuditAssistant.enums.RefMode

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
    val refMode: RefMode,
    var signature: String = "",
    override val sourceSpan: SourceSpan? = null
) : BaseAstNode(
    nodeType = "MethodDeclaration",
    children = body,
    sourceSpan = sourceSpan
){
    init {
        signature = "$className.$name(${parameters.joinToString { it.type }})"
    }
    override fun toString(): String {
        return "MethodNode(className='$className', name='$name', returnType='$returnType', parameters=$parameters, body=$body, sourceSpan=$sourceSpan)"
    }
}