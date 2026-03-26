package com.aidebugbridge.auth

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 authentication for securing the debug bridge.
 *
 * Supports two auth modes:
 * 1. HMAC signature: Client sends X-Timestamp + X-Signature headers
 * 2. Bearer token: Client sends pre-shared token in Authorization header
 *
 * The HMAC signature is computed as: HMAC-SHA256(secret, timestamp + ":" + bodyHash)
 * This prevents replay attacks (timestamp window) and body tampering.
 */
class HmacAuth(private val secret: String) {

    companion object {
        private const val ALGORITHM = "HmacSHA256"
        private const val MAX_TIMESTAMP_DRIFT_MS = 300_000L // 5 minutes
    }

    private val secretKeySpec = SecretKeySpec(secret.toByteArray(), ALGORITHM)

    /**
     * Verify an HMAC signature against the provided timestamp and body hash.
     *
     * @param timestamp Unix timestamp string from X-Timestamp header
     * @param bodyHash Hash of the request body from X-Body-Hash header
     * @param signature Base64-encoded HMAC signature from X-Signature header
     * @return true if the signature is valid and timestamp is within drift window
     */
    fun verify(timestamp: String, bodyHash: String, signature: String): Boolean {
        // Check timestamp freshness to prevent replay attacks
        val ts = timestamp.toLongOrNull() ?: return false
        val now = System.currentTimeMillis()
        if (kotlin.math.abs(now - ts) > MAX_TIMESTAMP_DRIFT_MS) {
            return false
        }

        val expected = sign("$timestamp:$bodyHash")
        return constantTimeEquals(expected, signature)
    }

    /**
     * Verify a bearer token. In simple mode, the token is just the secret itself.
     * For production use, derive tokens from the secret.
     */
    fun verifyToken(token: String): Boolean {
        return constantTimeEquals(secret, token)
    }

    /**
     * Compute HMAC-SHA256 signature for the given data.
     */
    fun sign(data: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(secretKeySpec)
        val bytes = mac.doFinal(data.toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray()
        val bBytes = b.toByteArray()
        if (aBytes.size != bBytes.size) return false

        var result = 0
        for (i in aBytes.indices) {
            result = result or (aBytes[i].toInt() xor bBytes[i].toInt())
        }
        return result == 0
    }
}
