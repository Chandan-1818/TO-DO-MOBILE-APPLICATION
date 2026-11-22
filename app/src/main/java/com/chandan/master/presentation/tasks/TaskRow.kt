package com.chandan.master.presentation.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chandan.master.R
import com.chandan.master.domain.logic.TaskRules
import com.chandan.master.domain.model.Priority
import com.chandan.master.domain.model.Task
import com.chandan.master.presentation.theme.MasterTheme
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.formatDate
import com.chandan.master.presentation.util.formatMinuteOfDay
import java.time.LocalDateTime

@Composable
fun priorityLabel(p: Priority): String = stringResource(
    when (p) { Priority.LOW -> R.string.priority_low; Priority.MEDIUM -> R.string.priority_medium; Priority.HIGH -> R.string.priority_high; Priority.URGENT -> R.string.priority_urgent },
)

/** One task: completion toggle, title, meta line (due, category, priority), optional delete. Colour is never the only cue. */
@Composable
fun TaskRow(
    task: Task,
    now: LocalDateTime,
    categoryName: String?,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val overdue = TaskRules.isOverdue(task, now)
    val today = now.toLocalDate()
    val dueText = task.dueDate?.let { d ->
        val day = when (d) { today -> stringResource(R.string.due_today); today.plusDays(1) -> stringResource(R.string.due_tomorrow); else -> formatDate(d) }
        if (task.dueMinuteOfDay != null) "$day, ${formatMinuteOfDay(task.dueMinuteOfDay)}" else day
    }
    val toggleDesc = stringResource(if (task.completed) R.string.task_mark_open else R.string.task_mark_done, task.title)

    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onOpen).padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.semantics { contentDescription = toggleDesc; role = Role.Checkbox }) {
            if (task.completed) Icon(Icons.Filled.CheckCircle, null, tint = MasterTheme.extra.success)
            else Icon(Icons.Outlined.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.outline)
        }
        Column(Modifier.weight(1f).padding(end = Spacing.sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            )
            val meta = buildList {
                if (overdue) add(stringResource(R.string.task_overdue))
                if (dueText != null) add(dueText)
                if (categoryName != null) add(categoryName)
                if (task.priority >= Priority.HIGH) add(priorityLabel(task.priority))
            }.joinToString(" · ")
            if (meta.isNotEmpty()) Text(
                meta, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (overdue) MasterTheme.extra.danger else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete_named, task.title), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
