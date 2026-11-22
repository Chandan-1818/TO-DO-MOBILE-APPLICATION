package com.chandan.master.presentation.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.model.Greeting
import com.chandan.master.presentation.components.ProfileAvatar
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.tasks.TaskRow
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatDuration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@StringRes
internal fun Greeting.labelRes(): Int = when (this) {
    Greeting.MORNING -> R.string.greeting_morning
    Greeting.AFTERNOON -> R.string.greeting_afternoon
    Greeting.EVENING -> R.string.greeting_evening
    Greeting.NIGHT -> R.string.greeting_night
}

class HomeActions(
    val onOpenProfile: () -> Unit, val onAddTask: () -> Unit, val onAddHabit: () -> Unit, val onOpenFocus: () -> Unit,
    val onAddNote: () -> Unit, val onOpenCalendar: () -> Unit, val onOpenNotes: () -> Unit, val onOpenTask: (Long) -> Unit,
)

@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, actions, viewModel::toggle)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeContent(state: HomeUiState, actions: HomeActions, onToggle: (com.chandan.master.domain.model.Task) -> Unit) {
    val dateText = remember(state.today) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(state.today) }
    val name = state.displayName ?: stringResource(R.string.home_guest_name)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.home_greeting_format, stringResource(state.greeting.labelRes()), name),
                    style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() },
                )
                Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val label = stringResource(R.string.home_open_profile)
            Surface(
                modifier = Modifier.sizeIn(minWidth = Spacing.minTouchTarget, minHeight = Spacing.minTouchTarget).clip(CircleShape)
                    .clickable(onClickLabel = label, role = Role.Button, onClick = actions.onOpenProfile),
                color = MaterialTheme.colorScheme.background,
            ) { ProfileAvatar(size = 48.dp, source = state.avatarPath) }
        }

        // Today's numbers, all read from the database.
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat(stringResource(R.string.home_stat_today), state.tasksDueToday.toString())
                    Stat(stringResource(R.string.home_stat_completed), state.completedToday.toString())
                    Stat(stringResource(R.string.home_stat_pending), state.pendingTasks.toString())
                    Stat(stringResource(R.string.home_stat_overdue), state.overdueTasks.toString())
                }
                ProgressLine(
                    stringResource(R.string.home_tasks_progress, state.completedToday, state.tasksDueToday),
                    if (state.tasksDueToday == 0) 0f else state.completedToday.toFloat() / state.tasksDueToday,
                )
                ProgressLine(
                    stringResource(R.string.home_habits_progress, state.habitsDoneToday, state.habitsScheduledToday),
                    if (state.habitsScheduledToday == 0) 0f else state.habitsDoneToday.toFloat() / state.habitsScheduledToday,
                )
                Text(stringResource(R.string.home_focus_today, formatDuration(state.focusSecondsToday)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            QuickAction(Icons.Outlined.CheckCircle, R.string.home_add_task, actions.onAddTask)
            QuickAction(Icons.Outlined.Repeat, R.string.home_add_habit, actions.onAddHabit)
            QuickAction(Icons.Outlined.PlayArrow, R.string.home_start_focus, actions.onOpenFocus)
            QuickAction(Icons.Outlined.StickyNote2, R.string.home_add_note, actions.onAddNote)
            QuickAction(Icons.Outlined.CalendarMonth, R.string.home_calendar, actions.onOpenCalendar)
            QuickAction(Icons.Outlined.StickyNote2, R.string.home_notes, actions.onOpenNotes)
        }

        Column {
            SectionHeader(stringResource(R.string.home_today_list))
            if (state.focusTasks.isEmpty()) {
                Text(stringResource(R.string.home_no_tasks_today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.focusTasks.forEach { t -> TaskRow(t, state.now, null, onToggle = { onToggle(t) }, onOpen = { actions.onOpenTask(t.id) }) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProgressLine(text: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(stringResource(label)) }, leadingIcon = { Icon(icon, null) })
}
