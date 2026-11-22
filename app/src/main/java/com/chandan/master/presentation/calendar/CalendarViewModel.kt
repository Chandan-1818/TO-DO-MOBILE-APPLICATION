package com.chandan.master.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.HabitRepository
import com.chandan.master.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val now: LocalDateTime = LocalDateTime.now(),
    /** Days that have at least one open task / any task, for the dots. */
    val daysWithOpenTasks: Set<LocalDate> = emptySet(),
    val daysWithTasks: Set<LocalDate> = emptySet(),
    val selectedTasks: List<Task> = emptyList(),
    val selectedHabits: List<Habit> = emptyList(),
    val habitsDoneOnSelected: Set<Long> = emptySet(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val tasks: TaskRepository,
    habits: HabitRepository,
    private val clock: DateTimeProvider,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(clock.today()))
    private val selected = MutableStateFlow(clock.today())

    val uiState: StateFlow<CalendarUiState> = combine(tasks.observeTasks(), habits.observeHabits(), habits.observeCompletions(), month, selected) { all, hs, completions, m, sel ->
        val dated = all.filter { it.dueDate != null }
        val onDay = dated.filter { it.dueDate == sel }
            .sortedWith(compareBy<Task> { it.completed }.thenBy { it.dueMinuteOfDay ?: Int.MAX_VALUE }.thenByDescending { it.priority.level })
        val scheduled = hs.filter { !it.archived && !sel.isBefore(it.startDate) && WeekdayMask.isScheduled(it.scheduleMask, sel) }.sortedBy { it.name.lowercase() }
        CalendarUiState(
            month = m, selected = sel, today = clock.today(), now = clock.nowLocal(),
            daysWithOpenTasks = dated.filter { !it.completed }.mapNotNull { it.dueDate }.toSet(),
            daysWithTasks = dated.mapNotNull { it.dueDate }.toSet(),
            selectedTasks = onDay, selectedHabits = scheduled,
            habitsDoneOnSelected = scheduled.filter { sel in completions[it.id].orEmpty() }.map { it.id }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun select(date: LocalDate) { selected.value = date; month.value = YearMonth.from(date) }
    fun goToday() = select(clock.today())
    fun toggleTask(task: Task) { viewModelScope.launch { tasks.setCompleted(task.id, !task.completed) } }
}
