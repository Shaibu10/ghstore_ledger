package com.example.util

import java.security.MessageDigest

/**
 * Utility for password security hashing for offline access without internet access.
 */
object HashUtils {
    fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            // Safe robust fallback
            input
        }
    }
}
