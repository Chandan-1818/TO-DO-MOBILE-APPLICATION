package com.chandan.master.domain.usecase

import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import com.chandan.master.domain.repository.PreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAppPreferencesUseCase @Inject constructor(private val repo: PreferencesRepository) {
    operator fun invoke(): Flow<AppPreferences> = repo.preferences
}

class SetThemeModeUseCase @Inject constructor(private val repo: PreferencesRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repo.setThemeMode(mode)
}

class SetAccentPaletteUseCase @Inject constructor(private val repo: PreferencesRepository) {
    suspend operator fun invoke(palette: AccentPalette) = repo.setAccentPalette(palette)
}

class SetAutoLockDelayUseCase @Inject constructor(private val repo: PreferencesRepository) {
    suspend operator fun invoke(delay: com.chandan.master.domain.auth.AutoLockDelay) = repo.setAutoLockDelay(delay)
}

class SetBiometricEnabledUseCase @Inject constructor(private val repo: PreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) = repo.setBiometricEnabled(enabled)
}
