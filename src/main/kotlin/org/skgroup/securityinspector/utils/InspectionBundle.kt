package com.skgroup.securityinspector.utils

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

// 顶层定义 BUNDLE 常量
@NonNls
const val BUNDLE = "Inspection"+"Bundle"

object InspectionBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}
