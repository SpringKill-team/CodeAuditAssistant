package org.skgroup.codeauditassistant.utils

import com.intellij.openapi.util.IconLoader

object IconUtil {

    fun loadIcon(path: String) = IconLoader.getIcon(path, javaClass)

    val windowsIcon = loadIcon("/icons/windows.svg")
    val linuxIcon = loadIcon("/icons/linux.svg")
    val macIcon = loadIcon("/icons/mac-os.svg")

    val platformIcon = loadIcon("/icons/platform-white.svg")
    val methodNotReadyIcon = loadIcon("/icons/methods-white.svg")
    val methodReadyIcon = loadIcon("/icons/methods-purple.svg")
    val graphNotReadyIcon = loadIcon("/icons/callgraph-white.svg")
    val graphReadyIcon = loadIcon("/icons/callgraph-purple.svg")
    val memoryLowIcon = loadIcon("/icons/memory-low.svg")
    val memoryMediumIcon = loadIcon("/icons/memory-mid.svg")
    val memoryHighIcon = loadIcon("/icons/memory-high.svg")
    val normalIcon = loadIcon("/icons/normal-info.svg")
    val errorIcon = loadIcon("/icons/error-info.svg")

    val pathIcon = loadIcon("/icons/path-white.svg")
    val callIcon = loadIcon("/icons/call-white.svg")
    val newIcon = loadIcon("/icons/new-white.svg")
    val declarationIcon = loadIcon("/icons/declare-white.svg")
}