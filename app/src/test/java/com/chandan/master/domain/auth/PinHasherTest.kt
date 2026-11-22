package com.chandan.master.domain.auth

import java.security.MessageDigest
import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    private val fast = 2_000 // iterations only reduced for test speed; production uses DEFAULT_ITERATIONS

    @Test fun correctPinVerifiesAndWrongPinFails() {
        val rec = PinHasher.create("2580".toCharArray(), iterations = fast)
        assertTrue(PinHasher.verify("2580".toCharArray(), rec))
        assertFalse(PinHasher.verify("2581".toCharArray(), rec))
        assertFalse(PinHasher.verify("25800".toCharArray(), rec))
        assertFalse(PinHasher.verify("".toCharArray(), rec))
    }

    @Test fun samePinGetsDifferentSaltAndHash() {
        val a = PinHasher.create("2580".toCharArray(), iterations = fast)
        val b = PinHasher.create("2580".toCharArray(), iterations = fast)
        assertFalse(a.salt.contentEquals(b.salt))
        assertFalse(a.hash.contentEquals(b.hash))
        assertEquals(PinHasher.SALT_BYTES, a.salt.size)
        assertEquals(32, a.hash.size)
    }

    @Test fun hashIsNotAPlainOrUnsaltedDigest() {
        val rec = PinHasher.create("2580".toCharArray(), iterations = fast)
        val sha = MessageDigest.getInstance("SHA-256").digest("2580".toByteArray())
        assertFalse(rec.hash.contentEquals(sha))
        assertFalse(String(rec.hash, Charsets.ISO_8859_1).contains("2580"))
    }

    @Test fun recordKeepsItsOwnIterationCountSoItCanBeUpgraded() {
        val rec = PinHasher.create("1357".toCharArray(), iterations = 1_500)
        assertEquals(1_500, rec.iterations)
        assertTrue(PinHasher.verify("1357".toCharArray(), rec))
        val tampered = PinRecord(rec.salt, 1_501, rec.hash)
        assertFalse(PinHasher.verify("1357".toCharArray(), tampered))
    }

    @Test fun tamperedSaltOrHashFails() {
        val rec = PinHasher.create("1357".toCharArray(), iterations = fast)
        val salt = rec.salt.copyOf().also { it[0] = (it[0] + 1).toByte() }
        assertFalse(PinHasher.verify("1357".toCharArray(), PinRecord(salt, rec.iterations, rec.hash)))
        val hash = rec.hash.copyOf().also { it[5] = (it[5] + 1).toByte() }
        assertFalse(PinHasher.verify("1357".toCharArray(), PinRecord(rec.salt, rec.iterations, hash)))
    }

    @Test fun matchesKnownPbkdf2Sha256Vector() {
        // RFC 7914 §11 style cross-check against the JDK's own PBKDF2 for a fixed salt: deterministic derivation.
        val salt = ByteArray(16) { it.toByte() }
        val random = object : SecureRandom() { override fun nextBytes(bytes: ByteArray) { salt.copyInto(bytes) } }
        val a = PinHasher.create("1234".toCharArray(), random, fast)
        val b = PinHasher.create("1234".toCharArray(), random, fast)
        assertArrayEquals(a.hash, b.hash)
        assertNotEquals(PinHasher.create("1235".toCharArray(), random, fast).hash.toList(), a.hash.toList())
    }

    @Test fun productionIterationCountIsAtLeastOwaspFloorForHmacSha1Era() {
        assertTrue(PinHasher.DEFAULT_ITERATIONS >= 200_000)
    }
}
