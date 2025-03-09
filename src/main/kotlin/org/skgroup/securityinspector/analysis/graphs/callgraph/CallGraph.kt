package org.skgroup.securityinspector.analysis.graphs.callgraph

import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.enums.EdgeType
import org.skgroup.securityinspector.enums.NodeType

/**
 * Call graph 是表示调用图的数据结构
 *
 * @property nodes  节点
 * @property edges  边
 * @constructor Create empty Call graph
 */
data class CallGraph(
    val nodes: MutableSet<MethodNode> = LinkedHashSet(),
    val edges: MutableMap<MethodNode, MutableSet<MethodNode>> = mutableMapOf(),
    val edgeTypes: MutableMap<Pair<MethodNode, MethodNode>, EdgeType> = mutableMapOf(),
    val nodeTypes: MutableMap<MethodNode, NodeType> = mutableMapOf()
) {
    fun addEdge(from: MethodNode, to: MethodNode, type: EdgeType) {
        edges.getOrPut(from) { mutableSetOf() }.add(to)
        edgeTypes[Pair(from, to)] = type
        nodes.add(from)
        nodes.add(to)
    }

    fun addSpecialNode(node: MethodNode, type: NodeType) {
        nodeTypes[node] = type
    }

    fun merge(other: CallGraph) {
        // 合并节点
        this.nodes.addAll(other.nodes)

        // 合并边
        other.edges.forEach { (from, toSet) ->
            val existingSet = this.edges.getOrPut(from) { mutableSetOf() }
            existingSet.addAll(toSet)
        }

        this.edgeTypes.putAll(other.edgeTypes)
        this.nodeTypes.putAll(other.nodeTypes)
    }

}