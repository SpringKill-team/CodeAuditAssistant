package org.skgroup.securityinspector.ui.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.skgroup.securityinspector.ui.SecurityInspectorMainToolWindow

/**
 * 类描述：SecurityInspectorProjectService 类用于存储实例，用于action进行调用。
 *
 * @author springkill
 * @version 1.0
 */
@Service(Service.Level.PROJECT)
class SecurityInspectorProjectService(project: Project) {
    var mainToolWindow: SecurityInspectorMainToolWindow? = null
}
