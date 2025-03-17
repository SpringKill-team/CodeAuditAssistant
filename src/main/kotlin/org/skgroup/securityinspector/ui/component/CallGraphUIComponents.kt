package org.skgroup.securityinspector.ui.component

import com.intellij.analysis.problemsView.toolWindow.ProblemsView
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.Gray
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import org.skgroup.securityinspector.analysis.ast.nodes.MethodNode
import org.skgroup.securityinspector.enums.AnalysisScope
import org.skgroup.securityinspector.enums.MainVulnerabilityType
import org.skgroup.securityinspector.enums.SubVulnerabilityType
import org.skgroup.securityinspector.utils.SinkList
import java.awt.*
import java.awt.event.ItemEvent
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
    val sinkListModel = DefaultListModel<MethodNode>()
    val sinkList = JBList(sinkListModel)
    val searchComboBox = ComboBox<AnalysisScope>(AnalysisScope.values())

    val searchResultRootNode = DefaultMutableTreeNode("Search Results")
    val searchResultTreeModel = DefaultTreeModel(searchResultRootNode)
    val searchResultTree = Tree(searchResultTreeModel).apply {
        isRootVisible = true
        showsRootHandles = true
    }
    val sourceField = JBTextField(15)
    val sinkField = JBTextField(15)
    val searchButton = JButton("Search")
    val infoArea = JBTextArea(5, 50).apply { isEditable = false }
    val vulnTypeComboBox = ComboBox<MainVulnerabilityType>()
    val subTypeComboBox = ComboBox<SubVulnerabilityType>()
    val sinkMethodList = SinkList.ALL_SUB_VUL_DEFINITIONS

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

    fun createSinkListMouseListener() = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 2 && !sinkList.isSelectionEmpty) {
                val selected = sinkList.selectedValue
                sinkField.text = selected.name
            }
        }
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
        panel.add(JLabel("VulnType:"), c)

        c.gridx = 1
        c.weightx = 0.0
        panel.add(vulnTypeComboBox, c)

        c.gridx = 2
        c.weightx = 0.0
        panel.add(JLabel("SubType:"), c)

        c.gridx = 3
        c.weightx = 0.0
        panel.add(subTypeComboBox, c)

        c.gridx = 4
        c.weightx = 0.0
        panel.add(Box.createHorizontalGlue(), c)

        return panel
    }


    fun initializeComboBoxes() {
        // 填充 MainVulnerabilityType
        MainVulnerabilityType.values().forEach { vulnTypeComboBox.addItem(it) }

        // 当选择了某个 MainVulnerabilityType 时，更新 subTypeComboBox 的内容
        vulnTypeComboBox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val selectedMainType = event.item as MainVulnerabilityType
                updateSubTypeComboBox(selectedMainType)
            }
        }

        // 监听 subTypeComboBox 的选项变化
        subTypeComboBox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val selectedSubType = event.item as SubVulnerabilityType
                updateSinkList(selectedSubType)
            }
        }

        // 默认先选中第一个 MainVulnerabilityType，并加载相应子类型
        if (vulnTypeComboBox.itemCount > 0) {
            vulnTypeComboBox.selectedIndex = 0
            updateSubTypeComboBox(vulnTypeComboBox.selectedItem as MainVulnerabilityType)
        }

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
                    // 假设这里存了一个 MethodNode或字符串
                    val userObject = node.userObject

                    if(userObject is MethodNode){
                        val offset = userObject.sourceSpan
                        // TODO 优化跳转到源码/Class的位置
                        offset?.let {
                            OpenFileDescriptor(project, it.virtualFile,offset.offset).navigate(true)
                            infoArea.append("Jump to ${it.virtualFile.name}:${it.offset}\n")
                        }
                    }

                }
            }
        })


    }

    fun updateSubTypeComboBox(mainType: MainVulnerabilityType) {
        subTypeComboBox.removeAllItems()
        // 根据父类型筛选可用的子类型
        SubVulnerabilityType.values().filter { it.parent == mainType }.forEach {
            subTypeComboBox.addItem(it)
        }

    }

    /**
     * Update sink list 展示对应的 sink 方法
     *
     * @param selectedSubType
     */
    private fun updateSinkList(selectedSubType: SubVulnerabilityType) {
        sinkListModel.removeAllElements()

        // 从 map 中拿到定义
        val definition = sinkMethodList.find { it.subType == selectedSubType }
        definition?.methodSinks?.forEach { (className, methods) ->
            methods.forEach { methodName ->
                val node = MethodNode(
                    className = className,
                    name = methodName,
                    returnType = "",
                    parameters = emptyList(),
                    body = emptyList()
                )
                sinkListModel.addElement(node)
            }
        }

        definition?.constructorSinks?.forEach { ctorClassName ->
            val name = "[Constructor] $ctorClassName"
            val node = MethodNode(
                className = name,
                name = "<init>",
                returnType = "",
                parameters = emptyList(),
                body = emptyList()
            )
            sinkListModel.addElement(node)
        }
    }

    private fun createCenterPanel(): JComponent {
        // 初始化列表渲染器
        val methodRenderer = MethodListRenderer.createMethodRenderer()
        rootList.cellRenderer = methodRenderer
        sinkList.cellRenderer = methodRenderer
//        searchResultList.cellRenderer = methodRenderer

        // 创建各个功能面板
        val rootPanel = createTitledPanel("Root Methods", JBScrollPane(rootList))
        val sinkPanel = createTitledPanel("Sink Methods", JBScrollPane(sinkList))
        val searchResultPanel = createTitledPanel("Search Results", JBScrollPane(searchResultTree))
        val infoPanel = createTitledPanel("Info", JBScrollPane(infoArea)).apply {
            preferredSize = Dimension(preferredSize.width, 150)
        }

        // 配置分割器
        val leftRightSplit = JBSplitter(false, 0.5f).apply {
            firstComponent = rootPanel
            secondComponent = sinkPanel
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
            add(infoPanel, BorderLayout.SOUTH)

            // 保持原有尺寸约束
            listOf(rootList, sinkList, searchResultTree).forEach {
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

}
