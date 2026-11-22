package com.chandan.master.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.auth.AutoLockDelay
import com.chandan.master.domain.auth.BiometricCipherResult
import com.chandan.master.domain.auth.BiometricKeyProvider
import com.chandan.master.domain.auth.PinError
import com.chandan.master.domain.auth.PinValidator
import com.chandan.master.domain.auth.PinVerification
import com.chandan.master.domain.repository.AccountRepository
import com.chandan.master.domain.repository.ChangePinResult
import com.chandan.master.domain.usecase.ObserveAppPreferencesUseCase
import com.chandan.master.domain.usecase.SetAutoLockDelayUseCase
import com.chandan.master.domain.usecase.SetBiometricEnabledUseCase
import com.chandan.master.domain.auth.SessionLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PinChangeStatus {
    data object Idle : PinChangeStatus
    data object Success : PinChangeStatus
    data class WrongCurrent(val attemptsLeft: Int) : PinChangeStatus
    data class LockedOut(val seconds: Int) : PinChangeStatus
    data class InvalidNew(val error: PinError) : PinChangeStatus
    data object Mismatch : PinChangeStatus
    data object Unavailable : PinChangeStatus
    data object Failed : PinChangeStatus
}

enum class BiometricNotice { NONE, ENROLL_FAILED, CHANGED }

data class SecurityUiState(
    val biometricEnabled: Boolean = false,
    val autoLockDelay: AutoLockDelay = AutoLockDelay.Default,
    val pinStatus: PinChangeStatus = PinChangeStatus.Idle,
    val biometricNotice: BiometricNotice = BiometricNotice.NONE,
    val busy: Boolean = false,
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val biometricKeys: BiometricKeyProvider,
    private val lockManager: SessionLockManager,
    observePreferences: ObserveAppPreferencesUseCase,
    private val setBiometricEnabled: SetBiometricEnabledUseCase,
    private val setAutoLock: SetAutoLockDelayUseCase,
) : ViewModel() {

    private val local = MutableStateFlow(SecurityUiState())

    val uiState: StateFlow<SecurityUiState> = combine(local, observePreferences()) { l, prefs ->
        l.copy(biometricEnabled = prefs.biometricEnabled, autoLockDelay = prefs.autoLockDelay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecurityUiState())

    fun lockNow() = lockManager.lock()

    fun onAutoLockSelected(delay: AutoLockDelay) { viewModelScope.launch { setAutoLock(delay) } }

    /** Step 1 of enabling biometrics: a fresh key + cipher for the confirmation prompt (null if the key cannot be created). */
    fun prepareBiometricEnrollment(): Cipher? = when (val r = biometricKeys.prepareEnrollment()) {
        is BiometricCipherResult.Ready -> r.cipher
        BiometricCipherResult.Invalidated -> { local.update { it.copy(biometricNotice = BiometricNotice.ENROLL_FAILED) }; null }
    }

    /** Step 2: the user authenticated successfully, so biometric unlock may be switched on. */
    fun onBiometricEnrolled() {
        viewModelScope.launch { setBiometricEnabled(true); local.update { it.copy(biometricNotice = BiometricNotice.NONE) } }
    }

    fun onBiometricEnrollFailed(showNotice: Boolean) {
        biometricKeys.deleteKey()
        if (showNotice) local.update { it.copy(biometricNotice = BiometricNotice.ENROLL_FAILED) }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            biometricKeys.deleteKey()
            setBiometricEnabled(false)
            local.update { it.copy(biometricNotice = BiometricNotice.NONE) }
        }
    }

    fun changePin(current: String, new: String, confirm: String) {
        if (local.value.busy) return
        PinValidator.validate(new)?.let { e -> local.update { it.copy(pinStatus = PinChangeStatus.InvalidNew(e)) }; return }
        if (!PinValidator.matches(new, confirm)) { local.update { it.copy(pinStatus = PinChangeStatus.Mismatch) }; return }
        local.update { it.copy(busy = true, pinStatus = PinChangeStatus.Idle) }
        viewModelScope.launch {
            val status = when (val r = accounts.changePin(current, new)) {
                ChangePinResult.Success -> PinChangeStatus.Success
                is ChangePinResult.InvalidNewPin -> PinChangeStatus.InvalidNew(r.error)
                ChangePinResult.StorageFailure -> PinChangeStatus.Failed
                is ChangePinResult.CurrentPinRejected -> when (val why = r.reason) {
                    is PinVerification.Wrong -> if (why.lockedForMillis > 0) PinChangeStatus.LockedOut(ceil(why.lockedForMillis / 1000.0).toInt()) else PinChangeStatus.WrongCurrent(why.attemptsLeftBeforeDelay)
                    is PinVerification.LockedOut -> PinChangeStatus.LockedOut(ceil(why.retryAfterMillis / 1000.0).toInt())
                    PinVerification.CredentialsUnavailable -> PinChangeStatus.Unavailable
                    PinVerification.NoAccount, PinVerification.Success -> PinChangeStatus.Failed
                }
            }
            local.update { it.copy(busy = false, pinStatus = status) }
        }
    }

    fun clearPinStatus() = local.update { it.copy(pinStatus = PinChangeStatus.Idle) }

    fun resetAccount() { viewModelScope.launch { accounts.resetAccount() } }
}
