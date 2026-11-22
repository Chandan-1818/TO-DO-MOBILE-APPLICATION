package com.chandan.master.logic

import com.chandan.master.domain.logic.InsightsCalculator
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsCalculatorTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 3, 10)
    private val now = LocalDateTime.of(2026, 3, 10, 12, 0)
    private fun ms(date: LocalDate, hour: Int = 10) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun calc(tasks: List<Task> = emptyList(), habits: List<Habit> = emptyList(), comps: Map<Long, Set<LocalDate>> = emptyMap(), sessions: List<FocusSession> = emptyList()) =
        InsightsCalculator.calculate(tasks, habits, comps, sessions, now, zone)

    @Test fun emptyDataGivesNullPercentagesAndNoData() {
        val s = calc()
        assertEquals(0, s.totalTasks); assertNull(s.taskCompletionPercent); assertNull(s.habitConsistencyPercent)
        assertFalse(s.hasAnyData); assertEquals(7, s.last7Days.size); assertEquals(today, s.last7Days.last().date)
    }

    @Test fun taskCountsAndPercentage() {
        val tasks = listOf(
            Task(1, "a", completed = true, completedAtMs = ms(today)), Task(2, "b"), Task(3, "c", dueDate = today.minusDays(1)), Task(4, "d", completed = true, completedAtMs = ms(today.minusDays(2))),
        )
        val s = calc(tasks)
        assertEquals(4, s.totalTasks); assertEquals(2, s.completedTasks); assertEquals(2, s.pendingTasks); assertEquals(1, s.overdueTasks); assertEquals(50, s.taskCompletionPercent)
        assertEquals(1, s.last7Days.last().tasksCompleted); assertEquals(1, s.last7Days[4].tasksCompleted)
    }

    @Test fun habitConsistencyAndStreaksUseOnlyScheduledDays() {
        val h = Habit(1, "Read", scheduleMask = WeekdayMask.ALL, startDate = today.minusDays(3))
        val s = calc(habits = listOf(h), comps = mapOf(1L to setOf(today.minusDays(3), today.minusDays(2), today.minusDays(1), today)))
        assertEquals(100, s.habitConsistencyPercent); assertEquals(4, s.habitStreaks.single().currentStreak)
        val missed = calc(habits = listOf(h), comps = mapOf(1L to setOf(today.minusDays(3))))
        assertEquals(33, missed.habitConsistencyPercent) // 1 of 3 elapsed days; today pending
    }

    @Test fun archivedHabitsAreExcluded() {
        val h = Habit(1, "Old", startDate = today.minusDays(5), archived = true)
        assertTrue(calc(habits = listOf(h)).habitStreaks.isEmpty())
    }

    @Test fun focusTotalsAndWeeklySummaryUseLocalDays() {
        val sessions = listOf(
            FocusSession(1, ms(today), 1500, 1500, true), FocusSession(2, ms(today.minusDays(1)), 1500, 600, false),
            FocusSession(3, ms(today.minusDays(30)), 1500, 1500, true),
        )
        val s = calc(sessions = sessions)
        assertEquals(3600L, s.totalFocusSeconds); assertEquals(2, s.completedFocusSessions)
        assertEquals(2100, s.focusSecondsLast7Days); assertEquals(1500, s.last7Days.last().focusSeconds); assertEquals(600, s.last7Days[5].focusSeconds)
        assertTrue(s.hasAnyData)
    }

    @Test fun lateNightSessionBelongsToTheLocalDayNotUtc() {
        // 23:30 IST on the 10th is 18:00Z the same day; 00:30 IST on the 10th is 19:00Z on the 9th.
        val early = FocusSession(1, Instant.parse("2026-03-09T19:00:00Z").toEpochMilli(), 600, 600, true)
        val s = calc(sessions = listOf(early))
        assertEquals(600, s.last7Days.last().focusSeconds)
    }
}
