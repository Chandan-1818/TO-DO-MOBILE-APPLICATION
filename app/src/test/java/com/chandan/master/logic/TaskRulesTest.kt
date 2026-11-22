package com.chandan.master.logic

import com.chandan.master.domain.logic.TaskFilter
import com.chandan.master.domain.logic.TaskRules
import com.chandan.master.domain.model.Priority
import com.chandan.master.domain.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRulesTest {
    private val today = LocalDate.of(2026, 3, 10)
    private val now = LocalDateTime.of(2026, 3, 10, 14, 30)
    private fun t(id: Long, title: String, due: LocalDate? = null, minute: Int? = null, p: Priority = Priority.MEDIUM, done: Boolean = false, cat: Long? = null, created: Long = id, completedAt: Long? = null, desc: String = "") =
        Task(id = id, title = title, description = desc, dueDate = due, dueMinuteOfDay = minute, priority = p, categoryId = cat, completed = done, completedAtMs = completedAt, createdAtMs = created)

    @Test fun overdueRules() {
        assertTrue(TaskRules.isOverdue(t(1, "a", today.minusDays(1)), now))
        assertFalse(TaskRules.isOverdue(t(2, "b", today), now)) // date-only task due today is not overdue until the day ends
        assertTrue(TaskRules.isOverdue(t(3, "c", today, 14 * 60), now))
        assertFalse(TaskRules.isOverdue(t(4, "d", today, 15 * 60), now))
        assertFalse(TaskRules.isOverdue(t(5, "e", today.minusDays(5), done = true), now))
        assertFalse(TaskRules.isOverdue(t(6, "f"), now))
    }

    @Test fun filters() {
        val tasks = listOf(
            t(1, "overdue", today.minusDays(2)), t(2, "today", today), t(3, "soon", today.plusDays(3)), t(4, "someday"),
            t(5, "done", today, done = true, completedAt = 5),
        )
        fun ids(f: TaskFilter) = TaskRules.apply(tasks, f, "", null, now).map { it.id }
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(TaskFilter.ALL))
        assertEquals(listOf(2L), ids(TaskFilter.TODAY))
        assertEquals(listOf(3L), ids(TaskFilter.UPCOMING))
        assertEquals(listOf(1L), ids(TaskFilter.OVERDUE))
        assertEquals(listOf(5L), ids(TaskFilter.COMPLETED))
    }

    @Test fun searchIsCaseInsensitiveOverTitleAndDescription() {
        val tasks = listOf(t(1, "Buy Milk"), t(2, "Call mum", desc = "about the MILK delivery"), t(3, "Gym"))
        assertEquals(listOf(1L, 2L), TaskRules.apply(tasks, TaskFilter.ALL, "milk", null, now).map { it.id }.sorted())
        assertEquals(3, TaskRules.apply(tasks, TaskFilter.ALL, "   ", null, now).size)
        assertEquals(0, TaskRules.apply(tasks, TaskFilter.ALL, "zzz", null, now).size)
    }

    @Test fun categoryFilter() {
        val tasks = listOf(t(1, "a", cat = 7), t(2, "b", cat = 8), t(3, "c"))
        assertEquals(listOf(1L), TaskRules.apply(tasks, TaskFilter.ALL, "", 7, now).map { it.id })
    }

    @Test fun sortOrderDueThenTimeThenPriority() {
        val tasks = listOf(
            t(1, "undated"), t(2, "later", today.plusDays(1)), t(3, "today-late", today, 18 * 60),
            t(4, "today-early-low", today, 8 * 60, Priority.LOW), t(5, "today-early-urgent", today, 8 * 60, Priority.URGENT),
        )
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), TaskRules.apply(tasks, TaskFilter.ALL, "", null, now).map { it.id })
    }

    @Test fun completedListShowsMostRecentlyCompletedFirst() {
        val tasks = listOf(t(1, "a", done = true, completedAt = 10), t(2, "b", done = true, completedAt = 30), t(3, "c", done = true, completedAt = 20))
        assertEquals(listOf(2L, 3L, 1L), TaskRules.apply(tasks, TaskFilter.COMPLETED, "", null, now).map { it.id })
    }

    @Test fun titleValidation() {
        assertTrue(TaskRules.titleError("")); assertTrue(TaskRules.titleError("   ")); assertTrue(TaskRules.titleError("x".repeat(121)))
        assertFalse(TaskRules.titleError("ok")); assertFalse(TaskRules.titleError("x".repeat(120)))
    }
}
