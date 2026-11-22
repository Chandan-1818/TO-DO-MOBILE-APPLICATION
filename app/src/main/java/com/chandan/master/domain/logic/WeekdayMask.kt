package com.chandan.master.domain.logic

import java.time.DayOfWeek
import java.time.LocalDate

/** Weekly schedule as a 7-bit mask: bit 0 = Monday ... bit 6 = Sunday. */
object WeekdayMask {
    const val ALL = 127
    const val WEEKDAYS = 31 // Mon-Fri

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    fun isScheduled(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0
    fun isScheduled(mask: Int, date: LocalDate): Boolean = isScheduled(mask, date.dayOfWeek)
    fun fromDays(days: Collection<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or bit(d) }
    fun days(mask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { isScheduled(mask, it) }
    fun isValid(mask: Int): Boolean = mask in 1..ALL
}
