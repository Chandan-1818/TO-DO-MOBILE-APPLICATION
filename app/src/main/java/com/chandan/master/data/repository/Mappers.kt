package com.chandan.master.data.repository

import com.chandan.master.data.local.db.entity.CategoryEntity
import com.chandan.master.data.local.db.entity.FocusSessionEntity
import com.chandan.master.data.local.db.entity.HabitEntity
import com.chandan.master.data.local.db.entity.NoteEntity
import com.chandan.master.data.local.db.entity.TaskEntity
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Priority
import com.chandan.master.domain.model.Task
import java.time.LocalDate

internal fun CategoryEntity.toDomain() = Category(id, name)
internal fun Category.toEntity() = CategoryEntity(id, name)

internal fun TaskEntity.toDomain() = Task(
    id = id, title = title, description = description, dueDate = dueDay?.let { LocalDate.ofEpochDay(it) }, dueMinuteOfDay = dueMinute,
    priority = Priority.fromLevel(priority), categoryId = categoryId, reminderOffsetMinutes = reminderOffset,
    completed = completed, completedAtMs = completedAt, createdAtMs = createdAt, updatedAtMs = updatedAt,
)

internal fun Task.toEntity() = TaskEntity(
    id = id, title = title, description = description, dueDay = dueDate?.toEpochDay(), dueMinute = dueMinuteOfDay,
    priority = priority.level, categoryId = categoryId, reminderOffset = reminderOffsetMinutes,
    completed = completed, completedAt = completedAtMs, createdAt = createdAtMs, updatedAt = updatedAtMs,
)

internal fun HabitEntity.toDomain() = Habit(
    id = id, name = name, description = description, scheduleMask = scheduleMask, reminderMinuteOfDay = reminderMinute,
    startDate = LocalDate.ofEpochDay(startDay), archived = archived, createdAtMs = createdAt,
)

internal fun Habit.toEntity() = HabitEntity(
    id = id, name = name, description = description, scheduleMask = scheduleMask, reminderMinute = reminderMinuteOfDay,
    startDay = startDate.toEpochDay(), archived = archived, createdAt = createdAtMs,
)

internal fun FocusSessionEntity.toDomain() = FocusSession(id, startedAt, plannedSeconds, actualSeconds, completed)
internal fun FocusSession.toEntity() = FocusSessionEntity(id, startedAtMs, plannedSeconds, actualSeconds, completed)

internal fun NoteEntity.toDomain() = Note(id, title, content, pinned, createdAt, updatedAt)
internal fun Note.toEntity() = NoteEntity(id, title, content, pinned, createdAtMs, updatedAtMs)
