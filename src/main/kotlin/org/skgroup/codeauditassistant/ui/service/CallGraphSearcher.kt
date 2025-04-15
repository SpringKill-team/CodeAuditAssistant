package org.skgroup.codeauditassistant.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import org.skgroup.codeauditassistant.analysis.ast.nodes.MethodNode
import org.skgroup.codeauditassistant.analysis.ast.nodes.MethodSigNode
import org.skgroup.codeauditassistant.analysis.ast.nodes.ParameterNode
import org.skgroup.codeauditassistant.analysis.graphs.callgraph.CallGraph
import org.skgroup.codeauditassistant.analysis.graphs.callgraph.MethodSigGraph
import org.skgroup.codeauditassistant.utils.GraphUtils
import java.util.regex.PatternSyntaxException
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * CallGraphSearcher 用于搜索调用图中的路径
 * 通过搜索源方法和汇聚方法，找到它们之间的调用路径
 *
 * @author springkill
 * @version 1.0
 */
object CallGraphSearcher {

    private fun String.safeToRegex(): Result<Regex> = runCatching {
        toRegex().also {
            if (isEmpty()) throw PatternSyntaxException("Empty pattern", "", 0)
        }
    }

    fun search(
        methodSigGraph: MethodSigGraph,
        className: String?,
        accessModifier: String?,
        methodModifier: String?,
        methodName: String?,
        paramCount: Int?,
        paramType: String?,
        paramName: String?,
        varArgs: String?,
        throwClause: String?,
        returnType: String?,
        annotations: String?,
        searchResultRootNode: DefaultMutableTreeNode,
        searchResultTreeModel: DefaultTreeModel,
    ) {
        clearPreviousResults(searchResultRootNode,searchResultTreeModel)
        val result = methodSigGraph.nodes.filter {
            val parameterType = getParamString(it.methodParams)
            val parameterName = getParamString(it.methodParams)
            (
                    className == "" || it.className == className) &&
                    (accessModifier == "" || it.methodAccessModifier == accessModifier) &&
                    (methodModifier == "" || it.methodModifier == methodModifier) &&
                    (methodName == "" || it.methodName == methodName) &&
                    (paramCount == null || it.methodParams.size == paramCount) &&
                    (paramType == "" || containsString(paramType!!, parameterType)) &&
                    (paramName == "" || containsString(paramName!!, parameterName)) &&
                    (varArgs == "" || it.methodVarargs == varArgs.toBoolean()) &&
                    (throwClause == "" || containsString(throwClause!!, getListString(it.methodThrowsClause))) &&
                    (returnType == "" || it.methodReturnType == returnType) &&
                    (annotations == "" || containsString(annotations!!, getListString(it.methodAnnotations)))
        }
        updateTreeWithList(result, searchResultRootNode, searchResultTreeModel)
    }

    fun getParamString(parameterNodeList: List<ParameterNode>): String {
        return parameterNodeList.joinToString(",") { it.type }
    }

    fun getListString(list: List<String>): String {
        return list.joinToString(",")
    }

    fun containsString(searchString: String, targetString: String): Boolean {
        val searchParts = searchString.split(",").map { it.trim() }
        val targetParts = targetString.split(",").map { it.trim() }

        for (i in searchParts.indices) {
            val searchPart = searchParts[i]
            val targetPart = targetParts[i]
            if (searchPart == "*") continue
            if (searchPart != targetPart) return false
        }
        return true
    }

    /**
     * Calculate roots and sinks 计算root和sink
     *
     * @param callGraph callGraph 调用图对象，其中包含所有的节点和边
     * @return  返回一个 Pair，包含了所有的 root 和 sink
     */
    fun calculateRootsAndSinks(callGraph: CallGraph) = Pair(
        callGraph.findRoots().sortedBy { it.name },
        callGraph.findSinks().sortedBy { it.name }
    )

    /**
     * Search 搜索方法根据用户输入的源方法和汇聚方法的正则表达式，在调用图中搜索路径。
     * 根据不同情况：
     *  - 如果 source 和 sink 均为空，则不进行搜索；
     *  - 如果仅输入了 sink 方法，则执行只搜索 sink 的逻辑；
     *  - 否则执行同时包含 source 和 sink 的全量搜索。
     *
     * @param sourceField   root方法输入框
     * @param sinkField     sink方法输入框
     * @param searchResultRootNode  用于展示搜索结果的树的根节点
     * @param searchResultTreeModel 用于展示搜索结果的树的模型
     * @param project       IDEA工程项目
     */
    fun search(
        sourceField: JBTextField,
        sinkField: JBTextField,
        searchResultRootNode: DefaultMutableTreeNode,
        searchResultTreeModel: DefaultTreeModel,
        project: Project
    ) {
        val graph = CallGraphMemoryService.getInstance(project).getCallGraph() ?: return

        val (srcText, sinkText) = sourceField.text.trim() to sinkField.text.trim()
        clearPreviousResults(searchResultRootNode, searchResultTreeModel)

        when {
            srcText.isEmpty() && sinkText.isEmpty() -> Unit
            sinkText.isNotEmpty() && srcText.isEmpty() -> {
                handleSinkOnlySearch(sinkText, graph, searchResultRootNode, searchResultTreeModel)
            }

            else -> {
                handleFullSearch(srcText, sinkText, graph, searchResultRootNode, searchResultTreeModel)
            }
        }
    }

    /**
     * Clear previous results 清除之前的搜索结果
     *
     * @param root 树的根节点
     * @param model 用于展示树结构的模型
     */
    private fun clearPreviousResults(root: DefaultMutableTreeNode, model: DefaultTreeModel) {
        SwingUtilities.invokeLater {
            root.removeAllChildren()
            model.reload(root)
        }
    }

    /**
     * Handle sink only search 处理只搜索 sink 的情况
     *
     * @param sinkText              sink 方法正则表达式字符串
     * @param graph                 当前的调用图
     * @param searchResultRootNode  用于展示搜索结果的树的根节点
     * @param searchResultTreeModel 用于展示搜索结果的树的模型
     */
    private fun handleSinkOnlySearch(
        sinkText: String,
        graph: CallGraph,
        searchResultRootNode: DefaultMutableTreeNode,
        searchResultTreeModel: DefaultTreeModel,
    ) {
        sinkText.safeToRegex()
            .onSuccess { sinkRegex ->
                val sinkNodes = graph.nodes.filter { sinkRegex.containsMatchIn(it.name) }
                if (sinkNodes.isEmpty()) return

                val paths = graph.nodes.flatMap { node ->
                    when {
                        sinkNodes.contains(node) -> listOf(listOf(node))
                        else -> sinkNodes.mapNotNull { sink -> GraphUtils.findPath(graph, node, sink) }
                    }
                }
                updateTreeWithPaths(paths, searchResultRootNode, searchResultTreeModel)
            }
            .onFailure { handleRegexError(sinkText, it) }

    }

    /**
     * Handle full search 处理同时包含 source 和 sink 的全量搜索
     *
     * @param srcPattern    待匹配的 source 方法正则表达式
     * @param sinkPattern   待匹配的 sink 方法正则表达式
     * @param graph         当前的调用图
     * @param rootNode      用于展示搜索结果的树的根节点
     * @param treeModel     用于展示搜索结果的树的模型
     */
    private fun handleFullSearch(
        srcPattern: String,
        sinkPattern: String,
        graph: CallGraph,
        rootNode: DefaultMutableTreeNode,
        treeModel: DefaultTreeModel
    ) {
        val srcRegex = srcPattern.safeToRegex()
        val sinkRegex = sinkPattern.safeToRegex()

        if (srcRegex.isFailure || sinkRegex.isFailure) {
            handleRegexError("$srcPattern/$sinkPattern", srcRegex.exceptionOrNull() ?: sinkRegex.exceptionOrNull()!!)
            return
        }

//        val sources = graph.nodes.filter { srcRegex.getOrThrow().containsMatchIn(it.name) }
        val sources = graph.nodes.filter { it.name == srcPattern }
//        val sinks = graph.nodes.filter { sinkRegex.getOrThrow().containsMatchIn(it.name) }
        val sinks = graph.nodes.filter { it.name == sinkPattern }
        when {
            sources.isEmpty() -> Unit
            sinks.isEmpty() -> Unit
            else -> performPathSearch(sources, sinks, graph, rootNode, treeModel)
        }
    }

    /**
     * Perform path search 执行路径搜索
     *
     * @param sources   source 方法节点列表
     * @param sinks     sink 方法节点列表
     * @param graph     当前调用图
     * @param rootNode  用于展示搜索结果的树的根节点
     * @param treeModel 用于展示搜索结果的树的模型
     */
    private fun performPathSearch(
        sources: List<MethodNode>,
        sinks: List<MethodNode>,
        graph: CallGraph,
        rootNode: DefaultMutableTreeNode,
        treeModel: DefaultTreeModel
    ) {
        val paths = sources.flatMap { src ->
            sinks.mapNotNull { sink -> GraphUtils.findPath(graph, src, sink) }
        }
        updateTreeWithPaths(paths, rootNode, treeModel)
    }

    /**
     * Update tree with paths 用路径更新树
     *
     * @param paths     搜索到的调用路径列表，每个路径是由 MethodNode 组成的列表
     * @param rootNode  用于展示搜索结果的树的根节点
     * @param treeModel 用于展示搜索结果的树的模型
     */
    private fun updateTreeWithPaths(
        paths: List<List<MethodNode>>,
        rootNode: DefaultMutableTreeNode,
        treeModel: DefaultTreeModel
    ) {
        SwingUtilities.invokeLater {
            paths.forEach { path ->
                rootNode.add(createPathNode(path))
            }
            treeModel.reload(rootNode)
        }
    }

    private fun updateTreeWithList(
        paths: List<MethodSigNode>,
        rootNode: DefaultMutableTreeNode,
        treeModel: DefaultTreeModel
    ) {
        SwingUtilities.invokeLater {
            paths.forEach { path ->
                rootNode.add(DefaultMutableTreeNode(path))
            }
            treeModel.reload(rootNode)
        }
    }

    /**
     * Create path node 创建路径节点
     *
     * @param path 一条调用路径，由多个 MethodNode 组成
     * @return DefaultMutableTreeNode 表示该调用路径的树节点
     */
    private fun createPathNode(path: List<MethodNode>) = DefaultMutableTreeNode(
        if (path.size == 1) "[SINGLE] ${path.first().name}"
        else "${path.first().name} -> ${path.last().name}"
    ).apply {
        path.forEach { add(DefaultMutableTreeNode(it)) }
    }

    /**
     * Handle regex error 处理正则表达式错误
     *
     * @param pattern   正则表达式字符串
     * @param exception 异常对象
     */
    private fun handleRegexError(pattern: String, exception: Throwable) {
        SwingUtilities.invokeLater {
            Messages.showErrorDialog(
                "Invalid regex pattern: $pattern\n${exception.message}",
                "Regex Syntax Error"
            )
        }
    }
}
