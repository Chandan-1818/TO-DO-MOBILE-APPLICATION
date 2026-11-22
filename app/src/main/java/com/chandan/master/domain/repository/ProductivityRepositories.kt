package com.chandan.master.domain.repository

import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Task
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTask(id: Long): Flow<Task?>
    suspend fun getTask(id: Long): Task?
    /** Inserts (id == 0) or updates, stamps times, and (re)schedules or cancels the reminder. Returns the id. */
    suspend fun save(task: Task): Long
    suspend fun delete(id: Long)
    suspend fun setCompleted(id: Long, completed: Boolean)
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    /** Returns the id of an existing category with the same name (case-insensitive) or creates one. */
    suspend fun createOrGet(name: String): Long
    /** Returns false (and changes nothing) if [name] is blank or another category already uses it. */
    suspend fun rename(id: Long, name: String): Boolean
    /** Deletes the category; its tasks are kept and become uncategorised. */
    suspend fun delete(id: Long)
}

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeCompletions(): Flow<Map<Long, Set<LocalDate>>>
    suspend fun getHabit(id: Long): Habit?
    suspend fun save(habit: Habit): Long
    suspend fun delete(id: Long)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun setCompletion(habitId: Long, date: LocalDate, done: Boolean)
}

interface FocusRepository {
    fun observeSessions(): Flow<List<FocusSession>>
    /** Idempotent: recording the same session twice (same start time) stores it once. */
    suspend fun record(session: FocusSession)
}

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun delete(id: Long)
    suspend fun setPinned(id: Long, pinned: Boolean)
}

/** Schedules/cancels system reminders. Implemented with WorkManager; timing is best-effort, not exact. */
interface ReminderScheduler {
    suspend fun scheduleTask(task: Task)
    suspend fun cancelTask(taskId: Long)
    suspend fun scheduleHabit(habit: Habit)
    suspend fun cancelHabit(habitId: Long)
    suspend fun rescheduleAll()
    fun scheduleFocusFinish(delayMillis: Long)
    fun cancelFocusFinish()
    /** Cancels every scheduled reminder (used when the account is reset). */
    fun cancelAll()
}
