package com.chandan.master.logic

import com.chandan.master.domain.logic.FocusSettings
import com.chandan.master.domain.logic.FocusTimerLogic
import com.chandan.master.domain.logic.SessionKind
import com.chandan.master.domain.logic.TimerPhase
import com.chandan.master.domain.logic.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerLogicTest {
    private val s = FocusSettings()
    private val t0 = 1_000_000L
    private fun running() = FocusTimerLogic.start(TimerState(), SessionKind.FOCUS, s, t0)

    @Test fun startSetsEndTime() {
        val r = running()
        assertEquals(TimerPhase.RUNNING, r.phase); assertEquals(1500, r.plannedSeconds); assertEquals(t0 + 1_500_000, r.endsAtMs)
        assertEquals(1500, FocusTimerLogic.remainingSeconds(r, t0))
        assertEquals(1500, FocusTimerLogic.remainingSeconds(r, t0 + 1)) // rounds up, so the display never shows 0 early
        assertEquals(1499, FocusTimerLogic.remainingSeconds(r, t0 + 1000))
    }

    @Test fun remainingIsDerivedFromTheClockSoBackgroundingCannotDriftIt() {
        val r = running()
        assertEquals(900, FocusTimerLogic.remainingSeconds(r, t0 + 600_000))
        assertEquals(0, FocusTimerLogic.remainingSeconds(r, t0 + 99_999_999))
    }

    @Test fun pauseFreezesTheRemainingTimeAndResumeContinuesFromIt() {
        val p = FocusTimerLogic.pause(running(), t0 + 600_000)
        assertEquals(TimerPhase.PAUSED, p.phase); assertEquals(900, p.remainingSecondsWhenPaused)
        assertEquals(900, FocusTimerLogic.remainingSeconds(p, t0 + 5_000_000)) // time passing while paused changes nothing
        val r = FocusTimerLogic.resume(p, t0 + 5_000_000)
        assertEquals(TimerPhase.RUNNING, r.phase); assertEquals(t0 + 5_000_000 + 900_000, r.endsAtMs)
    }

    @Test fun pauseAndResumeAreNoOpsInTheWrongPhase() {
        val idle = TimerState()
        assertEquals(idle, FocusTimerLogic.pause(idle, t0)); assertEquals(idle, FocusTimerLogic.resume(idle, t0))
        val r = running(); assertEquals(r, FocusTimerLogic.resume(r, t0))
    }

    @Test fun finishedSessionIsRecordedCompleteAndCountsTowardsTheCycle() {
        val (next, rec) = FocusTimerLogic.finish(running(), t0 + 1_500_000, s)!!
        assertTrue(rec.completed); assertEquals(1500, rec.actualSeconds); assertEquals(t0, rec.startedAtMs)
        assertEquals(TimerPhase.IDLE, next.phase); assertEquals(1, next.completedFocusSessions); assertEquals(SessionKind.SHORT_BREAK, next.kind)
    }

    @Test fun stoppingEarlyRecordsOnlyTheElapsedTimeAndDoesNotCountAsCompleted() {
        val (next, rec) = FocusTimerLogic.finish(running(), t0 + 600_000, s)!!
        assertFalse(rec.completed); assertEquals(600, rec.actualSeconds)
        assertEquals(0, next.completedFocusSessions); assertEquals(SessionKind.FOCUS, next.kind)
    }

    @Test fun stoppingWhilePausedUsesTheFrozenElapsedTime() {
        val p = FocusTimerLogic.pause(running(), t0 + 300_000)
        val (_, rec) = FocusTimerLogic.finish(p, t0 + 99_000_000, s)!!
        assertEquals(300, rec.actualSeconds); assertFalse(rec.completed)
    }

    @Test fun longBreakAfterEveryFourthFocusAndFocusAfterBreaks() {
        var state = TimerState()
        val kinds = ArrayList<SessionKind>()
        var now = t0
        repeat(4) {
            state = FocusTimerLogic.start(state, SessionKind.FOCUS, s, now); now += 1_500_000
            state = FocusTimerLogic.finish(state, now, s)!!.first; kinds += state.kind
            if (it < 3) { state = FocusTimerLogic.start(state, state.kind, s, now); now += 300_000; state = FocusTimerLogic.finish(state, now, s)!!.first; kinds += state.kind }
        }
        assertEquals(listOf(SessionKind.SHORT_BREAK, SessionKind.FOCUS, SessionKind.SHORT_BREAK, SessionKind.FOCUS, SessionKind.SHORT_BREAK, SessionKind.FOCUS, SessionKind.LONG_BREAK), kinds)
    }

    @Test fun finishingAnIdleTimerDoesNothingAndFinishedDetection() {
        assertNull(FocusTimerLogic.finish(TimerState(), t0, s))
        assertTrue(FocusTimerLogic.isFinished(running(), t0 + 1_500_000)); assertFalse(FocusTimerLogic.isFinished(running(), t0 + 1_499_999))
    }

    @Test fun customDurations() {
        val custom = FocusSettings(focusMinutes = 50, shortBreakMinutes = 10, longBreakMinutes = 30, sessionsBeforeLongBreak = 2)
        assertEquals(3000, custom.secondsFor(SessionKind.FOCUS)); assertEquals(600, custom.secondsFor(SessionKind.SHORT_BREAK)); assertEquals(1800, custom.secondsFor(SessionKind.LONG_BREAK))
        assertEquals(SessionKind.LONG_BREAK, FocusTimerLogic.nextKind(SessionKind.FOCUS, 2, true, custom))
    }
}
