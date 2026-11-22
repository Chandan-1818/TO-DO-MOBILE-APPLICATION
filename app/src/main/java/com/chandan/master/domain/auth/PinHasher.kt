package com.chandan.master.domain.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** A salted PBKDF2 verifier. Never contains the PIN. */
class PinRecord(val salt: ByteArray, val iterations: Int, val hash: ByteArray)

/**
 * PBKDF2-HMAC-SHA256 with a unique random 16-byte salt per PIN. The stored verifier is additionally
 * encrypted with a non-exportable Android Keystore key (see data/security), because a 4–6 digit PIN has
 * a tiny search space and must not be brute-forceable from a copied file.
 */
object PinHasher {
    const val DEFAULT_ITERATIONS = 210_000
    const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun create(pin: CharArray, random: SecureRandom = SecureRandom(), iterations: Int = DEFAULT_ITERATIONS): PinRecord {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        return PinRecord(salt, iterations, derive(pin, salt, iterations))
    }

    fun verify(pin: CharArray, record: PinRecord): Boolean {
        val candidate = derive(pin, record.salt, record.iterations)
        return MessageDigest.isEqual(candidate, record.hash) // constant-time comparison
    }

    private fun derive(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, HASH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
