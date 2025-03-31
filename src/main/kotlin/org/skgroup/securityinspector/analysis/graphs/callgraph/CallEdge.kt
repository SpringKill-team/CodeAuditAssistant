package org.skgroup.securityinspector.analysis.graphs.callgraph

import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.enums.EdgeType

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
