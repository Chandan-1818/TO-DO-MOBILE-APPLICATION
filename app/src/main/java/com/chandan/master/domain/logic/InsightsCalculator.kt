package com.chandan.master.domain.logic

import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class HabitStreakInfo(val name: String, val currentStreak: Int, val longestStreak: Int)
data class DayActivity(val date: LocalDate, val tasksCompleted: Int, val habitsCompleted: Int, val focusSeconds: Int)

data class InsightsSummary(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
    /** Null when there are no tasks. */
    val taskCompletionPercent: Int?,
    /** Share of scheduled habit days completed in the last 30 days; null if no habit was scheduled. */
    val habitConsistencyPercent: Int?,
    val habitStreaks: List<HabitStreakInfo>,
    val totalFocusSeconds: Long,
    val completedFocusSessions: Int,
    val focusSecondsLast7Days: Int,
    /** Oldest first, ending today. */
    val last7Days: List<DayActivity>,
) {
    val hasAnyData: Boolean get() = totalTasks > 0 || habitStreaks.isNotEmpty() || totalFocusSeconds > 0
}

object InsightsCalculator {
    const val CONSISTENCY_WINDOW_DAYS = 30

    fun calculate(
        tasks: List<Task>,
        habits: List<Habit>,
        completions: Map<Long, Set<LocalDate>>,
        sessions: List<FocusSession>,
        now: LocalDateTime,
        zone: ZoneId,
    ): InsightsSummary {
        val today = now.toLocalDate()
        val total = tasks.size
        val completed = tasks.count { it.completed }
        val active = habits.filter { !it.archived }

        var scheduled = 0
        var done = 0
        val windowStart = today.minusDays(CONSISTENCY_WINDOW_DAYS - 1L)
        val streaks = active.map { h ->
            val dates = completions[h.id].orEmpty()
            val stats = StreakCalculator.calculate(h.scheduleMask, h.startDate, dates, today)
            val from = if (h.startDate.isAfter(windowStart)) h.startDate else windowStart
            var d = from
            while (!d.isAfter(today)) {
                if (WeekdayMask.isScheduled(h.scheduleMask, d)) {
                    if (d in dates) { scheduled++; done++ } else if (d != today) scheduled++
                }
                d = d.plusDays(1)
            }
            HabitStreakInfo(h.name, stats.currentStreak, stats.longestStreak)
        }.sortedByDescending { it.currentStreak }

        val days = (6L downTo 0L).map { today.minusDays(it) }
        val focusByDay = sessions.groupBy { Instant.ofEpochMilli(it.startedAtMs).atZone(zone).toLocalDate() }
        val completedByDay = tasks.filter { it.completedAtMs != null }
            .groupBy { Instant.ofEpochMilli(it.completedAtMs!!).atZone(zone).toLocalDate() }
        val last7 = days.map { d ->
            DayActivity(
                date = d,
                tasksCompleted = completedByDay[d]?.size ?: 0,
                habitsCompleted = active.count { d in completions[it.id].orEmpty() },
                focusSeconds = focusByDay[d]?.sumOf { it.actualSeconds } ?: 0,
            )
        }

        return InsightsSummary(
            totalTasks = total,
            completedTasks = completed,
            pendingTasks = total - completed,
            overdueTasks = tasks.count { TaskRules.isOverdue(it, now) },
            taskCompletionPercent = if (total == 0) null else completed * 100 / total,
            habitConsistencyPercent = if (scheduled == 0) null else done * 100 / scheduled,
            habitStreaks = streaks,
            totalFocusSeconds = sessions.sumOf { it.actualSeconds.toLong() },
            completedFocusSessions = sessions.count { it.completed },
            focusSecondsLast7Days = last7.sumOf { it.focusSeconds },
            last7Days = last7,
        )
    }
}
