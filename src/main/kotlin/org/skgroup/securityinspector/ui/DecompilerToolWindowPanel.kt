package org.skgroup.securityinspector.ui

import com.intellij.openapi.project.Project
import org.skgroup.securityinspector.utils.Decompiler
import javax.swing.*
import java.awt.BorderLayout
import java.io.File

class DecompilerToolWindowPanel(private val project: Project) {

    val mainPanel = JPanel(BorderLayout())

    private val jarList = mutableListOf<File>()
    private val checkBoxList = mutableListOf<JCheckBox>()

    private val listPanel = JPanel()
    private val progressBar = JProgressBar(0, 100)
    private val runButton = JButton("Run")

    init {
        initializeUI()
        runButton.addActionListener { runDecompiler() }
    }

    private fun initializeUI() {
        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)

        jarList.addAll(findJarFiles())
        jarList.forEach { file ->
            val checkBox = JCheckBox(file.name)
            checkBoxList.add(checkBox)
            listPanel.add(checkBox)
        }

        mainPanel.add(JScrollPane(listPanel), BorderLayout.CENTER)
        mainPanel.add(progressBar, BorderLayout.SOUTH)
        mainPanel.add(runButton, BorderLayout.NORTH)
    }

    private fun findJarFiles(): List<File> {
        val decompiler = Decompiler(project)
        return decompiler.getAllJars()
    }

    private fun runDecompiler() {
        val selectedJars = checkBoxList
            .filter { it.isSelected }
            .map { jarCheckBox -> jarList.find { it.name == jarCheckBox.text }!! }

        val task = object : SwingWorker<Unit, Int>() {
            override fun doInBackground() {
                val decompiler = Decompiler(project)

                selectedJars.forEachIndexed { index, jar ->
                    decompiler.decompileJar(jar)
                    val progress = ((index + 1) * 100) / selectedJars.size
                    publish(progress)
                }
            }

            override fun process(chunks: List<Int>) {
                progressBar.value = chunks.last()
            }

            override fun done() {
                progressBar.value = 100
            }
        }

        try {
            task.execute()
        } catch (e: Exception) {
            println("execute task failed: $e")
        }
    }
}
