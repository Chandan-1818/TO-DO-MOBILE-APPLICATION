package com.chandan.master.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chandan.master.data.local.db.entity.CategoryEntity
import com.chandan.master.data.local.db.entity.FocusSessionEntity
import com.chandan.master.data.local.db.entity.HabitCompletionEntity
import com.chandan.master.data.local.db.entity.HabitEntity
import com.chandan.master.data.local.db.entity.NoteEntity
import com.chandan.master.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC") fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories") suspend fun getAll(): List<CategoryEntity>
    @Insert suspend fun insert(category: CategoryEntity): Long
    @Query("UPDATE categories SET name = :name WHERE id = :id") suspend fun rename(id: Long, name: String)
    /** Tasks keep existing: the foreign key sets their category to NULL. */
    @Query("DELETE FROM categories WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks") fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE id = :id") fun observeById(id: Long): Flow<TaskEntity?>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun getById(id: Long): TaskEntity?
    @Query("SELECT * FROM tasks") suspend fun getAll(): List<TaskEntity>
    @Insert suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun delete(id: Long)
    @Query("UPDATE tasks SET completed = :completed, completed_at = :completedAt, updated_at = :now WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?, now: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits") fun observeAll(): Flow<List<HabitEntity>>
    @Query("SELECT * FROM habits WHERE id = :id") suspend fun getById(id: Long): HabitEntity?
    @Query("SELECT * FROM habits WHERE id = :id") fun observeById(id: Long): Flow<HabitEntity?>
    @Query("SELECT * FROM habits") suspend fun getAll(): List<HabitEntity>
    @Insert suspend fun insert(habit: HabitEntity): Long
    @Update suspend fun update(habit: HabitEntity)
    /** Completions are removed by the foreign-key cascade: no orphaned history. */
    @Query("DELETE FROM habits WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions") fun observeAll(): Flow<List<HabitCompletionEntity>>
    @Query("SELECT * FROM habit_completions") suspend fun getAll(): List<HabitCompletionEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(c: HabitCompletionEntity): Long
    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId AND day = :day") suspend fun delete(habitId: Long, day: Long)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY started_at DESC") fun observeAll(): Flow<List<FocusSessionEntity>>
    @Query("SELECT * FROM focus_sessions") suspend fun getAll(): List<FocusSessionEntity>
    /** The unique index on started_at makes recording idempotent (UI and background worker can both try). */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(s: FocusSessionEntity): Long
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes") fun observeAll(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun getById(id: Long): NoteEntity?
    @Query("SELECT * FROM notes") suspend fun getAll(): List<NoteEntity>
    @Insert suspend fun insert(note: NoteEntity): Long
    @Update suspend fun update(note: NoteEntity)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun delete(id: Long)
    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id") suspend fun setPinned(id: Long, pinned: Boolean)
}
