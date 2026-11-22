package com.chandan.master.presentation.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.presentation.components.ConfirmDialog
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.theme.Spacing

@Composable
fun NoteEditScreen(onBack: () -> Unit, viewModel: NoteEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    BackHandler { viewModel.saveAndExit() } // never lose typed text: leaving saves

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MasterTopBar(
                title = stringResource(if (state.isNew) R.string.note_new else R.string.note_edit), onBack = viewModel::saveAndExit,
                actions = {
                    IconButton(onClick = viewModel::onTogglePin) {
                        Icon(if (state.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, stringResource(if (state.pinned) R.string.note_unpin else R.string.note_pin))
                    }
                    if (!state.isNew) IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete)) }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = Spacing.screenHorizontal)) {
            OutlinedTextField(
                value = state.title, onValueChange = viewModel::onTitle, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.field_note_title)) }, textStyle = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = state.content, onValueChange = viewModel::onContent, modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Spacing.sm),
                placeholder = { Text(stringResource(R.string.field_note_content)) },
            )
        }
    }

    if (showDelete) ConfirmDialog(
        stringResource(R.string.note_delete_title), stringResource(R.string.note_delete_message), stringResource(R.string.action_delete),
        onConfirm = { showDelete = false; viewModel.delete() }, onDismiss = { showDelete = false }, destructive = true,
    )
}
