package com.chandan.master.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chandan.master.di.SettingsStore
import com.chandan.master.domain.auth.AutoLockDelay
import com.chandan.master.domain.logic.FocusSettings
import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class PreferencesDataSource @Inject constructor(
    @SettingsStore private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppPreferences(
                themeMode = ThemeMode.fromStorageValue(prefs[Keys.THEME_MODE]),
                accentPalette = AccentPalette.fromStorageValue(prefs[Keys.ACCENT_PALETTE]),
                autoLockDelay = AutoLockDelay.fromStorageValue(prefs[Keys.AUTO_LOCK]),
                biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
                taskReminders = prefs[Keys.TASK_REMINDERS] ?: true,
                habitReminders = prefs[Keys.HABIT_REMINDERS] ?: true,
                focusNotifications = prefs[Keys.FOCUS_NOTIFICATIONS] ?: true,
                focusSettings = FocusSettings(
                    focusMinutes = (prefs[Keys.FOCUS_MIN] ?: 25).coerceIn(FocusSettings.MINUTES_RANGE),
                    shortBreakMinutes = (prefs[Keys.SHORT_MIN] ?: 5).coerceIn(FocusSettings.MINUTES_RANGE),
                    longBreakMinutes = (prefs[Keys.LONG_MIN] ?: 15).coerceIn(FocusSettings.MINUTES_RANGE),
                ),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) { dataStore.edit { it[Keys.THEME_MODE] = mode.storageValue } }
    suspend fun setAccentPalette(palette: AccentPalette) { dataStore.edit { it[Keys.ACCENT_PALETTE] = palette.storageValue } }
    suspend fun setAutoLockDelay(delay: AutoLockDelay) { dataStore.edit { it[Keys.AUTO_LOCK] = delay.storageValue } }
    suspend fun setBiometricEnabled(enabled: Boolean) { dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled } }

    suspend fun setNotificationsEnabled(v: Boolean) { dataStore.edit { it[Keys.NOTIFICATIONS] = v } }
    suspend fun setTaskReminders(v: Boolean) { dataStore.edit { it[Keys.TASK_REMINDERS] = v } }
    suspend fun setHabitReminders(v: Boolean) { dataStore.edit { it[Keys.HABIT_REMINDERS] = v } }
    suspend fun setFocusNotifications(v: Boolean) { dataStore.edit { it[Keys.FOCUS_NOTIFICATIONS] = v } }
    suspend fun setFocusSettings(s: FocusSettings) {
        dataStore.edit {
            it[Keys.FOCUS_MIN] = s.focusMinutes.coerceIn(FocusSettings.MINUTES_RANGE)
            it[Keys.SHORT_MIN] = s.shortBreakMinutes.coerceIn(FocusSettings.MINUTES_RANGE)
            it[Keys.LONG_MIN] = s.longBreakMinutes.coerceIn(FocusSettings.MINUTES_RANGE)
        }
    }

    private object Keys {
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val TASK_REMINDERS = booleanPreferencesKey("task_reminders")
        val HABIT_REMINDERS = booleanPreferencesKey("habit_reminders")
        val FOCUS_NOTIFICATIONS = booleanPreferencesKey("focus_notifications")
        val FOCUS_MIN = intPreferencesKey("focus_minutes")
        val SHORT_MIN = intPreferencesKey("short_break_minutes")
        val LONG_MIN = intPreferencesKey("long_break_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val AUTO_LOCK = stringPreferencesKey("auto_lock_delay")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }
}
