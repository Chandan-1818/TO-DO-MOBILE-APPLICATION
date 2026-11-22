package com.chandan.master.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.logic.TaskFilter
import com.chandan.master.presentation.components.EmptyState
import com.chandan.master.presentation.theme.Spacing

@Composable
private fun TaskFilter.label(): String = stringResource(
    when (this) {
        TaskFilter.ALL -> R.string.filter_all; TaskFilter.TODAY -> R.string.filter_today; TaskFilter.UPCOMING -> R.string.filter_upcoming
        TaskFilter.OVERDUE -> R.string.filter_overdue; TaskFilter.COMPLETED -> R.string.filter_completed
    },
)

@Composable
fun TasksScreen(onAddTask: () -> Unit, onOpenTask: (Long) -> Unit, viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.lastDeleted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.action_undo)
    val deletedMessage = deleted?.let { stringResource(R.string.task_deleted, it.title) }

    LaunchedEffect(deleted) {
        if (deleted != null && deletedMessage != null) {
            when (snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Long)) {
                SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                SnackbarResult.Dismissed -> viewModel.clearUndo()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.task_add))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(horizontal = Spacing.screenHorizontal)) {
            Text(
                stringResource(R.string.nav_tasks), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm).semantics { heading() },
            )
            OutlinedTextField(
                value = state.query, onValueChange = viewModel::onQuery, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_tasks)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(vertical = Spacing.sm)) {
                items(TaskFilter.entries.toList()) { f ->
                    FilterChip(selected = state.filter == f, onClick = { viewModel.onFilter(f) }, label = { Text(f.label()) })
                }
            }
            if (state.categories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(bottom = Spacing.sm)) {
                    items(state.categories, key = { it.id }) { c ->
                        FilterChip(selected = state.categoryId == c.id, onClick = { viewModel.onCategory(c.id) }, label = { Text(c.name) })
                    }
                }
            }
            if (state.loaded && state.tasks.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = stringResource(if (state.hasAnyTasks) R.string.tasks_none_match_title else R.string.tasks_empty_title),
                    message = stringResource(if (state.hasAnyTasks) R.string.tasks_none_match_message else R.string.tasks_empty_message),
                )
            } else {
                val names = remember(state.categories) { state.categories.associate { it.id to it.name } }
                LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(state.tasks, key = { it.id }) { t ->
                        TaskRow(
                            task = t, now = state.now, categoryName = t.categoryId?.let { names[it] },
                            onToggle = { viewModel.toggle(t) }, onOpen = { onOpenTask(t.id) }, onDelete = { viewModel.delete(t) },
                        )
                    }
                }
            }
        }
    }
}
