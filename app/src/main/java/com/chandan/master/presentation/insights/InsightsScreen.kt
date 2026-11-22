package com.chandan.master.presentation.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.logic.InsightsSummary
import com.chandan.master.presentation.components.EmptyState
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatDuration
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(stringResource(R.string.nav_insights), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
        val s = summary ?: return@Column
        if (!s.hasAnyData) {
            EmptyState(Icons.Outlined.BarChart, stringResource(R.string.insights_empty_title), stringResource(R.string.insights_empty_message))
            return@Column
        }
        TasksSection(s); HabitsSection(s); FocusSection(s); WeekSection(s)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TasksSection(s: InsightsSummary) {
    Column {
        SectionHeader(stringResource(R.string.insights_tasks))
        StatRow(stringResource(R.string.insights_total_tasks), s.totalTasks.toString())
        StatRow(stringResource(R.string.insights_completed_tasks), s.completedTasks.toString())
        StatRow(stringResource(R.string.insights_pending_tasks), s.pendingTasks.toString())
        StatRow(stringResource(R.string.insights_overdue_tasks), s.overdueTasks.toString())
        StatRow(stringResource(R.string.insights_completion_rate), s.taskCompletionPercent?.let { "$it%" } ?: "–")
        s.taskCompletionPercent?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest) }
    }
}

@Composable
private fun HabitsSection(s: InsightsSummary) {
    Column {
        SectionHeader(stringResource(R.string.insights_habits))
        if (s.habitStreaks.isEmpty()) {
            Text(stringResource(R.string.insights_no_habits), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        StatRow(stringResource(R.string.insights_consistency), s.habitConsistencyPercent?.let { "$it%" } ?: "–")
        Text(stringResource(R.string.insights_consistency_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        s.habitStreaks.forEach { h ->
            StatRow(h.name, stringResource(R.string.insights_streak_value, h.currentStreak, h.longestStreak))
        }
    }
}

@Composable
private fun FocusSection(s: InsightsSummary) {
    Column {
        SectionHeader(stringResource(R.string.insights_focus))
        StatRow(stringResource(R.string.insights_focus_total), formatDuration(s.totalFocusSeconds))
        StatRow(stringResource(R.string.insights_focus_week), formatDuration(s.focusSecondsLast7Days.toLong()))
        StatRow(stringResource(R.string.insights_focus_sessions), s.completedFocusSessions.toString())
    }
}

@Composable
private fun WeekSection(s: InsightsSummary) {
    Column {
        SectionHeader(stringResource(R.string.insights_week))
        Row(Modifier.fillMaxWidth()) {
            HeaderCell(stringResource(R.string.insights_col_day), 1.2f); HeaderCell(stringResource(R.string.insights_col_tasks), 1f)
            HeaderCell(stringResource(R.string.insights_col_habits), 1f); HeaderCell(stringResource(R.string.insights_col_focus), 1f)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        s.last7Days.reversed().forEach { d ->
            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                Cell(d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + d.date.dayOfMonth, 1.2f)
                Cell(d.tasksCompleted.toString(), 1f); Cell(d.habitsCompleted.toString(), 1f); Cell((d.focusSeconds / 60).toString(), 1f)
            }
        }
        Text(stringResource(R.string.insights_week_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.sm))
    }
}

@Composable private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) =
    Text(text, Modifier.weight(weight), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)

@Composable private fun androidx.compose.foundation.layout.RowScope.Cell(text: String, weight: Float) =
    Text(text, Modifier.weight(weight), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Start)
