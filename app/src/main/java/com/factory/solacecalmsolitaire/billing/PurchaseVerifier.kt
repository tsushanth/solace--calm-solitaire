package com.factory.solacecalmsolitaire.billing

import android.util.Base64
import android.util.Log
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies that a purchase's signature matches the app's Play Console licensing key,
 * guarding against purchases spoofed by local tampering (e.g. Lucky Patcher-style tools).
 *
 * Real defense-in-depth also validates purchase tokens server-side against the Play
 * Developer API, which this offline check cannot substitute for.
 */
object PurchaseVerifier {
    private const val TAG = "PurchaseVerifier"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Base64-encoded RSA public key from Play Console > Monetization setup > Licensing.
     * Replace this placeholder before release — verification is skipped while it is blank.
     */
    private const val BASE64_PUBLIC_KEY = ""

    fun verifyPurchase(signedData: String, signature: String): Boolean {
        if (BASE64_PUBLIC_KEY.isBlank()) {
            Log.w(TAG, "No licensing public key configured; skipping signature verification.")
            return true
        }
        return try {
            val key = generatePublicKey(BASE64_PUBLIC_KEY)
            verify(key, signedData, signature)
        } catch (e: Exception) {
            Log.e(TAG, "Purchase verification failed", e)
            false
        }
    }

    @Throws(InvalidKeyException::class)
    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            KeyFactory.getInstance(KEY_FACTORY_ALGORITHM).generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            throw InvalidKeyException(e)
        }
    }

    @Throws(SignatureException::class)
    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes = try {
            Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            return false
        }
        return try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray())
            sig.verify(signatureBytes)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            false
        }
    }
}
