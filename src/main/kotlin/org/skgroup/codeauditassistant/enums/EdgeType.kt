package org.skgroup.codeauditassistant.enums

/**
 * 枚举描述：EdgeType 枚举用于。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/2/16
 */
enum class EdgeType {
    CALL,
    IMPLEMENTS,
    DIRECT_CALL,
    LAMBDA_CONTAINMENT,
    IOC_INJECTION,
    INTERFACE_IMPLEMENTATION
}
