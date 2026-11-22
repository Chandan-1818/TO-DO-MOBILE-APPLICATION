package com.chandan.master.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["category_id"]), Index(value = ["due_day"])],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    @ColumnInfo(name = "due_day") val dueDay: Long?,
    @ColumnInfo(name = "due_minute") val dueMinute: Int?,
    val priority: Int,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "reminder_offset") val reminderOffset: Int?,
    val completed: Boolean,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    @ColumnInfo(name = "schedule_mask") val scheduleMask: Int,
    @ColumnInfo(name = "reminder_minute") val reminderMinute: Int?,
    @ColumnInfo(name = "start_day") val startDay: Long,
    val archived: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "habit_completions",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habit_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habit_id", "day"], unique = true)],
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val day: Long,
)

@Entity(tableName = "focus_sessions", indices = [Index(value = ["started_at"], unique = true)])
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "planned_seconds") val plannedSeconds: Int,
    @ColumnInfo(name = "actual_seconds") val actualSeconds: Int,
    val completed: Boolean,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val pinned: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
