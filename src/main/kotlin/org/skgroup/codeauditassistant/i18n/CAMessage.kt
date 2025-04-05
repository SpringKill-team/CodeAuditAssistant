package org.skgroup.codeauditassistant.i18n

import com.intellij.AbstractBundle
import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.CodeAuditAssistant"

object CAMessage : AbstractBundle(BUNDLE) {
    private val bundle = DynamicBundle(CAMessage::class.java, BUNDLE)

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return bundle.getMessage(key, *params)
    }

    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): () -> String {
        val supplier = bundle.getLazyMessage(key, *params)
        return { supplier.get() }
    }
} 