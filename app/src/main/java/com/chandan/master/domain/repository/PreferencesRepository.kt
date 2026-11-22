package com.chandan.master.domain.repository

import com.chandan.master.domain.auth.AutoLockDelay
import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentPalette(palette: AccentPalette)
    suspend fun setAutoLockDelay(delay: AutoLockDelay)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setTaskReminders(enabled: Boolean)
    suspend fun setHabitReminders(enabled: Boolean)
    suspend fun setFocusNotifications(enabled: Boolean)
    suspend fun setFocusSettings(settings: com.chandan.master.domain.logic.FocusSettings)
}
