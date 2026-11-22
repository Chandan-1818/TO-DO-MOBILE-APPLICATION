package com.chandan.master.presentation.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.logic.FocusSettings
import com.chandan.master.domain.logic.SessionKind
import com.chandan.master.domain.logic.TimerPhase
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatClock
import com.chandan.master.presentation.util.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
private fun SessionKind.label(): String = stringResource(
    when (this) { SessionKind.FOCUS -> R.string.focus_kind_focus; SessionKind.SHORT_BREAK -> R.string.focus_kind_short; SessionKind.LONG_BREAK -> R.string.focus_kind_long },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(onBack: () -> Unit, viewModel: FocusViewModel = hiltViewModel()) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val idle = s.phase == TimerPhase.IDLE
    val clockDescription = stringResource(R.string.focus_remaining_description, s.displaySeconds / 60, s.displaySeconds % 60)

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { MasterTopBar(stringResource(R.string.focus_title), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SessionKind.entries.forEach { k -> FilterChip(selected = s.kind == k, onClick = { viewModel.selectKind(k) }, enabled = idle, label = { Text(k.label()) }) }
            }
            Box(Modifier.size(240.dp).semantics(mergeDescendants = true) { contentDescription = clockDescription }, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { s.progress }, modifier = Modifier.size(240.dp), strokeWidth = 8.dp, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatClock(s.displaySeconds), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        stringResource(when (s.phase) { TimerPhase.IDLE -> R.string.focus_ready; TimerPhase.RUNNING -> R.string.focus_running; TimerPhase.PAUSED -> R.string.focus_paused }),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(stringResource(R.string.focus_sessions_today, s.completedFocusSessions, formatDuration(s.todayFocusSeconds)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (s.phase) {
                TimerPhase.IDLE -> Button(onClick = viewModel::start, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.focus_start)) }
                TimerPhase.RUNNING -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = viewModel::pause, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.focus_pause)) }
                    OutlinedButton(onClick = viewModel::stop, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.focus_stop)) }
                }
                TimerPhase.PAUSED -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = viewModel::resume, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.focus_resume)) }
                        OutlinedButton(onClick = viewModel::stop, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.focus_stop)) }
                    }
                    TextButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.focus_reset)) }
                }
            }
            if (!idle) Text(stringResource(R.string.focus_background_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (idle) {
                Column(Modifier.fillMaxWidth()) {
                    SectionHeader(stringResource(R.string.focus_durations))
                    MinutesRow(stringResource(R.string.focus_kind_focus), s.settings.focusMinutes) { viewModel.setMinutes(SessionKind.FOCUS, it) }
                    MinutesRow(stringResource(R.string.focus_kind_short), s.settings.shortBreakMinutes) { viewModel.setMinutes(SessionKind.SHORT_BREAK, it) }
                    MinutesRow(stringResource(R.string.focus_kind_long), s.settings.longBreakMinutes) { viewModel.setMinutes(SessionKind.LONG_BREAK, it) }
                }
            }

            Column(Modifier.fillMaxWidth()) {
                SectionHeader(stringResource(R.string.focus_history))
                if (s.history.isEmpty()) Text(stringResource(R.string.focus_history_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val fmt = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT) }
                s.history.forEach { h ->
                    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt.format(Instant.ofEpochMilli(h.startedAtMs).atZone(ZoneId.systemDefault())), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            formatDuration(h.actualSeconds.toLong()) + if (h.completed) "" else " · " + stringResource(R.string.focus_stopped_early),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinutesRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(minutes - 5) }, enabled = minutes > FocusSettings.MINUTES_RANGE.first) { Icon(Icons.Filled.Remove, stringResource(R.string.focus_decrease, label)) }
            Text(stringResource(R.string.focus_minutes_value, minutes), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = { onChange(minutes + 5) }, enabled = minutes < FocusSettings.MINUTES_RANGE.last) { Icon(Icons.Filled.Add, stringResource(R.string.focus_increase, label)) }
        }
    }
}
