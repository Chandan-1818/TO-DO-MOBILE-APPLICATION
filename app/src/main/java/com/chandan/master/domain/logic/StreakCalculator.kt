package com.chandan.master.domain.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class HabitStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompleted: Int,
    val scheduledDays: Int,
    val completedScheduledDays: Int,
) {
    /** 0..100, or null when no scheduled day has elapsed yet. */
    val completionPercent: Int? get() = if (scheduledDays == 0) null else completedScheduledDays * 100 / scheduledDays
}

data class DayStatus(val date: LocalDate, val scheduled: Boolean, val completed: Boolean, val isFuture: Boolean, val isToday: Boolean)

/**
 * Streaks are computed only from SCHEDULED days:
 *  - an unscheduled day never breaks a streak (and a completion on one is a bonus that does not extend it);
 *  - a future day is never "missed";
 *  - today, if scheduled and not yet done, is pending: it neither counts nor breaks the streak;
 *  - dates before the habit's start date are ignored.
 * Because everything is recomputed from the stored completion dates, editing history recalculates correctly.
 */
object StreakCalculator {
    fun calculate(mask: Int, startDate: LocalDate, completedDates: Set<LocalDate>, today: LocalDate): HabitStats {
        val done = completedDates.filter { !it.isBefore(startDate) && !it.isAfter(today) }.toSet()
        if (startDate.isAfter(today)) return HabitStats(0, 0, done.size, 0, 0)

        var longest = 0
        var run = 0
        var scheduled = 0
        var completedScheduled = 0
        val days = ChronoUnit.DAYS.between(startDate, today)
        for (i in 0..days) {
            val d = startDate.plusDays(i)
            if (!WeekdayMask.isScheduled(mask, d)) continue
            if (d in done) {
                scheduled++; completedScheduled++; run++
                if (run > longest) longest = run
            } else if (d == today) {
                // pending today: not counted as scheduled-missed, does not break the run
            } else {
                scheduled++; run = 0
            }
        }

        var current = 0
        for (i in days downTo 0) {
            val d = startDate.plusDays(i)
            if (!WeekdayMask.isScheduled(mask, d)) continue
            if (d in done) current++
            else if (d == today) continue
            else break
        }
        return HabitStats(current, longest, done.size, scheduled, completedScheduled)
    }

    /** Seven consecutive days starting at [weekStart], with per-day status for the weekly progress row. */
    fun week(mask: Int, startDate: LocalDate, completedDates: Set<LocalDate>, weekStart: LocalDate, today: LocalDate): List<DayStatus> =
        (0L..6L).map { i ->
            val d = weekStart.plusDays(i)
            DayStatus(
                date = d,
                scheduled = WeekdayMask.isScheduled(mask, d) && !d.isBefore(startDate),
                completed = d in completedDates,
                isFuture = d.isAfter(today),
                isToday = d == today,
            )
        }
}
