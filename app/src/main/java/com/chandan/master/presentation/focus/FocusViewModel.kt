package com.chandan.master.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.focus.FocusTimerController
import com.chandan.master.domain.logic.FocusSettings
import com.chandan.master.domain.logic.FocusTimerLogic
import com.chandan.master.domain.logic.SessionKind
import com.chandan.master.domain.logic.TimerPhase
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.repository.FocusRepository
import com.chandan.master.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val kind: SessionKind = SessionKind.FOCUS,
    val remainingSeconds: Int = 0,
    val plannedSeconds: Int = 0,
    val completedFocusSessions: Int = 0,
    val settings: FocusSettings = FocusSettings(),
    val history: List<FocusSession> = emptyList(),
    val todayFocusSeconds: Long = 0,
) {
    val progress: Float get() = if (plannedSeconds == 0) 0f else 1f - remainingSeconds.toFloat() / plannedSeconds
    /** What the dial shows when idle: the full length of the selected session. */
    val displaySeconds: Int get() = if (phase == TimerPhase.IDLE) settings.secondsFor(kind) else remainingSeconds
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val controller: FocusTimerController,
    sessions: FocusRepository,
    private val prefs: PreferencesRepository,
    private val clock: DateTimeProvider,
) : ViewModel() {
    private val ticker = flow { while (true) { emit(Unit); delay(500) } }

    val uiState: StateFlow<FocusUiState> = combine(controller.state, prefs.preferences.map { it.focusSettings }, sessions.observeSessions(), ticker) { t, settings, history, _ ->
        val now = clock.now().toEpochMilli()
        val today = clock.today()
        val todaySeconds = history.filter { Instant.ofEpochMilli(it.startedAtMs).atZone(clock.zone()).toLocalDate() == today }.sumOf { it.actualSeconds.toLong() }
        FocusUiState(
            phase = t.phase, kind = t.kind, remainingSeconds = FocusTimerLogic.remainingSeconds(t, now), plannedSeconds = t.plannedSeconds,
            completedFocusSessions = t.completedFocusSessions, settings = settings, history = history.take(10), todayFocusSeconds = todaySeconds,
        ) to FocusTimerLogic.isFinished(t, now)
    }.onEach { (_, finished) -> if (finished) controller.checkFinished(notify = false) } // complete the session if its time is up
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    fun start() { viewModelScope.launch { controller.start() } }
    fun pause() { viewModelScope.launch { controller.pause() } }
    fun resume() { viewModelScope.launch { controller.resume() } }
    fun stop() { viewModelScope.launch { controller.stop() } }
    fun reset() { viewModelScope.launch { controller.reset() } }
    fun selectKind(k: SessionKind) { viewModelScope.launch { controller.selectKind(k) } }

    fun setMinutes(kind: SessionKind, minutes: Int) {
        val m = minutes.coerceIn(FocusSettings.MINUTES_RANGE)
        val s = uiState.value.settings
        viewModelScope.launch {
            prefs.setFocusSettings(
                when (kind) { SessionKind.FOCUS -> s.copy(focusMinutes = m); SessionKind.SHORT_BREAK -> s.copy(shortBreakMinutes = m); SessionKind.LONG_BREAK -> s.copy(longBreakMinutes = m) },
            )
        }
    }
}
