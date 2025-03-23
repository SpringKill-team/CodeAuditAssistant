package org.skgroup.securityinspector.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph
import org.skgroup.securityinspector.utils.GraphUtils
import java.util.regex.PatternSyntaxException
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

object CallGraphSearcher {

    fun calculateRootsAndSinks(callGraph: CallGraph): Pair<List<MethodNode>, List<MethodNode>> {
        val allCallees = callGraph.edges.values.flatten().toSet()
        val roots = callGraph.nodes
            .filter { it !in allCallees }
            .sortedBy { it.name }

        val sinks = callGraph.nodes
            .filter { node ->
                callGraph.edges[node].isNullOrEmpty()
            }
            .sortedBy { it.name }

        return Pair(roots, sinks)
    }

    /**
     * Search 搜索方法
     *
     * @param sourceField   root方法输入框
     * @param sinkField     sink方法输入框
     * @param infoArea      信息展示区
     * @param searchResultRootNode  用于展示搜索结果的树的根节点
     * @param searchResultTreeModel 用于展示搜索结果的树的模型
     * @param project       IDEA工程项目
     */
    fun search(
        sourceField: JBTextField,
        sinkField: JBTextField,
//        infoArea: JBTextArea,
        // 用树的根节点和模型来展示结果
        searchResultRootNode: DefaultMutableTreeNode,
        searchResultTreeModel: DefaultTreeModel,
        project: Project
    ) {
        val service = CallGraphMemoryService.getInstance(project)
        val graph = service.getCallGraph() ?: run {
            return
        }

        val srcText = sourceField.text.trim()
        val sinkText = sinkField.text.trim()

        // 清空之前的搜索结果
        SwingUtilities.invokeLater {
            searchResultRootNode.removeAllChildren()
            searchResultTreeModel.reload()
        }

        when {
            srcText.isEmpty() && sinkText.isEmpty() -> {
//                appendErrorMessage(infoArea, "Both Source & Sink are empty!")
            }
            sinkText.isNotEmpty() && srcText.isEmpty() -> {
                handleSinkOnlySearch(sinkText, graph, searchResultRootNode, searchResultTreeModel)
            }
            else -> {
                handleFullSearch(srcText, sinkText, graph, searchResultRootNode, searchResultTreeModel)
            }
        }
    }

    private fun handleSinkOnlySearch(
        sinkText: String,
        graph: CallGraph,
        searchResultRootNode: DefaultMutableTreeNode,
        searchResultTreeModel: DefaultTreeModel,
//        infoArea: JBTextArea
    ) {
        try {
            val sinkRegex = sinkText.toRegex()
            val sinkNodes = graph.nodes.filter { sinkRegex.containsMatchIn(it.name) }

            if (sinkNodes.isEmpty()) {
                return
            }

            var found = false
            graph.nodes.forEach { node ->
                if (sinkNodes.contains(node)) {
                    // 如果本身就是 sink，就只生成一个单节点的列表
                    addSearchResultToTree(listOf(node), searchResultRootNode, searchResultTreeModel)
                    found = true
                } else {
                    // DFS查找路径
                    sinkNodes.forEach { sink ->
                        GraphUtils.findPath(graph, node, sink)?.let { path ->
                            addSearchResultToTree(path, searchResultRootNode, searchResultTreeModel)
                            found = true
                        }
                    }
                }
            }

            if (!found) {
//                appendInfoMessage(infoArea, "No path found to $sinkText")
            }
        } catch (e: PatternSyntaxException) {
//            handleRegexError(infoArea, sinkText, e)
        }
    }

    private fun handleFullSearch(
        srcText: String,
        sinkText: String,
        graph: CallGraph,
        searchResultRootNode: DefaultMutableTreeNode,
        searchResultTreeModel: DefaultTreeModel,
//        infoArea: JBTextArea
    ) {
        try {
            val srcRegex = srcText.toRegex()
            val sinkRegex = sinkText.toRegex()

            val sources = graph.nodes.filter { srcRegex.containsMatchIn(it.name) }
            val sinks = graph.nodes.filter { sinkRegex.containsMatchIn(it.name) }

            when {
//                sources.isEmpty() -> appendInfoMessage(infoArea, "No method matches source: $srcText")
//                sinks.isEmpty() -> appendInfoMessage(infoArea, "No method matches sink: $sinkText")
                else -> performPathSearch(sources, sinks, graph, searchResultRootNode, searchResultTreeModel)
            }
        } catch (e: PatternSyntaxException) {
//            handleRegexError(infoArea, "$srcText/$sinkText", e)
        }
    }

    private fun performPathSearch(
        sources: List<MethodNode>,
        sinks: List<MethodNode>,
        graph: CallGraph,
        searchResultRootNode: DefaultMutableTreeNode,
        searchResultTreeModel: DefaultTreeModel,
//        infoArea: JBTextArea
    ) {
        var foundAny = false
        sources.forEach { src ->
            sinks.forEach { sink ->
                GraphUtils.findPath(graph, src, sink)?.let { path ->
                    // 在树上新增一条搜索结果
                    addSearchResultToTree(path, searchResultRootNode, searchResultTreeModel)
                    foundAny = true
                }
            }
        }

        if (!foundAny) {
//            appendInfoMessage(infoArea, "No path found between ${sources.first().name} -> ${sinks.first().name}")
        }
    }

    /**
     * 将某条调用链以可折叠树的方式添加到根节点下
     */
    private fun addSearchResultToTree(
        path: List<MethodNode>,
        rootNode: DefaultMutableTreeNode,
        treeModel: DefaultTreeModel
    ) {
        // 如果 path 为空，不做任何处理
        if (path.isEmpty()) return

        SwingUtilities.invokeLater {
            // 用“头 -> 尾”作为父节点的显示文本
            val source = path.first()
            val sink = path.last()
            var title = "${source.name} -> ${sink.name}"
            if (source == sink) title = "[SINGLE NODE] ${sink.name}"

            // 创建父节点
            val parentNode = DefaultMutableTreeNode(title)

            // 将整个调用链里的每个节点，放到 parentNode 的子节点中
            path.forEachIndexed { index, methodNode ->
                val childNode = DefaultMutableTreeNode(methodNode)
                parentNode.add(childNode)
            }

            // 将 parentNode 挂到根节点下
            rootNode.add(parentNode)

            // 刷新
            treeModel.reload(rootNode)
        }
    }

    private fun appendErrorMessage(area: JBTextArea, message: String) {
        SwingUtilities.invokeLater {
            area.append("\n[ERROR] $message\n")
        }
    }

    private fun appendInfoMessage(area: JBTextArea, message: String) {
        SwingUtilities.invokeLater {
            area.append("\n[INFO] $message\n")
        }
    }

    private fun handleRegexError(area: JBTextArea, pattern: String, e: PatternSyntaxException) {
        SwingUtilities.invokeLater {
            Messages.showErrorDialog(
                "Invalid regex pattern: $pattern\n${e.description}",
                "Regex Syntax Error"
            )
            area.append("\n[ERROR] Invalid regex: ${e.message}\n")
        }
    }
}
