package com.chandan.master.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import com.chandan.master.domain.usecase.ObserveActiveProfileUseCase
import com.chandan.master.domain.usecase.ObserveAppPreferencesUseCase
import com.chandan.master.domain.usecase.SetAccentPaletteUseCase
import com.chandan.master.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    /** Null means no local account exists yet (guest). */
    val username: String? = null,
    val avatarPath: String? = null,
    val preferences: AppPreferences = AppPreferences(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeProfile: ObserveActiveProfileUseCase,
    observePreferences: ObserveAppPreferencesUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAccentPalette: SetAccentPaletteUseCase,
    private val lockManager: com.chandan.master.domain.auth.SessionLockManager,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> =
        combine(observeProfile(), observePreferences()) { profile, prefs ->
            ProfileUiState(username = profile?.username, avatarPath = profile?.avatarUri, preferences = prefs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun lockNow() = lockManager.lock()

    fun onAccentSelected(palette: AccentPalette) {
        viewModelScope.launch { setAccentPalette(palette) }
    }
}
