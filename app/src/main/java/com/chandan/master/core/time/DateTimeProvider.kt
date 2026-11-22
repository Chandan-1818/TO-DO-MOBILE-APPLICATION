package com.chandan.master.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Single entry point for "what time is it" so that every feature (tasks, habits, focus,
 * analytics) uses the same clock and the same time zone, and so tests can control both.
 */
interface DateTimeProvider {
    fun now(): Instant
    fun zone(): ZoneId

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
    fun nowLocal(): LocalDateTime = now().atZone(zone()).toLocalDateTime()
    fun startOfDay(date: LocalDate): Instant = date.atStartOfDay(zone()).toInstant()
    fun endOfDayExclusive(date: LocalDate): Instant = date.plusDays(1).atStartOfDay(zone()).toInstant()
}

/** Production implementation. The zone is read on every call, so a device time-zone change is honoured. */
class SystemDateTimeProvider : DateTimeProvider {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
