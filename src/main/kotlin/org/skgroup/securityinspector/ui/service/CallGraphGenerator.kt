package org.skgroup.securityinspector.ui.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.Gray
import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraphBuilder
import org.skgroup.securityinspector.enums.AnalysisScope
import org.skgroup.securityinspector.ui.component.CallGraphUIComponents
import org.skgroup.securityinspector.ui.component.ModuleSelectorDialog
import org.skgroup.securityinspector.utils.GraphUtils
import java.awt.Font
import java.util.concurrent.CompletableFuture
import javax.swing.*

object CallGraphGenerator {
    fun generate(
        project: Project,
        progressBar: JProgressBar,
        uiComponents: CallGraphUIComponents,
        rootListModel: DefaultListModel<MethodNode>,
        searchComboBox: ComboBox<AnalysisScope>
    ) {
        progressBar.apply {
            isVisible = true
            isIndeterminate = true
            background = Gray._240
            font = Font("SansSerif", Font.BOLD, 12)
            string = "Initializing..."
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating CallGraph", true) {
            private var newGraph: CallGraph? = null
            private lateinit var javaFiles: List<PsiFile>

            //private var scope = ProjectScope.getAllScope(project)
            private var scope = ProjectScope.getProjectScope(project)

            override fun run(indicator: ProgressIndicator) {
                val future = CompletableFuture<Unit>()

                if (searchComboBox.selectedItem != AnalysisScope.ENTIRE_PROJECT) {
                    ApplicationManager.getApplication().invokeLater {
                        scope = showModuleSelectorDialog(project).moduleScope
                        ApplicationManager.getApplication().runReadAction {
                            javaFiles = GraphUtils.findScopeJavaFiles(project, scope)
                            future.complete(Unit)
                        }
                    }
                } else {
                    ApplicationManager.getApplication().runReadAction {
                        javaFiles = GraphUtils.findAllJavaFiles(project)
                        future.complete(Unit)
                    }
                }

                if (indicator.isCanceled) {
                    uiComponents.addBuildInfo("Call graph generation was cancelled.")
                    return
                }

                // 等待任务完成
                try {
                    future.get()
                } catch (e: Exception) {
                    e.printStackTrace()
                    uiComponents.addErrorInfo("Build CallGraph failed: ${e.message}")
                    return
                }

                // 因为invokeLater是异步的，所以要确保javaFiles被赋值后再执行后续逻辑
                indicator.isIndeterminate = false
                indicator.fraction = 0.0
                indicator.text = "Analyzing ${javaFiles.size} files"

                val builder = CallGraphBuilder()
                val totalFiles = javaFiles.size

                javaFiles.forEachIndexed { index, psiFile ->
                    if (indicator.isCanceled) return

                    ApplicationManager.getApplication().runReadAction {
                        if (psiFile is PsiJavaFile) {
                            psiFile.accept(builder)
                        }
                    }

                    SwingUtilities.invokeLater {
                        progressBar.string = "Building CallGraph (${index + 1}/$totalFiles)"
                    }

                    indicator.fraction = (index + 1).toDouble() / totalFiles
                    indicator.text2 = "${psiFile.name} (${index + 1}/$totalFiles)"
                }

                newGraph = builder.getCallGraph(project)
            }

            override fun onSuccess() {
                newGraph?.let { graph ->
                    ApplicationManager.getApplication().invokeLater {
                        CallGraphMemoryService.getInstance(project).setCallGraph(graph)
                        // 更新信息面板
                        uiComponents.updateInfoPanel(graph)
                        updateRootList(graph, rootListModel)
                    }
                }
            }

            override fun onFinished() {
                SwingUtilities.invokeLater {
                    uiComponents.addBuildInfo("Call Graph Finished")
                    progressBar.isVisible = false
                    progressBar.isIndeterminate = false
                }
            }

            override fun onCancel() {
                SwingUtilities.invokeLater {
                    uiComponents.addBuildInfo("Call graph generation was cancelled.")
                }
            }
        })
    }

    /**
     * Generate 方法重载，为单个Method生成调用图
     *
     * @param project
     * @param method
     * @param progressBar
     * @param infoArea
     * @param rootListModel
     */
    fun generate(
        project: Project,
        method: PsiMethod,
        progressBar: JProgressBar,
        uiComponents: CallGraphUIComponents,
        rootListModel: DefaultListModel<MethodNode>,
    ) {
        progressBar.apply {
            isVisible = true
            isIndeterminate = true
            background = Gray._240
            font = Font("SansSerif", Font.BOLD, 12)
            string = "Initializing..."
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating CallGraph", true) {
            private var tempGraph: CallGraph? = null

            override fun run(indicator: ProgressIndicator) {

                if (indicator.isCanceled) {
                    uiComponents.addBuildInfo("Call graph generation was cancelled.")
                    return
                }

                indicator.isIndeterminate = false
                indicator.fraction = 0.0
                indicator.text = "Analyzing ${method.name}"

                val builder = CallGraphBuilder()
                ReferencesSearch.search(method, GlobalSearchScope.projectScope(project)).forEach { reference ->
                    var callerMethod: PsiMethod = method
                    ApplicationManager.getApplication().runReadAction {
                        callerMethod = PsiTreeUtil.getParentOfType(reference.element, PsiMethod::class.java)
                            ?: return@runReadAction
                    }
                    if (callerMethod != method) {
                        generate(project, callerMethod, progressBar, uiComponents, rootListModel)
                    }
                }
                ApplicationManager.getApplication().runReadAction {
                    method.accept(builder)
                }
                progressBar.string = "Building CallGraph for method ${method.name}"

                tempGraph = builder.getCallGraph(project)
            }

            override fun onSuccess() {
                tempGraph?.let { delta ->
                    ApplicationManager.getApplication().invokeLater {
                        val memoryService = CallGraphMemoryService.getInstance(project)
                        val currentGraph = memoryService.getCallGraph() ?: CallGraph()

                        // 合并
                        currentGraph.merge(delta)
                        memoryService.setCallGraph(currentGraph)

                        uiComponents.updateInfoPanel(currentGraph)
                        updateRootList(currentGraph, rootListModel)
                    }
                }
            }

            override fun onFinished() {
                SwingUtilities.invokeLater {
                    progressBar.isVisible = false
                    progressBar.isIndeterminate = false
                    uiComponents.addBuildInfo("Call Graph Finished")
                }
            }

            override fun onCancel() {
                SwingUtilities.invokeLater {
                    uiComponents.addBuildInfo("Call graph generation was cancelled.")
                }
            }
        })
    }

    private fun updateRootList(
        callGraph: CallGraph,
        rootListModel: DefaultListModel<MethodNode>,
    ) {
        rootListModel.clear()

        val allCallees = callGraph.edges.values.flatten().toSet()
        val roots = callGraph.nodes.filter { it !in allCallees }.sortedBy { it.name }

        roots.forEach(rootListModel::addElement)
    }

    private fun showModuleSelectorDialog(project: Project): Module {
        val dialog = ModuleSelectorDialog(project)
        if (dialog.showAndGet()) { // 显示窗口并等待用户操作
            val selectedModule = dialog.getSelectedModuleName()
            return selectedModule
        }
        return dialog.getSelectedModuleName()
    }
}

