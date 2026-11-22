package com.chandan.master.presentation.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.model.Note
import com.chandan.master.presentation.components.EmptyState
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.theme.Spacing

fun Note.displayTitle(untitled: String): String = title.ifBlank { content.lineSequence().firstOrNull { it.isNotBlank() }?.take(60) ?: untitled }

@Composable
fun NotesScreen(onBack: () -> Unit, onAddNote: () -> Unit, onOpenNote: (Long) -> Unit, viewModel: NotesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.lastDeleted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val undo = stringResource(R.string.action_undo)
    val untitled = stringResource(R.string.note_untitled)
    val message = deleted?.let { stringResource(R.string.note_deleted, it.displayTitle(untitled)) }
    LaunchedEffect(deleted) {
        if (deleted != null && message != null) {
            when (snackbar.showSnackbar(message, undo, duration = SnackbarDuration.Long)) {
                SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                SnackbarResult.Dismissed -> viewModel.clearUndo()
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MasterTopBar(stringResource(R.string.notes_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = onAddNote, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Add, stringResource(R.string.note_add)) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.screenHorizontal)) {
            OutlinedTextField(
                value = state.query, onValueChange = viewModel::onQuery, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_notes)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
            )
            if (state.loaded && state.notes.isEmpty()) {
                EmptyState(
                    Icons.Outlined.StickyNote2,
                    stringResource(if (state.hasAny) R.string.notes_none_match_title else R.string.notes_empty_title),
                    stringResource(if (state.hasAny) R.string.notes_none_match_message else R.string.notes_empty_message),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(top = Spacing.md, bottom = 96.dp)) {
                    items(state.notes, key = { it.id }) { n ->
                        OutlinedCard(Modifier.fillMaxWidth().clickable { onOpenNote(n.id) }) {
                            Row(Modifier.padding(start = Spacing.lg, top = Spacing.sm, bottom = Spacing.sm), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f).padding(top = Spacing.xs)) {
                                    Text(n.displayTitle(untitled), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                                    if (n.title.isNotBlank() && n.content.isNotBlank()) {
                                        Text(n.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { viewModel.togglePin(n) }) {
                                    Icon(
                                        if (n.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        stringResource(if (n.pinned) R.string.note_unpin else R.string.note_pin),
                                        tint = if (n.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.delete(n) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete_named, n.displayTitle(untitled)), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
    }
}
