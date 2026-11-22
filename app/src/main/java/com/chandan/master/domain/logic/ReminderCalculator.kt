package com.chandan.master.domain.logic

import com.chandan.master.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ReminderCalculator {
    /** Default time for reminders on tasks that have a date but no time. */
    const val DEFAULT_MINUTE_OF_DAY = 9 * 60
    val OFFSET_CHOICES = listOf(0, 10, 60, 1440)

    /** When to notify for [task], or null when it has no reminder, no due date, is completed, or the moment already passed. */
    fun taskTrigger(task: Task, zone: ZoneId, now: Instant): Instant? {
        val offset = task.reminderOffsetMinutes ?: return null
        val date = task.dueDate ?: return null
        if (task.completed) return null
        val minute = task.dueMinuteOfDay ?: DEFAULT_MINUTE_OF_DAY
        val trigger = date.atTime(minute / 60, minute % 60).atZone(zone).toInstant().minusSeconds(offset * 60L)
        return if (trigger.isAfter(now)) trigger else null
    }

    /** The next time a daily/weekly habit reminder should fire strictly after [now], or null if the schedule is empty. */
    fun nextHabitTrigger(mask: Int, minuteOfDay: Int, startDate: LocalDate, zone: ZoneId, now: Instant): Instant? {
        if (!WeekdayMask.isValid(mask)) return null
        val today = now.atZone(zone).toLocalDate()
        var day = if (today.isBefore(startDate)) startDate else today
        repeat(9) {
            if (WeekdayMask.isScheduled(mask, day)) {
                val t = day.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(zone).toInstant()
                if (t.isAfter(now)) return t
            }
            day = day.plusDays(1)
        }
        return null
    }
}
