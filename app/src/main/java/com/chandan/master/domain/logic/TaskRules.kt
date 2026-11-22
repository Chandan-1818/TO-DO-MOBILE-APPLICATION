package com.chandan.master.domain.logic

import com.chandan.master.domain.model.Task
import java.time.LocalDate
import java.time.LocalDateTime

enum class TaskFilter { ALL, TODAY, UPCOMING, OVERDUE, COMPLETED }

object TaskRules {
    fun isOverdue(task: Task, now: LocalDateTime): Boolean {
        if (task.completed) return false
        val date = task.dueDate ?: return false
        val minute = task.dueMinuteOfDay
        return if (minute == null) date.isBefore(now.toLocalDate())
        else date.atTime(minute / 60, minute % 60).isBefore(now)
    }

    fun isDueToday(task: Task, today: LocalDate): Boolean = task.dueDate == today

    fun matches(task: Task, filter: TaskFilter, now: LocalDateTime): Boolean {
        val today = now.toLocalDate()
        return when (filter) {
            TaskFilter.ALL -> !task.completed
            TaskFilter.TODAY -> !task.completed && task.dueDate == today
            TaskFilter.UPCOMING -> !task.completed && task.dueDate != null && task.dueDate.isAfter(today)
            TaskFilter.OVERDUE -> isOverdue(task, now)
            TaskFilter.COMPLETED -> task.completed
        }
    }

    fun search(task: Task, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return task.title.contains(q, ignoreCase = true) || task.description.contains(q, ignoreCase = true)
    }

    /** Filter, search, optional category, then sort. Open tasks: soonest due first (undated last), higher priority first, newest first. */
    fun apply(tasks: List<Task>, filter: TaskFilter, query: String, categoryId: Long?, now: LocalDateTime): List<Task> {
        val filtered = tasks.filter { matches(it, filter, now) && search(it, query) && (categoryId == null || it.categoryId == categoryId) }
        return if (filter == TaskFilter.COMPLETED) filtered.sortedByDescending { it.completedAtMs ?: 0L }
        else filtered.sortedWith(
            compareBy<Task> { it.dueDate ?: LocalDate.MAX }
                .thenBy { it.dueMinuteOfDay ?: Int.MAX_VALUE }
                .thenByDescending { it.priority.level }
                .thenByDescending { it.createdAtMs },
        )
    }

    fun titleError(title: String): Boolean = title.trim().isEmpty() || title.trim().length > 120
}
