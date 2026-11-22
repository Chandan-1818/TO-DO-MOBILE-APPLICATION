package com.chandan.master.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.auth.PinError
import com.chandan.master.domain.auth.PinValidator
import com.chandan.master.domain.auth.SessionLockManager
import com.chandan.master.domain.auth.UsernameError
import com.chandan.master.domain.auth.UsernameValidator
import com.chandan.master.domain.repository.AccountRepository
import com.chandan.master.domain.repository.AvatarRepository
import com.chandan.master.domain.repository.CreateAccountResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateAccountUiState(
    val username: String = "",
    val pin: String = "",
    val confirmPin: String = "",
    val avatarPath: String? = null,
    val showErrors: Boolean = false,
    val busy: Boolean = false,
    val storageFailed: Boolean = false,
    val avatarFailed: Boolean = false,
) {
    val usernameError: UsernameError? get() = UsernameValidator.validate(username)
    val pinError: PinError? get() = PinValidator.validate(pin)
    val confirmMismatch: Boolean get() = confirmPin.isNotEmpty() && !PinValidator.matches(pin, confirmPin) || (showErrors && confirmPin.isEmpty())
    val pinIsWeak: Boolean get() = PinValidator.isWeak(pin)
    val isValid: Boolean get() = usernameError == null && pinError == null && PinValidator.matches(pin, confirmPin)
}

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val avatars: AvatarRepository,
    private val lockManager: SessionLockManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateAccountUiState())
    val state: StateFlow<CreateAccountUiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v.take(40), storageFailed = false) }
    fun onPinChange(v: String) = _state.update { it.copy(pin = v, storageFailed = false) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPin = v, storageFailed = false) }

    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            val old = _state.value.avatarPath
            val path = avatars.importFromUri(uri)
            if (path == null) {
                _state.update { it.copy(avatarFailed = true) }
            } else {
                avatars.delete(old)
                _state.update { it.copy(avatarPath = path, avatarFailed = false) }
            }
        }
    }

    fun onRemovePhoto() {
        viewModelScope.launch {
            avatars.delete(_state.value.avatarPath)
            _state.update { it.copy(avatarPath = null, avatarFailed = false) }
        }
    }

    fun submit() {
        val s = _state.value
        if (s.busy) return
        if (!s.isValid) { _state.update { it.copy(showErrors = true) }; return }
        _state.update { it.copy(busy = true, storageFailed = false) }
        viewModelScope.launch {
            // Unlock first so the app opens straight to Home instead of flashing the lock screen.
            // This is safe: it only happens while no account exists, and failure re-locks.
            val noAccountYet = !accounts.hasAccount.first()
            if (noAccountYet) lockManager.unlock()
            val result = accounts.createAccount(s.username, s.pin, s.avatarPath)
            if (result != CreateAccountResult.Success) {
                lockManager.lock()
                _state.update { it.copy(busy = false, storageFailed = true, showErrors = true) }
            }
            // On success the auth state flips to Unlocked by itself and this screen leaves composition.
        }
    }
}
