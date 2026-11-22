package com.chandan.master.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.chandan.master.domain.auth.BiometricCipherResult
import com.chandan.master.domain.auth.BiometricKeyProvider
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBiometricKeyProvider @Inject constructor() : BiometricKeyProvider {

    override fun prepareEnrollment(): BiometricCipherResult {
        deleteKey()
        return try {
            createKey()
            cipher(requireNotNull(existingKey()))
        } catch (e: Exception) {
            BiometricCipherResult.Invalidated
        }
    }

    override fun cipherForUnlock(): BiometricCipherResult {
        return try {
            val key = existingKey() ?: return BiometricCipherResult.Invalidated
            cipher(key)
        } catch (e: Exception) { // includes KeyPermanentlyInvalidatedException (biometric set changed)
            BiometricCipherResult.Invalidated
        }
    }

    override fun deleteKey() {
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private fun cipher(key: SecretKey): BiometricCipherResult.Ready {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        return BiometricCipherResult.Ready(c)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
    private fun existingKey(): SecretKey? = keyStore().getKey(ALIAS, null) as? SecretKey

    private fun createKey() {
        val builder = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG) // auth on every use
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1) // auth on every use
        }
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).apply { init(builder.build()) }.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "master_biometric_unlock_key"
    }
}
