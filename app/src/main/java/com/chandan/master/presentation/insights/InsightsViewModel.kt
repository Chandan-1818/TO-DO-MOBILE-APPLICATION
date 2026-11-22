package com.chandan.master.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.domain.logic.InsightsCalculator
import com.chandan.master.domain.logic.InsightsSummary
import com.chandan.master.domain.repository.FocusRepository
import com.chandan.master.domain.repository.HabitRepository
import com.chandan.master.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Every number comes from [InsightsCalculator] over the real database rows; nothing is sampled or invented. */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    tasks: TaskRepository,
    habits: HabitRepository,
    focus: FocusRepository,
    private val clock: DateTimeProvider,
) : ViewModel() {
    val summary: StateFlow<InsightsSummary?> = combine(tasks.observeTasks(), habits.observeHabits(), habits.observeCompletions(), focus.observeSessions()) { t, h, c, s ->
        InsightsCalculator.calculate(t, h, c, s, clock.nowLocal(), clock.zone())
    }.map<InsightsSummary, InsightsSummary?> { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
