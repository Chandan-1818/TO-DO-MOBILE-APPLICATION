package com.chandan.master.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.auth.AutoLockDelay
import com.chandan.master.domain.auth.SessionLockManager
import com.chandan.master.domain.repository.AccountRepository
import com.chandan.master.domain.usecase.ObserveAppPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AuthState {
    data object Loading : AuthState
    data object NoAccount : AuthState
    data object Locked : AuthState
    data object Unlocked : AuthState
}

/** Activity-scoped: decides what the whole app shows (create account, lock screen, or the app itself). */
@HiltViewModel
class AuthViewModel @Inject constructor(
    accountRepository: AccountRepository,
    observeProfile: com.chandan.master.domain.usecase.ObserveActiveProfileUseCase,
    private val lockManager: SessionLockManager,
    observePreferences: ObserveAppPreferencesUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthState> =
        combine(accountRepository.hasAccount, lockManager.isLocked) { hasAccount, locked ->
            when {
                !hasAccount -> AuthState.NoAccount
                locked -> AuthState.Locked
                else -> AuthState.Unlocked
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    /** Picture shown on the lock screen. */
    val avatarPath: StateFlow<String?> = observeProfile().map { it?.avatarUri }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val autoLockDelay: StateFlow<AutoLockDelay> =
        observePreferences().map { it.autoLockDelay }.stateIn(viewModelScope, SharingStarted.Eagerly, AutoLockDelay.Default)

    fun onAppStopped(elapsedRealtime: Long) = lockManager.onAppBackgrounded(elapsedRealtime)
    fun onAppStarted(elapsedRealtime: Long) = lockManager.onAppForegrounded(elapsedRealtime, autoLockDelay.value)
}
