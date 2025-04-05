package org.skgroup.CodeAuditAssistant.ui.panel

import com.intellij.openapi.project.Project
import org.skgroup.CodeAuditAssistant.analysis.graphs.callgraph.CallGraph
import org.skgroup.CodeAuditAssistant.ui.component.CallGraphUIComponents
import org.skgroup.CodeAuditAssistant.ui.service.CallGraphMemoryService
import org.skgroup.CodeAuditAssistant.ui.service.CallGraphGenerator
import org.skgroup.CodeAuditAssistant.ui.service.CallGraphSearcher

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
                searchResultRootNode = uiComponents.searchResultRootNode,
                searchResultTreeModel = uiComponents.searchResultTreeModel,
                project = project
            )
        }

        uiComponents.findMethodButton.addActionListener {
            val sigGraph = service.getMethodSigGraph()
            sigGraph?.let {
                CallGraphSearcher.search(
                    it,
                    uiComponents.classNameField.text,
                    uiComponents.accessModifierField.text,
                    uiComponents.methodModifierField.text,
                    uiComponents.methodNameField.text,
                    uiComponents.paramCountField.text.toIntOrNull(),
                    uiComponents.paramTypeField.text,
                    uiComponents.paramNameField.text,
                    uiComponents.varargsField.text,
                    uiComponents.throwsClauseField.text,
                    uiComponents.returnTypeField.text,
                    uiComponents.annotationsField.text,
                    uiComponents.searchResultRootNode,
                    uiComponents.searchResultTreeModel
                )
            }
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
