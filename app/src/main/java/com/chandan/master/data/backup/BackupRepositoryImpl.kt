package com.chandan.master.data.backup

import androidx.room.withTransaction
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.local.db.MasterDatabase
import com.chandan.master.data.local.db.entity.CategoryEntity
import com.chandan.master.data.local.db.entity.HabitCompletionEntity
import com.chandan.master.data.repository.toDomain
import com.chandan.master.data.repository.toEntity
import com.chandan.master.domain.backup.BackupCodec
import com.chandan.master.domain.backup.BackupData
import com.chandan.master.domain.backup.BackupPreferences
import com.chandan.master.domain.backup.ExistingData
import com.chandan.master.domain.backup.RestorePlanner
import com.chandan.master.domain.backup.RestoreSummary
import com.chandan.master.domain.logic.FocusSettings
import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.HabitCompletion
import com.chandan.master.domain.model.ThemeMode
import com.chandan.master.domain.repository.BackupPreview
import com.chandan.master.domain.repository.BackupRepository
import com.chandan.master.domain.repository.PreferencesRepository
import com.chandan.master.domain.repository.ReminderScheduler
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val db: MasterDatabase,
    private val prefs: PreferencesRepository,
    private val clock: DateTimeProvider,
    private val reminders: ReminderScheduler,
) : BackupRepository {

    private suspend fun loadAll(): ExistingData = ExistingData(
        categories = db.categoryDao().getAll().map { it.toDomain() },
        tasks = db.taskDao().getAll().map { it.toDomain() },
        habits = db.habitDao().getAll().map { it.toDomain() },
        completions = db.habitCompletionDao().getAll().map { HabitCompletion(it.habitId, LocalDate.ofEpochDay(it.day)) },
        sessions = db.focusSessionDao().getAll().map { it.toDomain() },
        notes = db.noteDao().getAll().map { it.toDomain() },
    )

    override suspend fun createBackup(): String {
        val all = db.withTransaction { loadAll() }
        val p = prefs.preferences.first()
        val data = BackupData(
            exportedAtMs = clock.now().toEpochMilli(),
            appVersion = com.chandan.master.BuildConfig.VERSION_NAME,
            profileUsername = db.profileDao().getActive()?.username,
            preferences = BackupPreferences(
                themeMode = p.themeMode.storageValue, accentPalette = p.accentPalette.storageValue,
                notificationsEnabled = p.notificationsEnabled, taskReminders = p.taskReminders, habitReminders = p.habitReminders,
                focusNotifications = p.focusNotifications, focusMinutes = p.focusSettings.focusMinutes,
                shortBreakMinutes = p.focusSettings.shortBreakMinutes, longBreakMinutes = p.focusSettings.longBreakMinutes,
            ),
            categories = all.categories, tasks = all.tasks, habits = all.habits, completions = all.completions, sessions = all.sessions, notes = all.notes,
        )
        return BackupCodec.encode(data)
    }

    override suspend fun inspect(text: String): BackupPreview {
        val data = BackupCodec.decode(text)
        return BackupPreview(data, RestorePlanner.plan(db.withTransaction { loadAll() }, data))
    }

    override suspend fun restore(data: BackupData): RestoreSummary {
        val summary = db.withTransaction {
            // Re-plan inside the transaction so the result reflects the data at the moment of writing.
            val plan = RestorePlanner.plan(loadAll(), data)
            val categoryIds = HashMap(plan.existingCategoryFor)
            plan.newCategories.forEach { categoryIds[it.id] = db.categoryDao().insert(CategoryEntity(name = it.name.trim())) }
            val habitIds = HashMap(plan.existingHabitFor)
            plan.newHabits.forEach { habitIds[it.id] = db.habitDao().insert(it.copy(id = 0).toEntity()) }
            plan.newTasks.forEach { t ->
                db.taskDao().insert(t.copy(id = 0, categoryId = t.categoryId?.let { categoryIds[it] }).toEntity())
            }
            plan.newCompletions.forEach { c ->
                habitIds[c.habitId]?.let { db.habitCompletionDao().insertIgnore(HabitCompletionEntity(habitId = it, day = c.date.toEpochDay())) }
            }
            plan.newSessions.forEach { db.focusSessionDao().insertIgnore(it.copy(id = 0).toEntity()) }
            plan.newNotes.forEach { db.noteDao().insert(it.copy(id = 0).toEntity()) }
            plan.summary
        }
        // Settings (outside the DB transaction): appearance, notification switches and timer lengths.
        // Security settings (PIN, biometrics, auto-lock) are deliberately never restored.
        data.preferences?.let { p ->
            prefs.setThemeMode(ThemeMode.fromStorageValue(p.themeMode))
            prefs.setAccentPalette(AccentPalette.fromStorageValue(p.accentPalette))
            prefs.setNotificationsEnabled(p.notificationsEnabled); prefs.setTaskReminders(p.taskReminders)
            prefs.setHabitReminders(p.habitReminders); prefs.setFocusNotifications(p.focusNotifications)
            prefs.setFocusSettings(FocusSettings(p.focusMinutes, p.shortBreakMinutes, p.longBreakMinutes))
        }
        reminders.rescheduleAll()
        return summary
    }
}
