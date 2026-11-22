package com.chandan.master.domain.repository

import com.chandan.master.domain.auth.PinError
import com.chandan.master.domain.auth.PinVerification
import com.chandan.master.domain.auth.UsernameError
import kotlinx.coroutines.flow.Flow

sealed interface CreateAccountResult {
    data object Success : CreateAccountResult
    data class Invalid(val usernameError: UsernameError?, val pinError: PinError?) : CreateAccountResult
    data object AlreadyExists : CreateAccountResult
    data object StorageFailure : CreateAccountResult
}

sealed interface ChangePinResult {
    data object Success : ChangePinResult
    data class CurrentPinRejected(val reason: PinVerification) : ChangePinResult
    data class InvalidNewPin(val error: PinError) : ChangePinResult
    data object StorageFailure : ChangePinResult
}

/** Local-only account: username + PIN. There is no email, phone, OTP, social login or remote recovery. */
interface AccountRepository {
    val hasAccount: Flow<Boolean>
    val pinLength: Flow<Int?>
    suspend fun createAccount(username: String, pin: String, avatarPath: String?): CreateAccountResult
    suspend fun verifyPin(pin: String): PinVerification
    suspend fun lockoutRemainingMillis(): Long
    suspend fun changePin(currentPin: String, newPin: String): ChangePinResult
    /** Irreversibly deletes the profile, PIN, biometric key, pictures and all local data. */
    suspend fun resetAccount()
}
