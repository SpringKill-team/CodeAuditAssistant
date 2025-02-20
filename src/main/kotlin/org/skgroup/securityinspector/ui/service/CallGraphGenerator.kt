package org.skgroup.securityinspector.ui.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.Gray
import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraphBuilder
import org.skgroup.securityinspector.utils.GraphUtils
import java.awt.Font
import javax.swing.*

object CallGraphGenerator {
    fun generate(
        project: Project,
        progressBar: JProgressBar,
        infoArea: JTextArea,
        rootListModel: DefaultListModel<MethodNode>,
        sinkListModel: DefaultListModel<MethodNode>
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

            override fun run(indicator: ProgressIndicator) {
                ApplicationManager.getApplication().runReadAction {
                    javaFiles = GraphUtils.findAllJavaFiles(project)
                }

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
                        infoArea.append("Rebuilt CallGraph with ${graph.nodes.size} methods.\n")
                        updateRootAndSinkLists(graph, rootListModel)
                    }
                }
            }

            override fun onFinished() {
                SwingUtilities.invokeLater {
                    progressBar.isVisible = false
                    progressBar.isIndeterminate = false
                }
            }

            override fun onCancel() {
                SwingUtilities.invokeLater {
                    infoArea.append("[Cancelled] Call graph generation was cancelled.\n")
                }
            }
        })
    }

    private fun updateRootAndSinkLists(
        callGraph: CallGraph,
        rootListModel: DefaultListModel<MethodNode>,
//        sinkListModel: DefaultListModel<MethodNode>
    ) {
        rootListModel.clear()
//        sinkListModel.clear()

        val allCallees = callGraph.edges.values.flatten().toSet()
        val roots = callGraph.nodes.filter { it !in allCallees }.sortedBy { it.name }

        val sinks = callGraph.nodes.filter { node ->
            val callees = callGraph.edges[node]
            callees.isNullOrEmpty()
        }.sortedBy { it.name }

        roots.forEach(rootListModel::addElement)
//        sinks.forEach(sinkListModel::addElement)
    }
}

