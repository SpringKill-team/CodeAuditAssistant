package org.skgroup.securityinspector.utils

import com.intellij.openapi.project.Project
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.jar.JarFile

class Decompiler(private val project: Project) {

    private fun getMavenDependencies(): List<File> {
        val mavenJars = mutableListOf<File>()
        val projectBaseDir = project.basePath ?: return emptyList()

        val pomFile = File("$projectBaseDir/pom.xml")
        if (!pomFile.exists()) {
            println("pom.xml does not exist")
            return emptyList()
        }

        val mavenReader = MavenXpp3Reader()
        val model: Model = FileReader(pomFile).use { reader ->
            mavenReader.read(reader)
        }

        val dependencies1 = model.dependencies

        val dependencies2 = model.dependencyManagement.dependencies

        val dependencies = dependencies1 + dependencies2

        dependencies.forEach { dependency ->
            if(dependency.version != null) {
                //直接用version的时候拼接的是${}，解析实际的值替换才能获取真正的jar
                dependency.version = model.properties.getProperty(dependency.version.replace("\${", "").replace("}", ""))
                val jarFile = resolveJarFileFromDependency(dependency)
                if (jarFile != null && jarFile.exists()) {
                    mavenJars.add(jarFile)
                }
            }
        }

        return mavenJars
    }

    fun getAllJars(): List<File> {
        val projectBaseDir = project.basePath ?: return emptyList()
        val libDir = File("$projectBaseDir/lib")

        val libJars = libDir.listFiles { _, name -> name.endsWith(".jar") }?.toList() ?: emptyList()

        val mavenJars = getMavenDependencies()

        println("JAR files in lib directory: ${libJars.map { it.name }}")
        println("Maven JAR files: ${mavenJars.map { it.name }}")

        return libJars + mavenJars
    }

    fun decompileJar(jarFile: File) {
        val outputDir = File("${project.basePath}/decompiled")
        outputDir.mkdirs()

        val options = mapOf(
            IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES to 1,
            IFernflowerPreferences.REMOVE_EMPTY_RANGES to 0,
            IFernflowerPreferences.FINALLY_DEINLINE to 0,
            IFernflowerPreferences.BYTECODE_SOURCE_MAPPING to 1,
            IFernflowerPreferences.LOG_LEVEL to "INFO"
        )

        val decompiler = ConsoleDecompiler(outputDir, options)

        decompiler.addSpace(jarFile, true)
        decompiler.decompileContext()
        val decompiledJarFile = File(outputDir, jarFile.name)
        extractJarFile(decompiledJarFile, outputDir)
    }

    private fun resolveJarFileFromDependency(dependency: Dependency): File? {
        val mavenRepo = File(System.getProperty("user.home") + "/.m2/repository")

        val groupPath = dependency.groupId.replace('.', '/')
        val artifactId = dependency.artifactId
        val version = dependency.version


        val jarPath = "$groupPath/$artifactId/$version/$artifactId-$version.jar"

        return File(mavenRepo, jarPath)
    }

    fun extractJarFile(jarFile: File, outputDir: File) {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val jarDir = File(outputDir, jarFile.name.split(".jar")[0])
        jarDir.mkdirs()

        val mavenDir = File(jarDir, "src/main/java")
        mavenDir.mkdirs()

        JarFile(jarFile).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                val entryFileName = File(entry.name).name

                val outputFile = if (entryFileName == "pom.xml" || entryFileName == "pom.properties") {
                    File(jarDir, entryFileName)
                } else {
                    File(mavenDir, entry.name)
                }

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()

                    jar.getInputStream(entry).use { inputStream ->
                        FileOutputStream(outputFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        }
    }
}
