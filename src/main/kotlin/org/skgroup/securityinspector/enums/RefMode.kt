package org.skgroup.securityinspector.enums

/**
 * 枚举描述：RefMode 枚举用于。
 *
 * @author springkill
 * @version 1.0
 */
enum class RefMode(val value: String) {
    CALL("call"),
    DECLARATION("declaration"),
    IMPLEMENTATION("implementation"),
    NEW("new"),
}
