package com.chandan.master.presentation.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.logic.DayStatus
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.presentation.components.EmptyState
import com.chandan.master.presentation.theme.MasterTheme
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun scheduleSummary(mask: Int): String =
    if (mask == WeekdayMask.ALL) stringResource(R.string.habit_every_day)
    else WeekdayMask.days(mask).joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }

@Composable
fun HabitsScreen(onAddHabit: () -> Unit, onOpenHabit: (Long) -> Unit, viewModel: HabitsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Add, stringResource(R.string.habit_add)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(horizontal = Spacing.screenHorizontal)) {
            Text(
                stringResource(R.string.nav_habits), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm).semantics { heading() },
            )
            if (state.hasArchived || state.showArchived) {
                Row(Modifier.padding(bottom = Spacing.sm)) {
                    FilterChip(selected = state.showArchived, onClick = { viewModel.setShowArchived(!state.showArchived) }, label = { Text(stringResource(R.string.habit_show_archived)) })
                }
            }
            if (state.loaded && state.items.isEmpty()) {
                EmptyState(Icons.Outlined.Repeat, stringResource(R.string.habits_empty_title), stringResource(R.string.habits_empty_message))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md), contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(state.items, key = { it.habit.id }) { item -> HabitCard(item, viewModel, onOpenHabit) }
                }
            }
        }
    }
}

@Composable
private fun HabitCard(item: HabitItem, viewModel: HabitsViewModel, onOpen: (Long) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.habit_edit)) { onOpen(item.habit.id) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.habit.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(scheduleSummary(item.habit.scheduleMask), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                stringResource(R.string.habit_stats, item.stats.currentStreak, item.stats.longestStreak, item.stats.completionPercent?.let { "$it%" } ?: "–"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            )
            WeekRow(item, viewModel)
            Text(stringResource(R.string.habit_week_progress, item.weekDone, item.weekScheduled), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!item.habit.archived) {
                if (item.doneToday) OutlinedButton(onClick = { viewModel.toggleToday(item) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.habit_undo_today)) }
                else Button(onClick = { viewModel.toggleToday(item) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.habit_done_today)) }
            }
        }
    }
}

@Composable
private fun WeekRow(item: HabitItem, viewModel: HabitsViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        item.week.forEach { day -> DayDot(day) { viewModel.toggleDay(item, day.date) } }
    }
}

@Composable
private fun DayDot(day: DayStatus, onClick: () -> Unit) {
    val letter = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
    val state = stringResource(
        when { day.completed -> R.string.day_completed; day.isFuture -> R.string.day_upcoming; day.scheduled -> R.string.day_not_done; else -> R.string.day_not_scheduled },
    )
    val description = "${formatDate(day.date)}, $state"
    val primary = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { contentDescription = description; role = Role.Checkbox }) {
        Text(letter, style = MaterialTheme.typography.labelSmall, color = if (day.isToday) primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier.padding(top = 4.dp).size(36.dp).clip(CircleShape)
                .background(if (day.completed) primary else MaterialTheme.colorScheme.background)
                .border(
                    if (day.isToday) 2.dp else 1.dp,
                    when { day.completed -> primary; day.isToday -> primary; day.scheduled -> MaterialTheme.colorScheme.outline; else -> MaterialTheme.colorScheme.outlineVariant },
                    CircleShape,
                )
                .clickable(enabled = !day.isFuture, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (day.completed) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
    }
}
