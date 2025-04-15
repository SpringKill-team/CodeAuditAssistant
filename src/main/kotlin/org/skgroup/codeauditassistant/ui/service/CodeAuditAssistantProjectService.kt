package org.skgroup.codeauditassistant.ui.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.skgroup.codeauditassistant.ui.CodeAuditAssistantMainToolWindow

/**
 * 类描述：CodeAuditAssistantProjectService 类用于存储实例，用于action进行调用。
 *
 * @author springkill
 * @version 1.0
 */
@Service(Service.Level.PROJECT)
class CodeAuditAssistantProjectService(project: Project) {
    var mainToolWindow: CodeAuditAssistantMainToolWindow? = null
}
