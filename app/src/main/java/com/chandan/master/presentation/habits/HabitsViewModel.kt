package com.chandan.master.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.domain.logic.DayStatus
import com.chandan.master.domain.logic.HabitStats
import com.chandan.master.domain.logic.StreakCalculator
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitItem(
    val habit: Habit,
    val stats: HabitStats,
    val week: List<DayStatus>,
    val doneToday: Boolean,
    val scheduledToday: Boolean,
    val weekDone: Int,
    val weekScheduled: Int,
)

data class HabitsUiState(val items: List<HabitItem> = emptyList(), val loaded: Boolean = false, val showArchived: Boolean = false, val hasArchived: Boolean = false)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val clock: DateTimeProvider,
) : ViewModel() {
    private val showArchived = MutableStateFlow(false)
    /** Re-evaluates "today" shortly after midnight while the screen is visible. */
    private val ticker = flow { while (true) { emit(Unit); delay(60_000) } }

    val uiState: StateFlow<HabitsUiState> = combine(habits.observeHabits(), habits.observeCompletions(), showArchived, ticker) { all, completions, archived, _ ->
        val today = clock.today()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val items = all.filter { it.archived == archived }.sortedBy { it.name.lowercase() }.map { h ->
            val dates = completions[h.id].orEmpty()
            val week = StreakCalculator.week(h.scheduleMask, h.startDate, dates, weekStart, today)
            HabitItem(
                habit = h, stats = StreakCalculator.calculate(h.scheduleMask, h.startDate, dates, today), week = week,
                doneToday = today in dates, scheduledToday = WeekdayMask.isScheduled(h.scheduleMask, today) && !today.isBefore(h.startDate),
                weekDone = week.count { it.scheduled && it.completed }, weekScheduled = week.count { it.scheduled && !it.isFuture },
            )
        }
        HabitsUiState(items, loaded = true, showArchived = archived, hasArchived = all.any { it.archived })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun toggleDay(item: HabitItem, date: LocalDate) {
        if (date.isAfter(clock.today())) return // the future cannot be checked in
        viewModelScope.launch { habits.setCompletion(item.habit.id, date, !(item.week.firstOrNull { it.date == date }?.completed ?: false)) }
    }

    fun toggleToday(item: HabitItem) = toggleDay(item, clock.today())
    fun setShowArchived(v: Boolean) { showArchived.value = v }
}
