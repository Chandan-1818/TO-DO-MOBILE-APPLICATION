package com.chandan.master.domain.model

import com.chandan.master.domain.auth.AutoLockDelay
import com.chandan.master.domain.logic.FocusSettings

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.Default,
    val accentPalette: AccentPalette = AccentPalette.Default,
    val autoLockDelay: AutoLockDelay = AutoLockDelay.Default,
    /** Only a convenience flag; the real gate is the Keystore key that requires biometric authentication. */
    val biometricEnabled: Boolean = false,
    /** Master switch for every notification Master posts. */
    val notificationsEnabled: Boolean = true,
    val taskReminders: Boolean = true,
    val habitReminders: Boolean = true,
    val focusNotifications: Boolean = true,
    val focusSettings: FocusSettings = FocusSettings(),
)
