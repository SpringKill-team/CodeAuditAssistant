package org.skgroup.securityinspector.analysis.ast

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

/**
 * SourceSpan 是表示源码位置的数据结构
 * @property containingFile 源码所在的文件
 * @property virtualFile    源码所在的虚拟文件
 * @property offset         源码在文件中的偏移量
 * @constructor Create empty Source span
 */
data class SourceSpan(
    val containingFile: PsiFile,
    val virtualFile: VirtualFile,
    val offset : Int
)
