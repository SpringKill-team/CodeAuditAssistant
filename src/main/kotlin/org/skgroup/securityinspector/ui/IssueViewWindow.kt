package org.skgroup.securityinspector.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import org.skgroup.securityinspector.ui.component.FirstColumnRenderer
import org.skgroup.securityinspector.ui.component.HighlightRenderer
import org.skgroup.securityinspector.utils.GraphUtils.collectProjectIssues
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

/**
 * 类描述：IssueViewWindow 类用于创建SinkFinder。
 *
 * @author springkill
 * @version 1.0
 */
class IssueViewWindow : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        val tableModel =
            DefaultTableModel(arrayOf("File", "Line", "SinkClass", "SinkMethod", "Type", "SubType", "CallMode"), 0)
        val refreshButton = JButton("Init Sink")
        val table = object : JBTable(tableModel) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }

        val sorter = TableRowSorter(tableModel)
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

        refreshButton.apply {
            addActionListener {
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Finding sink method", true) {
                    override fun run(indicator: ProgressIndicator) {
                        collectProjectIssues(project, callback = { issues ->

                            ApplicationManager.getApplication().invokeLater {
                                tableModel.rowCount = 0
                                issues.forEach { issue ->
                                    tableModel.addRow(
                                        arrayOf(
                                            issue.file,
                                            issue.line.toString(),
                                            issue.sinkClass,
                                            issue.sinkMethod,
                                            issue.type,
                                            issue.subType,
                                            issue.callMode
                                        )
                                    )
                                }
                                sorter.sort()
                            }
                        })
                    }
                })
            }
        }

        panel.add(JBScrollPane(table), BorderLayout.CENTER)
        panel.add(refreshButton, BorderLayout.SOUTH)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

}
