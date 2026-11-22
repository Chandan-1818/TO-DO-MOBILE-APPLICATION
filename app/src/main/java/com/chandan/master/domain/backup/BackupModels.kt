package com.chandan.master.domain.backup

import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.HabitCompletion
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Task

/** Settings that are safe and useful to carry over. Never contains PINs, hashes, keys or the biometric flag. */
data class BackupPreferences(
    val themeMode: String,
    val accentPalette: String,
    val notificationsEnabled: Boolean,
    val taskReminders: Boolean,
    val habitReminders: Boolean,
    val focusNotifications: Boolean,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
)

data class BackupData(
    val version: Int = BackupCodec.VERSION,
    val exportedAtMs: Long,
    val appVersion: String,
    /** Informational only: restoring never changes the existing account's username or PIN. */
    val profileUsername: String?,
    val preferences: BackupPreferences?,
    val categories: List<Category>,
    val tasks: List<Task>,
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
    val sessions: List<FocusSession>,
    val notes: List<Note>,
)

class BackupFormatException(message: String) : Exception(message)
