package com.chandan.master.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockoutAndAutoLockTest {
    private val now = 1_000_000_000L

    @Test fun firstFiveWrongAttemptsAreFree() {
        var s = AttemptState()
        repeat(4) { s = AttemptTracker.onFailure(s, now); assertEquals(0L, AttemptTracker.remainingLockMillis(s, now)) }
        assertEquals(4, s.failedAttempts)
        assertEquals(1, LockoutPolicy.freeAttemptsLeft(s.failedAttempts))
    }

    @Test fun delaysEscalateAndNeverBecomePermanent() {
        val delays = (1..12).map { LockoutPolicy.delayMillisAfter(it) }
        assertEquals(listOf(0L, 0L, 0L, 0L, 30_000L, 60_000L, 300_000L, 900_000L), delays.take(8))
        assertTrue(delays.drop(8).all { it == LockoutPolicy.MAX_DELAY_MILLIS })
        assertTrue(delays.max() <= 30 * 60_000L)
    }

    @Test fun fifthFailureStartsA30SecondDelayThatExpires() {
        var s = AttemptState()
        repeat(5) { s = AttemptTracker.onFailure(s, now) }
        assertEquals(30_000L, AttemptTracker.remainingLockMillis(s, now))
        assertEquals(1_000L, AttemptTracker.remainingLockMillis(s, now + 29_000))
        assertEquals(0L, AttemptTracker.remainingLockMillis(s, now + 30_000))
        assertEquals(0L, AttemptTracker.remainingLockMillis(s, now + 99_000))
    }

    @Test fun successResetsEverything() {
        var s = AttemptState()
        repeat(7) { s = AttemptTracker.onFailure(s, now) }
        assertEquals(AttemptState(), AttemptTracker.onSuccess())
    }

    @Test fun clockMovedBackwardsCannotCauseLongerThanMaxLock() {
        val s = AttemptState(failedAttempts = 9, lockedUntilEpochMillis = now + 10L * 24 * 3_600_000)
        assertEquals(LockoutPolicy.MAX_DELAY_MILLIS, AttemptTracker.remainingLockMillis(s, now))
    }

    @Test fun autoLockDelays() {
        val bg = 5_000L
        assertTrue(AutoLockPolicy.shouldLock(bg, bg, AutoLockDelay.IMMEDIATELY))
        assertFalse(AutoLockPolicy.shouldLock(bg, bg + 29_999, AutoLockDelay.SECONDS_30))
        assertTrue(AutoLockPolicy.shouldLock(bg, bg + 30_000, AutoLockDelay.SECONDS_30))
        assertFalse(AutoLockPolicy.shouldLock(bg, bg + 59_999, AutoLockDelay.MINUTE_1))
        assertTrue(AutoLockPolicy.shouldLock(bg, bg + 5 * 60_000, AutoLockDelay.MINUTES_5))
        assertFalse(AutoLockPolicy.shouldLock(bg, bg + 5 * 60_000 - 1, AutoLockDelay.MINUTES_5))
    }

    @Test fun neverBackgroundedMeansNoLockAndRebootAlwaysLocks() {
        assertFalse(AutoLockPolicy.shouldLock(null, 10, AutoLockDelay.IMMEDIATELY))
        assertTrue(AutoLockPolicy.shouldLock(9_000_000, 10, AutoLockDelay.MINUTES_5))
    }

    @Test fun autoLockStorageFallsBackToDefault() {
        AutoLockDelay.entries.forEach { assertEquals(it, AutoLockDelay.fromStorageValue(it.storageValue)) }
        assertEquals(AutoLockDelay.MINUTE_1, AutoLockDelay.fromStorageValue("never"))
        assertEquals(AutoLockDelay.MINUTE_1, AutoLockDelay.fromStorageValue(null))
    }
}
