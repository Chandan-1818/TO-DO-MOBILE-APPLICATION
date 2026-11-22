package com.chandan.master.presentation.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

fun formatDate(date: LocalDate): String = dateFormatter.format(date)
fun formatMinuteOfDay(minute: Int): String = timeFormatter.format(LocalTime.of(minute / 60, minute % 60))

/** "1 h 05 min" / "25 min" / "0 min". */
fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    return if (minutes >= 60) "${minutes / 60} h ${"%02d".format(minutes % 60)} min" else "$minutes min"
}

fun formatClock(totalSeconds: Int): String = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
