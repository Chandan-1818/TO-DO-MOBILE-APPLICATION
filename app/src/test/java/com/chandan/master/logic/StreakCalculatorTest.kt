package com.chandan.master.logic

import com.chandan.master.domain.logic.StreakCalculator
import com.chandan.master.domain.logic.WeekdayMask
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculatorTest {
    // 2026-03-02 is a Monday.
    private val mon = LocalDate.of(2026, 3, 2)
    private fun d(offset: Int) = mon.plusDays(offset.toLong())

    @Test fun dailyHabitStreakCountsConsecutiveDays() {
        val s = StreakCalculator.calculate(WeekdayMask.ALL, d(0), setOf(d(0), d(1), d(2)), d(2))
        assertEquals(3, s.currentStreak); assertEquals(3, s.longestStreak); assertEquals(100, s.completionPercent)
    }

    @Test fun todayNotYetDoneDoesNotBreakStreak() {
        val s = StreakCalculator.calculate(WeekdayMask.ALL, d(0), setOf(d(0), d(1)), d(2))
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.scheduledDays) // today is pending, not "missed"
        assertEquals(100, s.completionPercent)
    }

    @Test fun missedScheduledDayBreaksStreak() {
        val s = StreakCalculator.calculate(WeekdayMask.ALL, d(0), setOf(d(0), d(1), d(3)), d(3))
        assertEquals(1, s.currentStreak); assertEquals(2, s.longestStreak)
        assertEquals(3, s.completedScheduledDays); assertEquals(4, s.scheduledDays); assertEquals(75, s.completionPercent) // d(2) missed
    }

    @Test fun weekdayOnlyHabitIsNotBrokenByWeekend() {
        val mask = WeekdayMask.WEEKDAYS
        // done Thu, Fri, then Mon (weekend skipped)
        val done = setOf(d(3), d(4), d(7))
        val s = StreakCalculator.calculate(mask, d(0), done + setOf(d(0), d(1), d(2)), d(7))
        assertEquals(6, s.currentStreak)
    }

    @Test fun unscheduledDaysNeverCountAsMissed() {
        val mask = WeekdayMask.fromDays(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val s = StreakCalculator.calculate(mask, d(0), setOf(d(0), d(2), d(7)), d(9))
        assertEquals(3, s.currentStreak)
        assertEquals(100, s.completionPercent)
    }

    @Test fun completionOnUnscheduledDayIsBonusAndDoesNotExtendTheStreak() {
        val mask = WeekdayMask.fromDays(listOf(DayOfWeek.MONDAY))
        val s = StreakCalculator.calculate(mask, d(0), setOf(d(0), d(1)), d(2))
        assertEquals(1, s.currentStreak)
        assertEquals(2, s.totalCompleted)
    }

    @Test fun futureStartAndFutureCompletionsAreIgnored() {
        val s = StreakCalculator.calculate(WeekdayMask.ALL, d(5), setOf(d(6)), d(2))
        assertEquals(0, s.currentStreak); assertEquals(0, s.scheduledDays); assertNull(s.completionPercent)
        val s2 = StreakCalculator.calculate(WeekdayMask.ALL, d(0), setOf(d(0), d(10)), d(1))
        assertEquals(1, s2.totalCompleted)
    }

    @Test fun completionsBeforeStartDateAreIgnored() {
        val s = StreakCalculator.calculate(WeekdayMask.ALL, d(2), setOf(d(0), d(1), d(2)), d(2))
        assertEquals(1, s.currentStreak); assertEquals(1, s.totalCompleted)
    }

    @Test fun undoingACompletionRecalculatesEverything() {
        val all = setOf(d(0), d(1), d(2), d(3))
        assertEquals(4, StreakCalculator.calculate(WeekdayMask.ALL, d(0), all, d(3)).currentStreak)
        val undone = StreakCalculator.calculate(WeekdayMask.ALL, d(0), all - d(1), d(3))
        assertEquals(2, undone.currentStreak); assertEquals(2, undone.longestStreak)
    }

    @Test fun longestStreakSurvivesALaterBreak() {
        val s = StreakCalculator.calculate(WeekdayMask.ALL, d(0), setOf(d(0), d(1), d(2), d(3), d(5)), d(6))
        assertEquals(1, s.currentStreak) // d(5) done, d(6) pending today
        assertEquals(4, s.longestStreak)
    }

    @Test fun weekProgressRow() {
        val week = StreakCalculator.week(WeekdayMask.WEEKDAYS, d(0), setOf(d(0), d(1)), d(0), d(2))
        assertEquals(7, week.size)
        assertTrue(week[0].scheduled && week[0].completed && !week[0].isFuture)
        assertTrue(week[2].isToday && !week[2].completed)
        assertTrue(week[3].isFuture)
        assertFalse(week[5].scheduled) // Saturday
    }

    @Test fun maskHelpers() {
        assertEquals(127, WeekdayMask.ALL)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), WeekdayMask.days(WeekdayMask.fromDays(listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY))))
        assertFalse(WeekdayMask.isValid(0)); assertFalse(WeekdayMask.isValid(128)); assertTrue(WeekdayMask.isValid(1))
    }
}
