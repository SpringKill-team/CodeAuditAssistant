package org.skgroup.CodeAuditAssistant.enums

/**
 * 类描述：AnalysisScope 类用于 指定构建范围
 *
 * @author springkill
 * @version 1.0
 */
enum class AnalysisScope(private val displayName: String) {

    ENTIRE_PROJECT("Entire Project"),
    MODULE("Selected Module");
    // OPEN_FILE("Open Files");

    override fun toString(): String {
        return displayName
    }

}
