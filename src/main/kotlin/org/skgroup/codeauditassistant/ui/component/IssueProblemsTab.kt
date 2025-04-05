package org.skgroup.codeauditassistant.ui.component

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import org.skgroup.codeauditassistant.i18n.CAMessage
import org.skgroup.codeauditassistant.ui.renderer.FirstColumnRenderer
import org.skgroup.codeauditassistant.ui.renderer.HighlightRenderer
import org.skgroup.codeauditassistant.utils.SinkUtil.collectProjectIssues
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.JScrollPane
import java.util.TreeMap
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import java.io.Serializable

class IssueProblemsTab(private val project: Project) : ProblemsViewTab {

    private val panel = JBPanel<JBPanel<*>>(BorderLayout())

    private val tableModel =
        DefaultTableModel(
            arrayOf(
                CAMessage.message("sink.table.column.file"),
                CAMessage.message("sink.table.column.line"),
                CAMessage.message("sink.table.column.sink.class"),
                CAMessage.message("sink.table.column.sink.method"),
                CAMessage.message("sink.table.column.type"),
                CAMessage.message("sink.table.column.subtype"),
                CAMessage.message("sink.table.column.call.mode")
            ), 0
        )
    
    private val refreshButton = JButton(CAMessage.message("sink.button.init"))
    private val table = object : JBTable(tableModel) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    private val sorter = TableRowSorter(tableModel)
    
    private val rootNode = DefaultMutableTreeNode(CAMessage.message("sink.tree.root"))
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = JTree(treeModel)
    
    private val allIssues = mutableListOf<Array<out Any>>()
    
    private val typeMap = TreeMap<String, TreeMap<String, MutableList<Array<out Any>>>>()

    init {
        table.rowSorter = sorter
        table.apply {
            columnModel.getColumn(0).cellRenderer = FirstColumnRenderer()
            columnModel.getColumn(4).cellRenderer = HighlightRenderer()
            table.setAutoCreateRowSorter(true)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val selectedRow = table.selectedRow
                        if (selectedRow != -1) {
                            val modelRow = table.rowSorter.convertRowIndexToModel(selectedRow)
                            val file = tableModel.getValueAt(modelRow, 0) as VirtualFile
                            val line = tableModel.getValueAt(modelRow, 1).toString().toInt()

                            OpenFileDescriptor(project, file, line, 0).navigate(true)
                        }
                    }
                }
            })
        }

        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.addTreeSelectionListener(object : TreeSelectionListener {
            override fun valueChanged(e: TreeSelectionEvent) {
                val path = e.path
                filterTableByTreeSelection(path)
            }
        })

        refreshButton.apply {
            addActionListener {
                ProgressManager.getInstance().run(object : Task.Backgroundable(
                    project, 
                    CAMessage.message("sink.progress.finding"), 
                    true
                ) {
                    override fun run(indicator: ProgressIndicator) {
                        collectProjectIssues(project, callback = { issues ->
                            ApplicationManager.getApplication().invokeLater {
                                tableModel.rowCount = 0
                                allIssues.clear()
                                typeMap.clear()
                                rootNode.removeAllChildren()
                                
                                issues.forEach { issue ->
                                    val row = arrayOf<Any>(
                                        issue.file,
                                        issue.line.toString(),
                                        issue.sinkClass,
                                        issue.sinkMethod,
                                        issue.type,
                                        issue.subType,
                                        issue.callMode
                                    )
                                    allIssues.add(row)
                                    
                                    val type = issue.type
                                    val subType = issue.subType
                                    
                                    if (!typeMap.containsKey(type)) {
                                        typeMap[type] = TreeMap()
                                    }
                                    
                                    if (!typeMap[type]!!.containsKey(subType)) {
                                        typeMap[type]!![subType] = mutableListOf()
                                    }
                                    
                                    typeMap[type]!![subType]!!.add(row)
                                }
                                
                                buildTreeStructure()
                                
                                updateTableWithAllIssues()
                                
                                treeModel.reload()
                                sorter.sort()
                            }
                        })
                    }
                })
            }
        }

        val splitPanel = JBPanel<JBPanel<*>>(BorderLayout())
        splitPanel.add(JScrollPane(tree), BorderLayout.WEST)
        splitPanel.add(JBScrollPane(table), BorderLayout.CENTER)
        
        panel.add(splitPanel, BorderLayout.CENTER)
        panel.add(refreshButton, BorderLayout.SOUTH)
    }

    private fun buildTreeStructure() {
        rootNode.removeAllChildren()
        
        for ((type, subTypeMap) in typeMap) {
            val typeNode = DefaultMutableTreeNode(type)
            rootNode.add(typeNode)
            
            var totalCount = 0
            
            for ((subType, issues) in subTypeMap) {
                val count = issues.size
                totalCount += count
                val subTypeNode = DefaultMutableTreeNode("$subType ($count)")
                typeNode.add(subTypeNode)
            }
            
            typeNode.userObject = "$type ($totalCount)"
        }
    }
    
    private fun updateTableWithAllIssues() {
        tableModel.rowCount = 0
        for (row in allIssues) {
            tableModel.addRow(row as Array<*>)
        }
    }
    
    private fun filterTableByTreeSelection(path: TreePath) {
        tableModel.rowCount = 0
        
        when (path.pathCount) {
            2 -> {
                val typeStr = path.lastPathComponent.toString()
                val type = typeStr.substringBefore(" (")
                
                typeMap[type]?.forEach { (_, issues) ->
                    issues.forEach { row ->
                        tableModel.addRow(row as Array<*>)
                    }
                }
            }
            3 -> {
                val typeStr = path.getPathComponent(1).toString()
                val type = typeStr.substringBefore(" (")
                
                val subTypeStr = path.lastPathComponent.toString()
                val subType = subTypeStr.substringBefore(" (")
                
                typeMap[type]?.get(subType)?.forEach { row ->
                    tableModel.addRow(row as Array<*>)
                }
            }
            else -> {
                updateTableWithAllIssues()
            }
        }
    }

    fun getComponent() = panel

    override fun getName(count: Int): String {
        return CAMessage.message("tab.sink.finder")
    }

    override fun getTabId() = CAMessage.message("sink.tab.id")
}
