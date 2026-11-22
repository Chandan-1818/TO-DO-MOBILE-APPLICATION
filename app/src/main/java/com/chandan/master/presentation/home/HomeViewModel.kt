package com.chandan.master.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.domain.logic.TaskRules
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.domain.model.Greeting
import com.chandan.master.domain.model.GreetingResolver
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.FocusRepository
import com.chandan.master.domain.repository.HabitRepository
import com.chandan.master.domain.repository.TaskRepository
import com.chandan.master.domain.usecase.ObserveActiveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val greeting: Greeting,
    /** Username of the active local profile, or null while it loads. */
    val displayName: String?,
    val avatarPath: String? = null,
    val today: LocalDate,
    val now: LocalDateTime = LocalDateTime.now(),
    /** Open tasks due today plus overdue ones, most urgent first. */
    val focusTasks: List<Task> = emptyList(),
    val tasksDueToday: Int = 0,
    val completedToday: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val habitsScheduledToday: Int = 0,
    val habitsDoneToday: Int = 0,
    val focusSecondsToday: Long = 0,
    val loaded: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeProfile: ObserveActiveProfileUseCase,
    private val tasks: TaskRepository,
    habits: HabitRepository,
    focus: FocusRepository,
    private val clock: DateTimeProvider,
) : ViewModel() {
    /** Emits once a minute while the screen is visible so the greeting, date and "overdue" never go stale. */
    private val minuteTicker = flow { while (true) { emit(Unit); delay(60_000) } }

    val uiState: StateFlow<HomeUiState> = combine(
        observeProfile(), tasks.observeTasks(), habits.observeHabits(), habits.observeCompletions(), combine(focus.observeSessions(), minuteTicker) { s, _ -> s },
    ) { profile, allTasks, allHabits, completions, sessions ->
        val now = clock.nowLocal()
        val today = now.toLocalDate()
        val dueToday = allTasks.filter { it.dueDate == today }
        val scheduled = allHabits.filter { !it.archived && !today.isBefore(it.startDate) && WeekdayMask.isScheduled(it.scheduleMask, today) }
        HomeUiState(
            greeting = GreetingResolver.resolve(now.toLocalTime()), displayName = profile?.username, avatarPath = profile?.avatarUri, today = today, now = now,
            focusTasks = allTasks.filter { !it.completed && (it.dueDate == today || TaskRules.isOverdue(it, now)) }
                .sortedWith(compareByDescending<Task> { TaskRules.isOverdue(it, now) }.thenBy { it.dueMinuteOfDay ?: Int.MAX_VALUE }.thenByDescending { it.priority.level }).take(8),
            tasksDueToday = dueToday.size, completedToday = dueToday.count { it.completed },
            pendingTasks = allTasks.count { !it.completed }, overdueTasks = allTasks.count { TaskRules.isOverdue(it, now) },
            habitsScheduledToday = scheduled.size, habitsDoneToday = scheduled.count { today in completions[it.id].orEmpty() },
            focusSecondsToday = sessions.filter { Instant.ofEpochMilli(it.startedAtMs).atZone(clock.zone()).toLocalDate() == today }.sumOf { it.actualSeconds.toLong() },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial())

    private fun initial() = HomeUiState(GreetingResolver.resolve(clock.nowLocal().toLocalTime()), null, null, clock.today())

    fun toggle(task: Task) { viewModelScope.launch { tasks.setCompleted(task.id, !task.completed) } }
}
