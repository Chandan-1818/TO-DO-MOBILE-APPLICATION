package com.chandan.master.domain.auth

import javax.crypto.Cipher

sealed interface BiometricCipherResult {
    /** A cipher bound to the biometric-protected key; it only becomes usable after BiometricPrompt succeeds. */
    data class Ready(val cipher: Cipher) : BiometricCipherResult
    /** The key was invalidated (new fingerprint/face enrolled) or lost. Biometric unlock must be turned off. */
    data object Invalidated : BiometricCipherResult
}

/** Android Keystore key that requires biometric authentication for every use. No biometric data is stored. */
interface BiometricKeyProvider {
    /** Creates a fresh key (replacing any old one) and returns a cipher for the enrolment confirmation prompt. */
    fun prepareEnrollment(): BiometricCipherResult
    fun cipherForUnlock(): BiometricCipherResult
    fun deleteKey()
}
