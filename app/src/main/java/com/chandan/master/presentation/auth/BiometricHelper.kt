package com.chandan.master.presentation.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

enum class BiometricAvailability { AVAILABLE, NO_HARDWARE, NONE_ENROLLED, TEMPORARILY_UNAVAILABLE, UNSUPPORTED }
enum class BiometricFailure { CANCELED, LOCKOUT, LOCKOUT_PERMANENT, HARDWARE_UNAVAILABLE, OTHER }

/**
 * Thin wrapper over AndroidX BiometricPrompt. Uses a CryptoObject bound to a Keystore key that requires
 * biometric authentication, so success is proven cryptographically, not just by a callback.
 * Master never sees or stores biometric data; Android only reports success or failure.
 */
object BiometricHelper {
    private val CHALLENGE = "master-unlock".toByteArray()

    fun availability(context: Context): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.TEMPORARILY_UNAVAILABLE
            else -> BiometricAvailability.UNSUPPORTED
        }

    fun authenticate(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onFailure: (BiometricFailure) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val proven = try {
                        result.cryptoObject?.cipher?.doFinal(CHALLENGE) != null
                    } catch (e: Exception) {
                        false
                    }
                    if (proven) onSuccess() else onFailure(BiometricFailure.OTHER)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure(
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> BiometricFailure.CANCELED
                            BiometricPrompt.ERROR_LOCKOUT -> BiometricFailure.LOCKOUT
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricFailure.LOCKOUT_PERMANENT
                            BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_HW_NOT_PRESENT,
                            BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricFailure.HARDWARE_UNAVAILABLE
                            else -> BiometricFailure.OTHER
                        },
                    )
                }
                // onAuthenticationFailed (unrecognised finger/face): the system prompt stays open and lets the user retry.
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}
