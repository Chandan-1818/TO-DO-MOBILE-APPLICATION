package com.chandan.master.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.tasks.TaskRow
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(onBack: () -> Unit, onAddTask: (LocalDate) -> Unit, onOpenTask: (Long) -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { MasterTopBar(stringResource(R.string.calendar_title), onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = viewModel::previousMonth) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.calendar_previous)) }
                    Text(
                        "${s.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${s.month.year}",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() },
                    )
                    IconButton(onClick = viewModel::nextMonth) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.calendar_next)) }
                }
                Row(Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { d ->
                        Text(
                            d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                MonthGrid(s, viewModel::select)
                TextButton(onClick = viewModel::goToday) { Text(stringResource(R.string.calendar_today)) }
            }
            item {
                SectionHeader(formatDate(s.selected))
                OutlinedButton(onClick = { onAddTask(s.selected) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calendar_add_task)) }
            }
            if (s.selectedTasks.isEmpty() && s.selectedHabits.isEmpty()) {
                item { Text(stringResource(R.string.calendar_nothing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = Spacing.md)) }
            }
            items(s.selectedTasks, key = { "t${it.id}" }) { t ->
                TaskRow(t, s.now, null, onToggle = { viewModel.toggleTask(t) }, onOpen = { onOpenTask(t.id) })
            }
            if (s.selectedHabits.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.calendar_habits_that_day)) }
                items(s.selectedHabits, key = { "h${it.id}" }) { h ->
                    val done = h.id in s.habitsDoneOnSelected
                    Text(
                        h.name + " · " + stringResource(if (done) R.string.day_completed else R.string.day_not_done),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(s: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val first = s.month.atDay(1)
    val leading = first.dayOfWeek.value - 1 // Monday-first
    val cells = leading + s.month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val dayNumber = r * 7 + c - leading + 1
                    if (dayNumber < 1 || dayNumber > s.month.lengthOfMonth()) Box(Modifier.weight(1f).height(48.dp))
                    else DayCell(s.month.atDay(dayNumber), s, onSelect, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, s: CalendarUiState, onSelect: (LocalDate) -> Unit, modifier: Modifier) {
    val selected = date == s.selected
    val open = date in s.daysWithOpenTasks
    val any = date in s.daysWithTasks
    val desc = formatDate(date) + when { open -> ", " + stringResource(R.string.calendar_has_open_tasks); any -> ", " + stringResource(R.string.calendar_has_tasks); else -> "" }
    Box(modifier.height(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                .then(if (date == s.today) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                .clickable(role = Role.Button) { onSelect(date) }
                .semantics { contentDescription = desc },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground)
                // The dot is the only marker; filled = open tasks, ring = all tasks done. Shape differs, not just colour.
                Box(
                    Modifier.size(6.dp).clip(CircleShape).then(
                        when { open -> Modifier.background(MaterialTheme.colorScheme.primary); any -> Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape); else -> Modifier },
                    ),
                )
            }
        }
    }
}
