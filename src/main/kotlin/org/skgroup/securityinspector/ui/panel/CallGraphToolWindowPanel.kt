package org.skgroup.securityinspector.ui.panel

import com.intellij.openapi.project.Project
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph
import org.skgroup.securityinspector.ui.component.CallGraphUIComponents
import org.skgroup.securityinspector.ui.service.CallGraphMemoryService
import org.skgroup.securityinspector.ui.service.CallGraphGenerator
import org.skgroup.securityinspector.ui.service.CallGraphSearcher

/**
 * CallGraphToolWindowPanel 是调用图工具窗口的主面板
 *
 * @property project IDEA项目
 * @constructor Create Call graph tool window panel
 */
class CallGraphToolWindowPanel(private val project: Project) {
    val uiComponents = CallGraphUIComponents(project)
    val mainPanel = uiComponents.createMainPanel()

    private val service = CallGraphMemoryService.getInstance(project)

    init {
        initEventListeners()
        loadPersistedState()
    }

    private fun initEventListeners() {
        uiComponents.runAnalysisButton.addActionListener {
            uiComponents.runAnalysisButton.isEnabled = false
            CallGraphGenerator.generate(
                project,
                uiComponents.progressBar,
                uiComponents,
                uiComponents.rootListModel,
                uiComponents.searchComboBox
            )
            uiComponents.runAnalysisButton.isEnabled = true
        }

        uiComponents.searchButton.addActionListener {
            CallGraphSearcher.search(
                sourceField = uiComponents.sourceField,
                sinkField = uiComponents.sinkField,
//                infoArea = uiComponents.infoArea,
                searchResultRootNode = uiComponents.searchResultRootNode,
                searchResultTreeModel = uiComponents.searchResultTreeModel,
                project = project
            )
        }

    }

    private fun loadPersistedState() {
        service.getCallGraph()?.let {
//            uiComponents.statusPanel.append("Loaded CallGraph from persistent state.\n")
            updateRootAndSinkLists(it)
        } ?: run {
//            uiComponents.statusPanel.append("No existing CallGraph found.\n")
        }
    }

    private fun updateRootAndSinkLists(callGraph: CallGraph) {
        uiComponents.rootListModel.clear()

        val (roots, sinks) = CallGraphSearcher.calculateRootsAndSinks(callGraph)
        roots.forEach(uiComponents.rootListModel::addElement)
    }
}
