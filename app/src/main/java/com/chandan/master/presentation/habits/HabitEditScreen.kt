package com.chandan.master.presentation.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.presentation.components.ConfirmDialog
import com.chandan.master.presentation.components.MasterDatePickerDialog
import com.chandan.master.presentation.components.MasterTextField
import com.chandan.master.presentation.components.MasterTimePickerDialog
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.PrimaryButton
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.components.rememberNotificationPermissionRequest
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatDate
import com.chandan.master.presentation.util.formatMinuteOfDay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitEditScreen(onBack: () -> Unit, viewModel: HabitEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val askNotifications = rememberNotificationPermissionRequest()
    LaunchedEffect(state.finished) { if (state.finished) onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MasterTopBar(
                title = stringResource(if (state.isNew) R.string.habit_new else R.string.habit_edit), onBack = onBack,
                actions = { if (!state.isNew) IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete)) } },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MasterTextField(state.name, viewModel::onName, stringResource(R.string.field_habit_name), error = if (state.showNameError && state.nameInvalid) stringResource(R.string.error_habit_name) else null)
            MasterTextField(state.description, viewModel::onDescription, stringResource(R.string.field_habit_description))

            SectionHeader(stringResource(R.string.habit_schedule_section))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = WeekdayMask.isScheduled(state.mask, day), onClick = { viewModel.onToggleDay(day) },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TextButton(onClick = viewModel::onEveryDay) { Text(stringResource(R.string.habit_every_day)) }
                TextButton(onClick = viewModel::onWeekdays) { Text(stringResource(R.string.habit_weekdays)) }
            }
            if (state.scheduleInvalid) Text(stringResource(R.string.error_habit_schedule), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            SectionHeader(stringResource(R.string.habit_start_section))
            OutlinedButton(onClick = { showDate = true }) { Text(formatDate(state.startDate)) }

            SectionHeader(stringResource(R.string.habit_reminder_section))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showTime = true }) { Text(state.reminderMinute?.let { formatMinuteOfDay(it) } ?: stringResource(R.string.habit_set_reminder)) }
                if (state.reminderMinute != null) TextButton(onClick = { viewModel.onReminder(null) }) { Text(stringResource(R.string.action_clear)) }
            }

            PrimaryButton(stringResource(R.string.action_save), onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
            if (!state.isNew) {
                TextButton(onClick = { viewModel.setArchived(!state.archived) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (state.archived) R.string.habit_unarchive else R.string.habit_archive))
                }
                Text(stringResource(R.string.habit_archive_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDate) MasterDatePickerDialog(state.startDate, { showDate = false }) { viewModel.onStartDate(it); showDate = false }
    if (showTime) MasterTimePickerDialog(state.reminderMinute ?: 8 * 60, { showTime = false }) { viewModel.onReminder(it); showTime = false; askNotifications() }
    if (showDelete) ConfirmDialog(
        stringResource(R.string.habit_delete_title), stringResource(R.string.habit_delete_message), stringResource(R.string.action_delete),
        onConfirm = { showDelete = false; viewModel.delete() }, onDismiss = { showDelete = false }, destructive = true,
    )
}
