package org.skgroup.CodeAuditAssistant.analysis.graphs.callgraph

import org.skgroup.CodeAuditAssistant.analysis.ast.nodes.MethodSigNode

/**
 * 类描述：MethodsigGraph 类用于存储所有方法的信息。
 *
 * @author springkill
 * @version 1.0
 */
data class MethodSigGraph(
    val nodes: MutableSet<MethodSigNode> = LinkedHashSet(),
) {
    fun merge(other: MethodSigGraph) {
        this.nodes.addAll(other.nodes)
    }
}
