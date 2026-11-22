package com.chandan.master.domain.logic

enum class SessionKind { FOCUS, SHORT_BREAK, LONG_BREAK }
enum class TimerPhase { IDLE, RUNNING, PAUSED }

data class FocusSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
) {
    fun secondsFor(kind: SessionKind): Int = 60 * when (kind) {
        SessionKind.FOCUS -> focusMinutes
        SessionKind.SHORT_BREAK -> shortBreakMinutes
        SessionKind.LONG_BREAK -> longBreakMinutes
    }
    companion object { val MINUTES_RANGE = 1..180 }
}

/** Persisted timer state. Times are wall-clock epoch milliseconds so the timer survives process death. */
data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val kind: SessionKind = SessionKind.FOCUS,
    val plannedSeconds: Int = 0,
    val startedAtMs: Long = 0,
    val endsAtMs: Long = 0,
    val remainingSecondsWhenPaused: Int = 0,
    val completedFocusSessions: Int = 0,
)

data class FinishedSession(val kind: SessionKind, val startedAtMs: Long, val plannedSeconds: Int, val actualSeconds: Int, val completed: Boolean)

object FocusTimerLogic {
    fun start(state: TimerState, kind: SessionKind, settings: FocusSettings, nowMs: Long): TimerState {
        require(state.phase == TimerPhase.IDLE) { "timer already active" }
        val seconds = settings.secondsFor(kind)
        return state.copy(phase = TimerPhase.RUNNING, kind = kind, plannedSeconds = seconds, startedAtMs = nowMs, endsAtMs = nowMs + seconds * 1000L)
    }

    fun pause(state: TimerState, nowMs: Long): TimerState =
        if (state.phase != TimerPhase.RUNNING) state
        else state.copy(phase = TimerPhase.PAUSED, remainingSecondsWhenPaused = remainingSeconds(state, nowMs), endsAtMs = 0)

    fun resume(state: TimerState, nowMs: Long): TimerState =
        if (state.phase != TimerPhase.PAUSED) state
        else state.copy(phase = TimerPhase.RUNNING, endsAtMs = nowMs + state.remainingSecondsWhenPaused * 1000L)

    fun remainingSeconds(state: TimerState, nowMs: Long): Int = when (state.phase) {
        TimerPhase.IDLE -> 0
        TimerPhase.PAUSED -> state.remainingSecondsWhenPaused
        TimerPhase.RUNNING -> ((state.endsAtMs - nowMs + 999) / 1000).toInt().coerceAtLeast(0)
    }

    fun isFinished(state: TimerState, nowMs: Long): Boolean = state.phase == TimerPhase.RUNNING && nowMs >= state.endsAtMs

    fun elapsedSeconds(state: TimerState, nowMs: Long): Int =
        (state.plannedSeconds - remainingSeconds(state, nowMs)).coerceIn(0, state.plannedSeconds)

    /** Completes a finished session, or stops an active one early. Returns the new idle state and what to record. */
    fun finish(state: TimerState, nowMs: Long, settings: FocusSettings): Pair<TimerState, FinishedSession>? {
        if (state.phase == TimerPhase.IDLE) return null
        val completed = state.phase == TimerPhase.RUNNING && nowMs >= state.endsAtMs
        val actual = if (completed) state.plannedSeconds else elapsedSeconds(state, nowMs)
        val record = FinishedSession(state.kind, state.startedAtMs, state.plannedSeconds, actual, completed)
        val count = if (completed && state.kind == SessionKind.FOCUS) state.completedFocusSessions + 1 else state.completedFocusSessions
        return TimerState(kind = nextKind(state.kind, count, completed, settings), completedFocusSessions = count) to record
    }

    /** After a completed focus: short break (long every Nth). After a break: focus. After an early stop: unchanged. */
    fun nextKind(finished: SessionKind, completedFocus: Int, completed: Boolean, settings: FocusSettings): SessionKind = when {
        !completed -> finished
        finished != SessionKind.FOCUS -> SessionKind.FOCUS
        completedFocus % settings.sessionsBeforeLongBreak == 0 -> SessionKind.LONG_BREAK
        else -> SessionKind.SHORT_BREAK
    }

    /** Focus sessions shorter than this are not worth recording when stopped early. */
    const val MIN_RECORDED_SECONDS = 60
}
