package com.chandan.master.data.repository

import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.local.db.dao.CategoryDao
import com.chandan.master.data.local.db.dao.FocusSessionDao
import com.chandan.master.data.local.db.dao.HabitCompletionDao
import com.chandan.master.data.local.db.dao.HabitDao
import com.chandan.master.data.local.db.dao.NoteDao
import com.chandan.master.data.local.db.dao.TaskDao
import com.chandan.master.data.local.db.entity.CategoryEntity
import com.chandan.master.data.local.db.entity.HabitCompletionEntity
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.CategoryRepository
import com.chandan.master.domain.repository.FocusRepository
import com.chandan.master.domain.repository.HabitRepository
import com.chandan.master.domain.repository.NoteRepository
import com.chandan.master.domain.repository.ReminderScheduler
import com.chandan.master.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    private val clock: DateTimeProvider,
    private val reminders: ReminderScheduler,
) : TaskRepository {
    override fun observeTasks(): Flow<List<Task>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeTask(id: Long): Flow<Task?> = dao.observeById(id).map { it?.toDomain() }
    override suspend fun getTask(id: Long): Task? = dao.getById(id)?.toDomain()

    override suspend fun save(task: Task): Long {
        val now = clock.now().toEpochMilli()
        val clean = task.copy(title = task.title.trim(), description = task.description.trim())
        val id = if (clean.id == 0L) {
            dao.insert(clean.copy(createdAtMs = now, updatedAtMs = now).toEntity())
        } else {
            val existing = dao.getById(clean.id)
            dao.update(clean.copy(createdAtMs = existing?.createdAt ?: now, updatedAtMs = now).toEntity())
            clean.id
        }
        dao.getById(id)?.toDomain()?.let { reminders.scheduleTask(it) }
        return id
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
        reminders.cancelTask(id)
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) {
        val now = clock.now().toEpochMilli()
        dao.setCompleted(id, completed, if (completed) now else null, now)
        if (completed) reminders.cancelTask(id) else dao.getById(id)?.toDomain()?.let { reminders.scheduleTask(it) }
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(private val dao: CategoryDao) : CategoryRepository {
    override fun observeCategories(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun createOrGet(name: String): Long {
        val clean = name.trim().replace(Regex("\\s+"), " ").take(60)
        require(clean.isNotEmpty()) { "Category name is empty" }
        dao.getAll().firstOrNull { it.name.equals(clean, ignoreCase = true) }?.let { return it.id }
        return dao.insert(CategoryEntity(name = clean))
    }

    override suspend fun rename(id: Long, name: String): Boolean {
        val clean = name.trim().replace(Regex("\\s+"), " ").take(60)
        if (clean.isEmpty()) return false
        if (dao.getAll().any { it.id != id && it.name.equals(clean, ignoreCase = true) }) return false
        dao.rename(id, clean)
        return true
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitDao,
    private val completions: HabitCompletionDao,
    private val clock: DateTimeProvider,
    private val reminders: ReminderScheduler,
) : HabitRepository {
    override fun observeHabits(): Flow<List<Habit>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCompletions(): Flow<Map<Long, Set<LocalDate>>> = completions.observeAll().map { list ->
        list.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.day) }).mapValues { it.value.toSet() }
    }

    override suspend fun getHabit(id: Long): Habit? = dao.getById(id)?.toDomain()

    override suspend fun save(habit: Habit): Long {
        val clean = habit.copy(name = habit.name.trim(), description = habit.description.trim())
        val id = if (clean.id == 0L) dao.insert(clean.copy(createdAtMs = clock.now().toEpochMilli()).toEntity())
        else { dao.update(clean.toEntity()); clean.id }
        dao.getById(id)?.toDomain()?.let { reminders.scheduleHabit(it) }
        return id
    }

    override suspend fun delete(id: Long) {
        dao.delete(id) // completions go with it (foreign-key cascade)
        reminders.cancelHabit(id)
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        val h = dao.getById(id) ?: return
        dao.update(h.copy(archived = archived))
        if (archived) reminders.cancelHabit(id) else reminders.scheduleHabit(h.copy(archived = false).toDomain())
    }

    override suspend fun setCompletion(habitId: Long, date: LocalDate, done: Boolean) {
        if (done) completions.insertIgnore(HabitCompletionEntity(habitId = habitId, day = date.toEpochDay()))
        else completions.delete(habitId, date.toEpochDay())
    }
}

@Singleton
class FocusRepositoryImpl @Inject constructor(private val dao: FocusSessionDao) : FocusRepository {
    override fun observeSessions(): Flow<List<FocusSession>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun record(session: FocusSession) { dao.insertIgnore(session.copy(id = 0).toEntity()) }
}

@Singleton
class NoteRepositoryImpl @Inject constructor(private val dao: NoteDao, private val clock: DateTimeProvider) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getNote(id: Long): Note? = dao.getById(id)?.toDomain()

    override suspend fun save(note: Note): Long {
        val now = clock.now().toEpochMilli()
        return if (note.id == 0L) dao.insert(note.copy(createdAtMs = now, updatedAtMs = now).toEntity())
        else {
            val created = dao.getById(note.id)?.createdAt ?: now
            dao.update(note.copy(createdAtMs = created, updatedAtMs = now).toEntity()); note.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)
}
