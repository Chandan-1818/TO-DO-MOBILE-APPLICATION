package com.chandan.master.data.repository

import com.chandan.master.data.local.preferences.PreferencesDataSource
import com.chandan.master.domain.auth.AutoLockDelay
import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import com.chandan.master.domain.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataSource: PreferencesDataSource,
) : PreferencesRepository {
    override val preferences: Flow<AppPreferences> = dataSource.preferences
    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode)
    override suspend fun setAccentPalette(palette: AccentPalette) = dataSource.setAccentPalette(palette)
    override suspend fun setAutoLockDelay(delay: AutoLockDelay) = dataSource.setAutoLockDelay(delay)
    override suspend fun setBiometricEnabled(enabled: Boolean) = dataSource.setBiometricEnabled(enabled)
    override suspend fun setNotificationsEnabled(enabled: Boolean) = dataSource.setNotificationsEnabled(enabled)
    override suspend fun setTaskReminders(enabled: Boolean) = dataSource.setTaskReminders(enabled)
    override suspend fun setHabitReminders(enabled: Boolean) = dataSource.setHabitReminders(enabled)
    override suspend fun setFocusNotifications(enabled: Boolean) = dataSource.setFocusNotifications(enabled)
    override suspend fun setFocusSettings(settings: com.chandan.master.domain.logic.FocusSettings) = dataSource.setFocusSettings(settings)
}
