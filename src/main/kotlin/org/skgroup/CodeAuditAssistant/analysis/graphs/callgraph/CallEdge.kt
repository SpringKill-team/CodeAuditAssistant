package org.skgroup.CodeAuditAssistant.analysis.graphs.callgraph

import org.skgroup.CodeAuditAssistant.analysis.ast.nodes.MethodNode
import org.skgroup.CodeAuditAssistant.enums.EdgeType

/**
 * 类描述：CallPair 类用于。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/3/29
 */
data class CallEdge(
    val caller: MethodNode,
    val callee: MethodNode,
    val edgeType: EdgeType
)
