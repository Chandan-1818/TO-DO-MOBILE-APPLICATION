package com.chandan.master.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.presentation.components.ConfirmDialog
import com.chandan.master.presentation.components.EmptyState
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.TextInputDialog
import com.chandan.master.presentation.theme.Spacing

@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dialogError by rememberSaveable { mutableStateOf(false) }
    val duplicate = stringResource(R.string.categories_name_taken)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MasterTopBar(stringResource(R.string.categories_title), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogError = false; adding = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, stringResource(R.string.category_new))
            }
        },
    ) { padding ->
        if (state.loaded && state.items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(Icons.Outlined.Label, stringResource(R.string.categories_empty_title), stringResource(R.string.categories_empty_message))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)) {
                items(state.items, key = { it.category.id }) { item ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.category.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(pluralStringResource(R.plurals.categories_task_count, item.taskCount, item.taskCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { dialogError = false; renamingId = item.category.id }) { Icon(Icons.Outlined.Edit, stringResource(R.string.categories_rename_named, item.category.name)) }
                        IconButton(onClick = { deletingId = item.category.id }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete_named, item.category.name)) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    if (adding) TextInputDialog(stringResource(R.string.category_new), stringResource(R.string.field_category_name), { adding = false }, error = if (dialogError) duplicate else null) { name ->
        viewModel.create(name) { ok -> if (ok) adding = false else dialogError = true }
    }
    state.items.firstOrNull { it.category.id == renamingId }?.let { item ->
        TextInputDialog(stringResource(R.string.categories_rename), stringResource(R.string.field_category_name), { renamingId = null }, initial = item.category.name, error = if (dialogError) duplicate else null) { name ->
            viewModel.rename(item.category.id, name) { ok -> if (ok) renamingId = null else dialogError = true }
        }
    }
    state.items.firstOrNull { it.category.id == deletingId }?.let { item ->
        ConfirmDialog(
            stringResource(R.string.categories_delete_title, item.category.name),
            pluralStringResource(R.plurals.categories_delete_message, item.taskCount, item.taskCount),
            stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(item.category.id); deletingId = null }, onDismiss = { deletingId = null }, destructive = true,
        )
    }
}
