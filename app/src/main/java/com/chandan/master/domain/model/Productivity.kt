package com.chandan.master.domain.model

import java.time.LocalDate

enum class Priority(val level: Int) {
    LOW(0), MEDIUM(1), HIGH(2), URGENT(3);

    companion object {
        fun fromLevel(level: Int?): Priority = entries.firstOrNull { it.level == level } ?: MEDIUM
    }
}

data class Category(val id: Long, val name: String)

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: LocalDate? = null,
    /** Minutes after midnight (0..1439); null means "no specific time". */
    val dueMinuteOfDay: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val categoryId: Long? = null,
    /** Minutes before the due moment to remind (0 = at the due time); null = no reminder. */
    val reminderOffsetMinutes: Int? = null,
    val completed: Boolean = false,
    val completedAtMs: Long? = null,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)

data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    /** Bit 0 = Monday ... bit 6 = Sunday. See [com.chandan.master.domain.logic.WeekdayMask]. */
    val scheduleMask: Int = 127,
    val reminderMinuteOfDay: Int? = null,
    val startDate: LocalDate,
    val archived: Boolean = false,
    val createdAtMs: Long = 0,
)

data class HabitCompletion(val habitId: Long, val date: LocalDate)

data class FocusSession(
    val id: Long = 0,
    val startedAtMs: Long,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    /** True when the full planned time elapsed; false when the user stopped early. */
    val completed: Boolean,
)

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val pinned: Boolean = false,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)
