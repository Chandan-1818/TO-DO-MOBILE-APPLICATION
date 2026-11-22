package com.chandan.master.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chandan.master.data.local.db.dao.CategoryDao
import com.chandan.master.data.local.db.dao.FocusSessionDao
import com.chandan.master.data.local.db.dao.HabitCompletionDao
import com.chandan.master.data.local.db.dao.HabitDao
import com.chandan.master.data.local.db.dao.NoteDao
import com.chandan.master.data.local.db.dao.ProfileDao
import com.chandan.master.data.local.db.dao.TaskDao
import com.chandan.master.data.local.db.entity.CategoryEntity
import com.chandan.master.data.local.db.entity.FocusSessionEntity
import com.chandan.master.data.local.db.entity.HabitCompletionEntity
import com.chandan.master.data.local.db.entity.HabitEntity
import com.chandan.master.data.local.db.entity.NoteEntity
import com.chandan.master.data.local.db.entity.ProfileEntity
import com.chandan.master.data.local.db.entity.TaskEntity

/**
 * Master's structured store. `exportSchema = true` writes schemas to app/schemas (commit them!) so
 * every future version bump can ship a tested [androidx.room.migration.Migration].
 * Destructive migration is intentionally never enabled: user data must survive upgrades.
 */
@Database(
    entities = [
        ProfileEntity::class, CategoryEntity::class, TaskEntity::class, HabitEntity::class,
        HabitCompletionEntity::class, FocusSessionEntity::class, NoteEntity::class,
    ],
    version = MasterDatabase.VERSION,
    exportSchema = true,
)
abstract class MasterDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val VERSION = 2
        const val NAME = "master.db"
    }
}
