package com.chandan.master.core

import com.chandan.master.core.time.SystemDateTimeProvider
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeProviderTest {
    // 2026-03-10T23:30:00Z is already 11 March in Kolkata/Auckland but still 10 March in UTC/New York.
    private val instant = Instant.parse("2026-03-10T23:30:00Z")

    @Test fun todayDependsOnZoneAtDateBoundary() {
        assertEquals(LocalDate.of(2026, 3, 10), FixedDateTimeProvider(instant, ZoneId.of("UTC")).today())
        assertEquals(LocalDate.of(2026, 3, 10), FixedDateTimeProvider(instant, ZoneId.of("America/New_York")).today())
        assertEquals(LocalDate.of(2026, 3, 11), FixedDateTimeProvider(instant, ZoneId.of("Asia/Kolkata")).today())
        assertEquals(LocalDate.of(2026, 3, 11), FixedDateTimeProvider(instant, ZoneId.of("Pacific/Auckland")).today())
    }

    @Test fun zoneChangeIsHonouredImmediately() {
        val p = FixedDateTimeProvider(instant, ZoneId.of("UTC"))
        assertEquals(LocalDate.of(2026, 3, 10), p.today())
        p.zoneId = ZoneId.of("Asia/Kolkata")
        assertEquals(LocalDate.of(2026, 3, 11), p.today())
    }

    @Test fun startOfDayMatchesLocalMidnight() {
        val p = FixedDateTimeProvider(instant, ZoneId.of("Asia/Kolkata"))
        // Kolkata is UTC+05:30, so local midnight of 2026-03-11 is 2026-03-10T18:30:00Z.
        assertEquals(Instant.parse("2026-03-10T18:30:00Z"), p.startOfDay(LocalDate.of(2026, 3, 11)))
    }

    @Test fun dayLengthIsCorrectAcrossDaylightSavingChange() {
        val p = FixedDateTimeProvider(instant, ZoneId.of("America/New_York"))
        val spring = LocalDate.of(2026, 3, 8) // US clocks go forward: 23-hour day
        val fall = LocalDate.of(2026, 11, 1)  // clocks go back: 25-hour day
        assertEquals(Duration.ofHours(23), Duration.between(p.startOfDay(spring), p.endOfDayExclusive(spring)))
        assertEquals(Duration.ofHours(25), Duration.between(p.startOfDay(fall), p.endOfDayExclusive(fall)))
    }

    @Test fun systemProviderReturnsPlausibleValues() {
        val p = SystemDateTimeProvider()
        assertTrue(Duration.between(p.now(), Instant.now()).abs() < Duration.ofSeconds(5))
        assertNotEquals(null, p.zone())
    }
}
