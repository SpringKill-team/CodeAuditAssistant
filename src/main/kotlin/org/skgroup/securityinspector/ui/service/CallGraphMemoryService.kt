package org.skgroup.securityinspector.ui.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph
import org.skgroup.securityinspector.analysis.graphs.callgraph.MethodSigGraph

/**
 * 类描述：CallGraphMemoryService 类用于在内存中维护 CallGraph 实例
 *
 * @author springkill
 * @version 1.0
 * @since 2025/2/11
 */
@Service(Service.Level.PROJECT)
class CallGraphMemoryService(private val project: Project) {

    // 内存中维护的图结构
    private var callGraph: CallGraph? = null
    private var methodSigGraph: MethodSigGraph? = null

    /**
     * 获取当前内存中的调用图（可能为空）
     */
    fun getCallGraph(): CallGraph? = callGraph
    fun getMethodSigGraph(): MethodSigGraph? = methodSigGraph

    /**
     * Set call graph 设置（更新）新的调用图
     *
     * @param newGraph
     */
    fun setCallGraph(newGraph: CallGraph) {
        callGraph = newGraph
    }

    fun setMethodSigGraph(newGraph: MethodSigGraph) {
        methodSigGraph = newGraph
    }

    companion object {
        fun getInstance(project: Project): CallGraphMemoryService {
            return project.getService(CallGraphMemoryService::class.java)
        }
    }
}
