package com.chandan.master.presentation.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val description: String = "",
    val mask: Int = WeekdayMask.ALL,
    val startDate: LocalDate = LocalDate.now(),
    val reminderMinute: Int? = null,
    val archived: Boolean = false,
    val showNameError: Boolean = false,
    val finished: Boolean = false,
) {
    val nameInvalid: Boolean get() = name.trim().isEmpty()
    val scheduleInvalid: Boolean get() = !WeekdayMask.isValid(mask)
}

@HiltViewModel
class HabitEditViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val clock: DateTimeProvider,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val habitId: Long = savedState.get<Long>("habitId") ?: -1L
    private val _state = MutableStateFlow(HabitEditUiState())
    val state: StateFlow<HabitEditUiState> = _state.asStateFlow()
    private var original: Habit? = null

    init {
        viewModelScope.launch {
            if (habitId > 0) {
                val h = habits.getHabit(habitId)
                if (h == null) { _state.update { it.copy(loaded = true, finished = true) }; return@launch }
                original = h
                _state.value = HabitEditUiState(true, false, h.name, h.description, h.scheduleMask, h.startDate, h.reminderMinuteOfDay, h.archived)
            } else {
                _state.value = HabitEditUiState(loaded = true, startDate = clock.today())
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(100), showNameError = false) }
    fun onDescription(v: String) = _state.update { it.copy(description = v.take(500)) }
    fun onToggleDay(day: DayOfWeek) = _state.update { it.copy(mask = it.mask xor WeekdayMask.bit(day)) }
    fun onEveryDay() = _state.update { it.copy(mask = WeekdayMask.ALL) }
    fun onWeekdays() = _state.update { it.copy(mask = WeekdayMask.WEEKDAYS) }
    fun onStartDate(d: LocalDate) = _state.update { it.copy(startDate = d) }
    fun onReminder(minute: Int?) = _state.update { it.copy(reminderMinute = minute) }

    fun save() {
        val s = _state.value
        if (s.nameInvalid || s.scheduleInvalid) { _state.update { it.copy(showNameError = true) }; return }
        viewModelScope.launch {
            habits.save((original ?: Habit(name = "", startDate = s.startDate)).copy(
                name = s.name, description = s.description, scheduleMask = s.mask, reminderMinuteOfDay = s.reminderMinute, startDate = s.startDate,
            ))
            _state.update { it.copy(finished = true) }
        }
    }

    fun setArchived(archived: Boolean) {
        val id = original?.id ?: return
        viewModelScope.launch { habits.setArchived(id, archived); _state.update { it.copy(finished = true) } }
    }

    fun delete() {
        val id = original?.id ?: return
        viewModelScope.launch { habits.delete(id); _state.update { it.copy(finished = true) } }
    }
}
