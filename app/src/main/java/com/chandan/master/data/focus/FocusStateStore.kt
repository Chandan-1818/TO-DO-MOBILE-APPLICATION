package com.chandan.master.data.focus

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chandan.master.di.SettingsStore
import com.chandan.master.domain.logic.SessionKind
import com.chandan.master.domain.logic.TimerPhase
import com.chandan.master.domain.logic.TimerState
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Persists the running timer so it survives the app being backgrounded, killed or the device restarting. */
@Singleton
class FocusStateStore @Inject constructor(@SettingsStore private val dataStore: DataStore<Preferences>) {
    val state: Flow<TimerState> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toState() }
        .distinctUntilChanged()

    suspend fun current(): TimerState = state.first()

    suspend fun save(s: TimerState) {
        dataStore.edit {
            it[PHASE] = s.phase.name; it[KIND] = s.kind.name; it[PLANNED] = s.plannedSeconds
            it[STARTED] = s.startedAtMs; it[ENDS] = s.endsAtMs; it[REMAINING] = s.remainingSecondsWhenPaused; it[COMPLETED] = s.completedFocusSessions
        }
    }

    suspend fun clear() = save(TimerState())

    private fun Preferences.toState() = TimerState(
        phase = runCatching { TimerPhase.valueOf(this[PHASE] ?: "IDLE") }.getOrDefault(TimerPhase.IDLE),
        kind = runCatching { SessionKind.valueOf(this[KIND] ?: "FOCUS") }.getOrDefault(SessionKind.FOCUS),
        plannedSeconds = this[PLANNED] ?: 0, startedAtMs = this[STARTED] ?: 0L, endsAtMs = this[ENDS] ?: 0L,
        remainingSecondsWhenPaused = this[REMAINING] ?: 0, completedFocusSessions = this[COMPLETED] ?: 0,
    )

    private companion object {
        val PHASE = stringPreferencesKey("focus_phase"); val KIND = stringPreferencesKey("focus_kind")
        val PLANNED = intPreferencesKey("focus_planned"); val STARTED = longPreferencesKey("focus_started_at")
        val ENDS = longPreferencesKey("focus_ends_at"); val REMAINING = intPreferencesKey("focus_remaining")
        val COMPLETED = intPreferencesKey("focus_completed_count")
    }
}
