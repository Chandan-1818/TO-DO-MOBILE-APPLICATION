package com.chandan.master.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

class CredentialsUnreadableException(cause: Throwable? = null) : Exception("Stored credentials cannot be read", cause)

/**
 * AES-256-GCM using a non-exportable Android Keystore key. It wraps the PIN verifier so that copying the
 * credentials file off the device (or from a backup) is useless without this device's Keystore.
 * It does NOT require user authentication (the PIN screen is the authentication).
 */
@Singleton
class KeystoreCrypto @Inject constructor() {
    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun key(): SecretKey {
        (keyStore().getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /** Returns IV (12 bytes) followed by ciphertext+tag. */
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray): ByteArray = try {
        require(data.size > IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, data.copyOfRange(0, IV_BYTES)))
        cipher.doFinal(data, IV_BYTES, data.size - IV_BYTES)
    } catch (e: Exception) {
        throw CredentialsUnreadableException(e)
    }

    fun deleteKey() {
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "master_credentials_wrap_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
