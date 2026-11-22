package com.chandan.master.data.focus

import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.notifications.AppNotifier
import com.chandan.master.domain.logic.FocusTimerLogic
import com.chandan.master.domain.logic.SessionKind
import com.chandan.master.domain.logic.TimerPhase
import com.chandan.master.domain.logic.TimerState
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.repository.FocusRepository
import com.chandan.master.domain.repository.PreferencesRepository
import com.chandan.master.domain.repository.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates the focus timer. The pure rules live in [FocusTimerLogic]; this class persists state, records
 * sessions, schedules a WorkManager job to finish the session if the app is in the background, and notifies.
 * All transitions are serialised, and recording is idempotent, so the UI and the background job cannot double-count.
 */
@Singleton
class FocusTimerController @Inject constructor(
    private val store: FocusStateStore,
    private val sessions: FocusRepository,
    private val prefs: PreferencesRepository,
    private val clock: DateTimeProvider,
    private val scheduler: ReminderScheduler,
    private val notifier: AppNotifier,
) {
    val state: Flow<TimerState> = store.state
    private val mutex = Mutex()
    private fun nowMs() = clock.now().toEpochMilli()

    /** Chooses what the next session will be (only while idle). */
    suspend fun selectKind(kind: SessionKind) = mutex.withLock {
        val s = store.current(); if (s.phase == TimerPhase.IDLE) store.save(s.copy(kind = kind))
    }

    suspend fun start() = mutex.withLock {
        val s = store.current()
        if (s.phase != TimerPhase.IDLE) return@withLock
        val settings = prefs.preferences.first().focusSettings
        val n = FocusTimerLogic.start(s, s.kind, settings, nowMs())
        store.save(n)
        scheduler.scheduleFocusFinish(n.endsAtMs - nowMs())
    }

    suspend fun pause() = mutex.withLock {
        val n = FocusTimerLogic.pause(store.current(), nowMs())
        store.save(n); scheduler.cancelFocusFinish()
    }

    suspend fun resume() = mutex.withLock {
        val n = FocusTimerLogic.resume(store.current(), nowMs())
        store.save(n)
        if (n.phase == TimerPhase.RUNNING) scheduler.scheduleFocusFinish(n.endsAtMs - nowMs())
    }

    /** Stops early. A focus session of at least a minute is still recorded (as not completed). */
    suspend fun stop() = mutex.withLock { finishLocked(notify = false) }

    /** Throws the current session away without recording it. */
    suspend fun reset() = mutex.withLock {
        val s = store.current()
        scheduler.cancelFocusFinish()
        store.save(TimerState(kind = s.kind, completedFocusSessions = s.completedFocusSessions))
    }

    /** Completes the session if its time is up. Called by the UI (notify = false) and the background job (notify = true). */
    suspend fun checkFinished(notify: Boolean) = mutex.withLock {
        val s = store.current()
        if (FocusTimerLogic.isFinished(s, nowMs())) finishLocked(notify)
    }

    private suspend fun finishLocked(notify: Boolean) {
        val s = store.current()
        val settings = prefs.preferences.first().focusSettings
        val (next, record) = FocusTimerLogic.finish(s, nowMs(), settings) ?: return
        scheduler.cancelFocusFinish()
        store.save(next)
        if (record.kind == SessionKind.FOCUS && (record.completed || record.actualSeconds >= FocusTimerLogic.MIN_RECORDED_SECONDS)) {
            sessions.record(FocusSession(startedAtMs = record.startedAtMs, plannedSeconds = record.plannedSeconds, actualSeconds = record.actualSeconds, completed = record.completed))
        }
        if (notify && record.completed) notifier.notifyFocusFinished(record.kind)
    }
}
