package org.skgroup.securityinspector.enums

/**
 * 枚举描述：EdgeType 枚举用于。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/2/16
 */
enum class EdgeType {
    DIRECT_CALL,
    LAMBDA_CONTAINMENT,
    IOC_INJECTION,
    INTERFACE_IMPLEMENTATION
}
