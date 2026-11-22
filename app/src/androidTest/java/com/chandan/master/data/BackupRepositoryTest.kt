package com.chandan.master.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.backup.BackupRepositoryImpl
import com.chandan.master.data.local.db.MasterDatabase
import com.chandan.master.data.local.preferences.PreferencesDataSource
import com.chandan.master.data.repository.PreferencesRepositoryImpl
import com.chandan.master.domain.backup.BackupFormatException
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.ReminderScheduler
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private class Clock : DateTimeProvider { override fun now(): Instant = Instant.parse("2026-03-10T10:00:00Z"); override fun zone(): ZoneId = ZoneId.of("UTC") }
    private class NoReminders : ReminderScheduler {
        override suspend fun scheduleTask(task: Task) = Unit
        override suspend fun cancelTask(taskId: Long) = Unit
        override suspend fun scheduleHabit(habit: Habit) = Unit
        override suspend fun cancelHabit(habitId: Long) = Unit
        override suspend fun rescheduleAll() = Unit
        override fun scheduleFocusFinish(delayMillis: Long) = Unit
        override fun cancelFocusFinish() = Unit
        override fun cancelAll() = Unit
    }

    private lateinit var db: MasterDatabase
    private lateinit var repo: BackupRepositoryImpl
    private val file = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "bk-${System.nanoTime()}.preferences_pb")

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MasterDatabase::class.java).allowMainThreadQueries().build()
        val prefs = PreferencesRepositoryImpl(PreferencesDataSource(PreferenceDataStoreFactory.create { file }))
        repo = BackupRepositoryImpl(db, prefs, Clock(), NoReminders())
    }
    @After fun tearDown() { db.close(); file.delete() }

    private fun seed() = runBlocking {
        val cat = db.categoryDao().insert(com.chandan.master.data.local.db.entity.CategoryEntity(name = "Work"))
        db.taskDao().insert(com.chandan.master.data.local.db.entity.TaskEntity(title = "Report", description = "d", dueDay = LocalDate.of(2026, 3, 11).toEpochDay(), dueMinute = 600, priority = 2, categoryId = cat, reminderOffset = 60, completed = false, completedAt = null, createdAt = 5, updatedAt = 5))
        val h = db.habitDao().insert(com.chandan.master.data.local.db.entity.HabitEntity(name = "Read", description = "", scheduleMask = 127, reminderMinute = null, startDay = 1, archived = false, createdAt = 3))
        db.habitCompletionDao().insertIgnore(com.chandan.master.data.local.db.entity.HabitCompletionEntity(habitId = h, day = 20_000))
        db.focusSessionDao().insertIgnore(com.chandan.master.data.local.db.entity.FocusSessionEntity(startedAt = 99, plannedSeconds = 1500, actualSeconds = 1500, completed = true))
        db.noteDao().insert(com.chandan.master.data.local.db.entity.NoteEntity(title = "Idea", content = "x", pinned = true, createdAt = 7, updatedAt = 7))
    }

    @Test fun exportThenRestoreIntoAnEmptyDatabaseRebuildsEverythingWithRelationships() = runBlocking {
        seed()
        val json = repo.createBackup()
        db.clearAllTables()
        val preview = repo.inspect(json)
        assertEquals(1, preview.plan.summary.tasks.added)
        repo.restore(preview.data)
        val task = db.taskDao().getAll().single()
        val cat = db.categoryDao().getAll().single()
        assertEquals(cat.id, task.categoryId)
        assertEquals(1, db.habitCompletionDao().getAll().size); assertEquals(1, db.focusSessionDao().getAll().size); assertEquals(1, db.noteDao().getAll().size)
    }

    @Test fun restoringTheSameBackupTwiceNeverCreatesDuplicates() = runBlocking {
        seed()
        val data = repo.inspect(repo.createBackup()).data
        val second = repo.restore(data)
        assertEquals(0, second.totalAdded)
        assertEquals(1, db.taskDao().getAll().size); assertEquals(1, db.categoryDao().getAll().size); assertEquals(1, db.habitDao().getAll().size)
    }

    @Test fun backupNeverContainsCredentialsAndInvalidFilesAreRejectedWithoutTouchingData() = runBlocking {
        seed()
        val json = repo.createBackup()
        assertFalse(json.contains("pin_hash", true)); assertFalse(json.contains("salt", true))
        try { repo.inspect(json.take(json.length / 2)); fail("truncated backup accepted") } catch (e: BackupFormatException) { assertTrue(e.message!!.isNotBlank()) }
        assertEquals(1, db.taskDao().getAll().size)
    }
}
