package com.chandan.master.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLockManagerTest {
    @Test fun startsLockedSoAColdStartAlwaysAsksForThePin() {
        assertTrue(SessionLockManager().isLocked.value)
    }

    @Test fun unlockThenQuickReturnStaysUnlocked() {
        val m = SessionLockManager(); m.unlock()
        m.onAppBackgrounded(1_000); m.onAppForegrounded(10_000, AutoLockDelay.MINUTE_1)
        assertFalse(m.isLocked.value)
    }

    @Test fun returningAfterTheDelayLocks() {
        val m = SessionLockManager(); m.unlock()
        m.onAppBackgrounded(1_000); m.onAppForegrounded(61_000, AutoLockDelay.MINUTE_1)
        assertTrue(m.isLocked.value)
    }

    @Test fun immediatelyLocksOnEveryReturn() {
        val m = SessionLockManager(); m.unlock()
        m.onAppBackgrounded(5); m.onAppForegrounded(5, AutoLockDelay.IMMEDIATELY)
        assertTrue(m.isLocked.value)
    }

    @Test fun repeatedStopsKeepTheFirstBackgroundTimeSoTheDelayCannotBeExtended() {
        val m = SessionLockManager(); m.unlock()
        m.onAppBackgrounded(0); m.onAppBackgrounded(50_000); m.onAppForegrounded(61_000, AutoLockDelay.MINUTE_1)
        assertTrue(m.isLocked.value)
    }

    @Test fun lockNowWorksAndUnlockClearsBackgroundTimestamp() {
        val m = SessionLockManager(); m.unlock(); m.lock(); assertTrue(m.isLocked.value)
        m.unlock(); m.onAppForegrounded(999_999, AutoLockDelay.IMMEDIATELY) // never backgrounded since unlock
        assertFalse(m.isLocked.value)
    }
}
