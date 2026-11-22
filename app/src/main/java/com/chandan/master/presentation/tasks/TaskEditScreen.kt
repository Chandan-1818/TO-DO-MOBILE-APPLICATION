package com.chandan.master.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
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
import com.chandan.master.domain.logic.ReminderCalculator
import com.chandan.master.domain.model.Priority
import com.chandan.master.presentation.components.ConfirmDialog
import com.chandan.master.presentation.components.MasterDatePickerDialog
import com.chandan.master.presentation.components.MasterTextField
import com.chandan.master.presentation.components.MasterTimePickerDialog
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.PrimaryButton
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.components.TextInputDialog
import com.chandan.master.presentation.components.rememberNotificationPermissionRequest
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatDate
import com.chandan.master.presentation.util.formatMinuteOfDay

@Composable
private fun reminderLabel(offset: Int?): String = stringResource(
    when (offset) { null -> R.string.reminder_none; 0 -> R.string.reminder_at_time; 10 -> R.string.reminder_10_min; 60 -> R.string.reminder_1_hour; else -> R.string.reminder_1_day },
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(onBack: () -> Unit, viewModel: TaskEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val askNotifications = rememberNotificationPermissionRequest()
    LaunchedEffect(state.finished) { if (state.finished) onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MasterTopBar(
                title = stringResource(if (state.isNew) R.string.task_new else R.string.task_edit), onBack = onBack,
                actions = {
                    if (!state.isNew) IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete)) }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MasterTextField(
                state.title, viewModel::onTitle, stringResource(R.string.field_task_title),
                error = if (state.showTitleError && state.titleInvalid) stringResource(R.string.error_task_title) else null,
            )
            MasterTextField(state.description, viewModel::onDescription, stringResource(R.string.field_task_description))

            SectionHeader(stringResource(R.string.task_due_section))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDate = true }) { Text(state.dueDate?.let { formatDate(it) } ?: stringResource(R.string.task_set_date)) }
                if (state.dueDate != null) TextButton(onClick = { viewModel.onDueDate(null) }) { Text(stringResource(R.string.action_clear)) }
            }
            if (state.dueDate != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showTime = true }) { Text(state.dueMinute?.let { formatMinuteOfDay(it) } ?: stringResource(R.string.task_set_time)) }
                    if (state.dueMinute != null) TextButton(onClick = { viewModel.onDueMinute(null) }) { Text(stringResource(R.string.action_clear)) }
                }
            }

            SectionHeader(stringResource(R.string.task_priority_section))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Priority.entries.forEach { p -> FilterChip(selected = state.priority == p, onClick = { viewModel.onPriority(p) }, label = { Text(priorityLabel(p)) }) }
            }

            SectionHeader(stringResource(R.string.task_category_section))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(selected = state.categoryId == null, onClick = { viewModel.onCategory(null) }, label = { Text(stringResource(R.string.category_none)) })
                categories.forEach { c -> FilterChip(selected = state.categoryId == c.id, onClick = { viewModel.onCategory(c.id) }, label = { Text(c.name) }) }
                FilterChip(selected = false, onClick = { showNewCategory = true }, label = { Text(stringResource(R.string.category_new)) })
            }

            if (state.dueDate != null) {
                SectionHeader(stringResource(R.string.task_reminder_section))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    (listOf<Int?>(null) + ReminderCalculator.OFFSET_CHOICES).forEach { o ->
                        FilterChip(
                            selected = state.reminderOffset == o,
                            onClick = { viewModel.onReminder(o); if (o != null) askNotifications() },
                            label = { Text(reminderLabel(o)) },
                        )
                    }
                }
                if (state.dueMinute == null && state.reminderOffset != null) {
                    Text(stringResource(R.string.reminder_date_only_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            PrimaryButton(stringResource(R.string.action_save), onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showDate) MasterDatePickerDialog(state.dueDate, { showDate = false }) { viewModel.onDueDate(it); showDate = false }
    if (showTime) MasterTimePickerDialog(state.dueMinute ?: ReminderCalculator.DEFAULT_MINUTE_OF_DAY, { showTime = false }) { viewModel.onDueMinute(it); showTime = false }
    if (showNewCategory) TextInputDialog(stringResource(R.string.category_new), stringResource(R.string.field_category_name), { showNewCategory = false }) { viewModel.createCategory(it); showNewCategory = false }
    if (showDelete) ConfirmDialog(
        stringResource(R.string.task_delete_title), stringResource(R.string.task_delete_message), stringResource(R.string.action_delete),
        onConfirm = { showDelete = false; viewModel.delete() }, onDismiss = { showDelete = false }, destructive = true,
    )
}
