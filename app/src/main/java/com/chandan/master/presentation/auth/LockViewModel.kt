package com.chandan.master.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.auth.BiometricCipherResult
import com.chandan.master.domain.auth.BiometricKeyProvider
import com.chandan.master.domain.auth.PinVerification
import com.chandan.master.domain.auth.SessionLockManager
import com.chandan.master.domain.repository.AccountRepository
import com.chandan.master.domain.usecase.ObserveAppPreferencesUseCase
import com.chandan.master.domain.usecase.SetBiometricEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LockError {
    data class WrongPin(val attemptsLeft: Int) : LockError
    data object TooManyAttempts : LockError
    data object BiometricChanged : LockError
    data object BiometricLockedOut : LockError
    data object CredentialsUnavailable : LockError
}

data class LockUiState(
    /** 0 until the stored PIN length has been read; input is ignored until then. */
    val pinLength: Int = 0,
    val enteredCount: Int = 0,
    val error: LockError? = null,
    val lockedSecondsLeft: Int = 0,
    val busy: Boolean = false,
    val biometricEnabled: Boolean = false,
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val lockManager: SessionLockManager,
    private val biometricKeys: BiometricKeyProvider,
    private val setBiometricEnabled: SetBiometricEnabledUseCase,
    observePreferences: ObserveAppPreferencesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    private var pin = ""
    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            val length = accounts.pinLength.first()
            if (length == null) _state.update { it.copy(error = LockError.CredentialsUnavailable) }
            else _state.update { it.copy(pinLength = length) }
        }
        viewModelScope.launch { observePreferences().collect { p -> _state.update { it.copy(biometricEnabled = p.biometricEnabled) } } }
        viewModelScope.launch { accounts.lockoutRemainingMillis().takeIf { it > 0 }?.let { startCountdown(it) } }
    }

    fun onDigit(d: Char) {
        val s = _state.value
        if (s.pinLength == 0 || s.busy || s.lockedSecondsLeft > 0 || pin.length >= s.pinLength) return
        pin += d
        _state.update { it.copy(enteredCount = pin.length, error = if (it.error is LockError.WrongPin) null else it.error) }
        if (pin.length == s.pinLength) submit()
    }

    fun onDelete() {
        if (_state.value.busy || pin.isEmpty()) return
        pin = pin.dropLast(1)
        _state.update { it.copy(enteredCount = pin.length) }
    }

    private fun submit() {
        val attempt = pin
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = accounts.verifyPin(attempt)
            pin = ""
            _state.update { it.copy(busy = false, enteredCount = 0) }
            when (result) {
                PinVerification.Success -> lockManager.unlock()
                is PinVerification.Wrong ->
                    if (result.lockedForMillis > 0) startCountdown(result.lockedForMillis)
                    else _state.update { it.copy(error = LockError.WrongPin(result.attemptsLeftBeforeDelay)) }
                is PinVerification.LockedOut -> startCountdown(result.retryAfterMillis)
                PinVerification.CredentialsUnavailable -> _state.update { it.copy(error = LockError.CredentialsUnavailable) }
                PinVerification.NoAccount -> Unit // AuthViewModel will switch to the create-account flow.
            }
        }
    }

    private fun startCountdown(millis: Long) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            var left = millis
            while (left > 0) {
                _state.update { it.copy(lockedSecondsLeft = ceil(left / 1000.0).toInt(), error = LockError.TooManyAttempts) }
                delay(1_000)
                left -= 1_000
            }
            _state.update { it.copy(lockedSecondsLeft = 0, error = null) }
        }
    }

    /** Returns a cipher for BiometricPrompt, or null if biometric unlock is no longer valid (then it is switched off). */
    fun biometricCipher(): Cipher? = when (val r = biometricKeys.cipherForUnlock()) {
        is BiometricCipherResult.Ready -> r.cipher
        BiometricCipherResult.Invalidated -> {
            viewModelScope.launch {
                biometricKeys.deleteKey()
                setBiometricEnabled(false)
                _state.update { it.copy(error = LockError.BiometricChanged) }
            }
            null
        }
    }

    fun onBiometricSuccess() = lockManager.unlock()

    fun onBiometricFailure(failure: BiometricFailure) {
        when (failure) {
            BiometricFailure.LOCKOUT, BiometricFailure.LOCKOUT_PERMANENT -> _state.update { it.copy(error = LockError.BiometricLockedOut) }
            else -> Unit // cancelled or unavailable: the PIN pad is always available as the fallback
        }
    }

    fun resetAccount() {
        viewModelScope.launch { accounts.resetAccount() }
    }
}
