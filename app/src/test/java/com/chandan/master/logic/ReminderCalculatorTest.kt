package com.chandan.master.logic

import com.chandan.master.domain.logic.ReminderCalculator
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.domain.model.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderCalculatorTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-03-10T04:00:00Z") // 09:30 IST
    private fun task(date: LocalDate?, minute: Int?, offset: Int?, done: Boolean = false) =
        Task(title = "t", dueDate = date, dueMinuteOfDay = minute, reminderOffsetMinutes = offset, completed = done)

    @Test fun reminderAtDueTimeAndBeforeIt() {
        val d = LocalDate.of(2026, 3, 10)
        assertEquals(Instant.parse("2026-03-10T12:30:00Z"), ReminderCalculator.taskTrigger(task(d, 18 * 60, 0), kolkata, now)) // 18:00 IST = 12:30Z
        assertEquals(Instant.parse("2026-03-10T11:30:00Z"), ReminderCalculator.taskTrigger(task(d, 18 * 60, 60), kolkata, now))
        assertEquals(Instant.parse("2026-03-09T12:30:00Z").plusSeconds(0), ReminderCalculator.taskTrigger(task(d.plusDays(0), 18 * 60, 1440), kolkata, Instant.parse("2026-03-08T00:00:00Z")))
    }

    @Test fun dateOnlyTasksRemindAtNineInTheMorning() {
        assertEquals(Instant.parse("2026-03-11T03:30:00Z"), ReminderCalculator.taskTrigger(task(LocalDate.of(2026, 3, 11), null, 0), kolkata, now))
    }

    @Test fun noReminderWhenNoneRequestedNoDateCompletedOrInThePast() {
        assertNull(ReminderCalculator.taskTrigger(task(LocalDate.of(2026, 3, 11), 600, null), kolkata, now))
        assertNull(ReminderCalculator.taskTrigger(task(null, 600, 0), kolkata, now))
        assertNull(ReminderCalculator.taskTrigger(task(LocalDate.of(2026, 3, 11), 600, 0, done = true), kolkata, now))
        assertNull(ReminderCalculator.taskTrigger(task(LocalDate.of(2026, 3, 10), 9 * 60, 0), kolkata, now)) // 09:00 already passed
    }

    @Test fun timeZoneChangesTheAbsoluteInstant() {
        val d = LocalDate.of(2026, 3, 11)
        val ist = ReminderCalculator.taskTrigger(task(d, 600, 0), kolkata, now)
        val utc = ReminderCalculator.taskTrigger(task(d, 600, 0), ZoneId.of("UTC"), now)
        assertEquals(Instant.parse("2026-03-11T04:30:00Z"), ist); assertEquals(Instant.parse("2026-03-11T10:00:00Z"), utc)
    }

    @Test fun habitReminderPicksNextScheduledDayStrictlyAfterNow() {
        val start = LocalDate.of(2026, 1, 1)
        // now = Tue 2026-03-10 09:30 IST; daily at 08:00 -> already passed today -> tomorrow
        assertEquals(Instant.parse("2026-03-11T02:30:00Z"), ReminderCalculator.nextHabitTrigger(WeekdayMask.ALL, 8 * 60, start, kolkata, now))
        // daily at 21:00 -> still today
        assertEquals(Instant.parse("2026-03-10T15:30:00Z"), ReminderCalculator.nextHabitTrigger(WeekdayMask.ALL, 21 * 60, start, kolkata, now))
        // Mondays only -> next Monday 2026-03-16
        val mondays = WeekdayMask.fromDays(listOf(DayOfWeek.MONDAY))
        assertEquals(Instant.parse("2026-03-16T02:30:00Z"), ReminderCalculator.nextHabitTrigger(mondays, 8 * 60, start, kolkata, now))
    }

    @Test fun habitReminderRespectsStartDateAndInvalidMask() {
        val start = LocalDate.of(2026, 3, 20)
        assertEquals(Instant.parse("2026-03-20T02:30:00Z"), ReminderCalculator.nextHabitTrigger(WeekdayMask.ALL, 8 * 60, start, kolkata, now))
        assertNull(ReminderCalculator.nextHabitTrigger(0, 480, start, kolkata, now))
    }
}
