package org.skgroup.securityinspector.ui.component

import com.intellij.analysis.problemsView.toolWindow.ProblemsView
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.analysis.graphs.callgraph.CallGraph
import org.skgroup.securityinspector.enums.AnalysisScope
import org.skgroup.securityinspector.ui.renderer.MethodListRenderer
import org.skgroup.securityinspector.ui.renderer.ResultTreeRenderer
import org.skgroup.securityinspector.utils.IconUtil
import org.skgroup.securityinspector.utils.SinkList
import java.awt.*
import java.awt.FlowLayout.LEFT
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CallGraphUIComponents(val project: Project) {
    // 所有UI组件声明
    val runAnalysisButton = JButton("Generate CallGraph")
    val progressBar = createProgressBar()
    val rootListModel = DefaultListModel<MethodNode>()
    val rootList = JBList(rootListModel)

    //    val infoArea = JTextArea().apply { isEditable = false }
    val searchComboBox = ComboBox<AnalysisScope>(AnalysisScope.values())

    val searchResultRootNode = DefaultMutableTreeNode("Search Results")
    val searchResultTreeModel = DefaultTreeModel(searchResultRootNode)
    val searchResultTree = Tree(searchResultTreeModel).apply {
        cellRenderer = ResultTreeRenderer()
        isRootVisible = true
        showsRootHandles = true
    }
    val sourceField = JBTextField(15)
    val sinkField = JBTextField(15)
    val searchButton = JButton("Search")

    val systemPlatformLabel =
        JBLabel("System Plat: ${System.getProperty("os.name")}", LEFT).apply {
            border = JBUI.Borders.empty(5)
            when (System.getProperty("os.name")) {
                "Windows" -> icon = IconUtil.windowsIcon
                "Linux" -> icon = IconUtil.linuxIcon
                "Mac OS X" -> icon = IconUtil.macIcon
                else -> icon = IconUtil.platformIcon
            }
        }

    val graphStatusLabel = JBLabel("CallGraph: Not ready", IconUtil.graphNotReadyIcon, LEFT).apply {
        border = JBUI.Borders.empty(5)
    }

    val methodLabel = JBLabel("MethodNode: 0", IconUtil.methodNotReadyIcon, LEFT).apply {
        border = JBUI.Borders.empty(5)
    }

    val memoryLabel = JBLabel("Used memory: 0 MB", IconUtil.memoryLowIcon, LEFT).apply {
        border = JBUI.Borders.empty(5)
    }

    val buildInfoLabel = JBLabel("Build info: No info", IconUtil.normalIcon, LEFT).apply {
        border = JBUI.Borders.empty(5)
    }

    val searchInfoLabel = JBLabel("Search info: No info", IconUtil.normalIcon, LEFT).apply {
        border = JBUI.Borders.empty(5)
    }

    val errorInfoLabel = JBLabel("Error: No errors", IconUtil.errorIcon, LEFT).apply {
        border = JBUI.Borders.empty(5)
    }

    val statusPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        fun createLabel(jbPanel: JBLabel) = jbPanel.apply {
            border = JBUI.Borders.empty(5)
            iconTextGap = 8
            foreground = JBColor.foreground()
        }
        add(createLabel(systemPlatformLabel))
        add(createLabel(graphStatusLabel))
        add(createLabel(methodLabel))
        add(createLabel(memoryLabel))
        add(createLabel(buildInfoLabel))
        add(createLabel(searchInfoLabel))
        add(createLabel(errorInfoLabel))
    }

    fun createMainPanel(): JBPanel<JBPanel<*>> {
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
        mainPanel.add(createNorthContainer(), BorderLayout.NORTH)
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER)
        return mainPanel
    }

    private fun createProgressBar() = JProgressBar().apply {
        isStringPainted = true
        background = Gray._240
        font = Font("SansSerif", Font.BOLD, 12)
        border = BorderFactory.createEmptyBorder(2, 10, 2, 10)
        preferredSize = Dimension(300, 20)
        isVisible = false
    }

    private fun createNorthContainer(): JBPanel<JBPanel<*>> {
        val topPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT)).apply {
            add(runAnalysisButton)
            add(searchComboBox)
            add(progressBar)
        }
        val searchPanel = createSearchPanel()

        initializeComboBoxes()

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(topPanel, BorderLayout.NORTH)
            add(searchPanel, BorderLayout.SOUTH)

            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
    }

    //搜索模块布局
    private fun createSearchPanel(): JBPanel<JBPanel<*>> {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = GridBagLayout()

        val c = GridBagConstraints().apply {
            insets = Insets(2, 5, 2, 5)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.LINE_START
        }

        c.gridx = 0
        c.gridy = 0
        c.weightx = 0.0
        panel.add(JBLabel("ROOT:"), c)

        c.gridx = 1
        c.weightx = 0.0
        panel.add(sourceField, c)

        c.gridx = 2
        c.weightx = 0.0
        panel.add(JBLabel("SINK:"), c)

        c.gridx = 3
        c.weightx = 0.0
        panel.add(sinkField, c)

        c.gridx = 4
        c.weightx = 0.0
        panel.add(searchButton, c)

        c.weightx = 0.0

        c.gridx = 0
        c.gridy = 1

        c.gridx = 1
        c.weightx = 0.0

        c.gridx = 2
        c.weightx = 0.0

        c.gridx = 3
        c.weightx = 0.0

        c.gridx = 4
        c.weightx = 0.0
        panel.add(Box.createHorizontalGlue(), c)

        return panel
    }


    fun initializeComboBoxes() {

        //处理结果双击复制和跳转
        searchResultTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                // 双击666
                if (e.clickCount == 2) {
                    val tree = e.source as? Tree ?: return

                    // 获取点击位置对应的 TreePath
                    val path = tree.getPathForLocation(e.x, e.y) ?: return

                    // 取得最后一个组件（即具体的节点）
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return

                    // 取出这个节点的 userObject
                    val userObject = node.userObject

                    if (userObject is MethodNode) {
                        val offset = userObject.sourceSpan
                        offset?.let {
                            OpenFileDescriptor(project, it.virtualFile, offset.offset).navigate(true)
                        }
                    }

                }
            }
        })


    }

    private fun createCenterPanel(): JComponent {
        // 初始化列表渲染器
        val methodRenderer = MethodListRenderer.createMethodRenderer()
        rootList.cellRenderer = methodRenderer
//        sinkList.cellRenderer = methodRenderer
//        searchResultList.cellRenderer = methodRenderer

        // 创建各个功能面板
        val rootPanel = createTitledPanel("Root Methods", JBScrollPane(rootList))
        val infoPanel = createTitledPanel("Info", JBScrollPane(statusPanel))
        val searchResultPanel = createTitledPanel("Search Results", JBScrollPane(searchResultTree))

        // 配置分割器
        val leftRightSplit = JBSplitter(false, 0.5f).apply {
            firstComponent = rootPanel
            secondComponent = infoPanel
            dividerWidth = 5
        }

        val verticalSplit = JBSplitter(true, 0.5f).apply {
            firstComponent = leftRightSplit
            secondComponent = searchResultPanel
            dividerWidth = 5
        }

        // 组合底部信息面板
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(verticalSplit, BorderLayout.CENTER)

            // 保持原有尺寸约束
            listOf(rootList, statusPanel, searchResultTree).forEach {
                it.minimumSize = Dimension(200, 100)
            }
            searchResultTree.minimumSize = Dimension(400, 100)
        }
    }

    private fun createTitledPanel(title: String, component: JComponent): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder(title)
            add(component, BorderLayout.CENTER)
        }
    }

    fun addIssueProblemsTabToProblemsView() {

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ProblemsView.ID) ?: return
        val contentManager = toolWindow.contentManager

        val issueTab = IssueProblemsTab(project)
        val content = ContentFactory.getInstance().createContent(issueTab.getComponent(), "Sink Finder", false)

        contentManager.addContent(content)

        if (!toolWindow.isVisible) {
            toolWindow.show()
        }

        contentManager.setSelectedContent(content)
    }

    fun updateInfoPanel(callGraph: CallGraph) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        graphStatusLabel.text = "CallGraph: ready"
        graphStatusLabel.icon = IconUtil.graphReadyIcon
        methodLabel.text = "MethodNode: ${callGraph.nodes.size}"
        methodLabel.icon = IconUtil.methodReadyIcon
        memoryLabel.icon = IconUtil.graphReadyIcon
        memoryLabel.text = "Used memory: $usedMemory MB"
        when (usedMemory) {
            in 0..2048 -> memoryLabel.icon = IconUtil.memoryLowIcon
            in 2048..4096 -> memoryLabel.icon = IconUtil.memoryMediumIcon
            else -> memoryLabel.icon = IconUtil.memoryHighIcon
        }
    }

    fun addBuildInfo(buildInfo: String) {
        buildInfoLabel.text = "Build info: $buildInfo"
    }

    fun addSearchInfo(searchInfo: String) {
        searchInfoLabel.text = "Search info: $searchInfo"
    }

    fun addErrorInfo(errorInfo: String) {
        errorInfoLabel.text = "Error: $errorInfo"
    }

}
