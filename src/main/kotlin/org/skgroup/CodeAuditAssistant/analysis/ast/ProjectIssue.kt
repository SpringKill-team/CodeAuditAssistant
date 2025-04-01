package org.skgroup.CodeAuditAssistant.analysis.ast

import com.intellij.openapi.vfs.VirtualFile
import org.skgroup.CodeAuditAssistant.enums.SinkCallMode

/**
 * 类描述：ProjectIssue 类用于存储SinkFinder的跳转信息。
 *
 * @author springkill
 * @version 1.0
 */
data class ProjectIssue(
    val file: VirtualFile,
    val line: Int,
    val sinkClass: String,
    val sinkMethod: String,
    val type: String,
    val subType: String,
    val callMode: SinkCallMode
)
