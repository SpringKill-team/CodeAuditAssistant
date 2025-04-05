package org.skgroup.codeauditassistant.utils

import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.*

/**
 * 类描述：RSAUtil 类用于。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/4/1
 */
object RSAUtil {
    private val publicKeyPath = "/key/public.der"
    private var cachedPublicKey: PublicKey? = null

    init {
        cachedPublicKey = loadPublicKey()
    }

    fun getPublicKey(): PublicKey {
        return cachedPublicKey ?: loadPublicKey()
    }

    fun loadPublicKey(): PublicKey {
        return loadKeyFromResource(publicKeyPath) { bytes ->
            KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(bytes))
        }.also { cachedPublicKey = it }
    }

    private fun <K : Key> loadKeyFromResource(
        path: String,
        keySpecConverter: (ByteArray) -> K,
    ): K {
        try {
            val inputStream = checkNotNull(javaClass.getResourceAsStream(path)) {
                "Could not find $path"
            }
            return keySpecConverter(inputStream.readAllBytes())
        } catch (e: Exception) {
            throw RuntimeException("load Fail $path", e)
        }
    }

    fun verifySignature(data: ByteArray, signature: String, algorithm: String = "SHA256withRSA"): Boolean {
        try {
            val sig = Signature.getInstance(algorithm)
            sig.initVerify(getPublicKey())
            sig.update(data)
            return sig.verify(Base64.getDecoder().decode(signature))
        } catch (e: Exception) {
            throw RuntimeException("Signature verification failed", e)
        }
    }

    /**
     * Convenience method to verify a signature against String data.
     */
    fun verifySignature(data: String, signature: String, algorithm: String = "SHA256withRSA"): Boolean {
        return verifySignature(data.toByteArray(Charsets.UTF_8), signature, algorithm)
    }
}
