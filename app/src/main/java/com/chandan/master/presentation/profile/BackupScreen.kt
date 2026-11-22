package com.chandan.master.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.backup.CountPair
import com.chandan.master.presentation.components.ConfirmDialog
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.PrimaryButton
import com.chandan.master.presentation.components.SecondaryButton
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.theme.MasterTheme
import com.chandan.master.presentation.theme.Spacing

@Composable
private fun countLine(label: String, c: CountPair) = stringResource(R.string.backup_count_line, label, c.added, c.skippedDuplicates)

@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { viewModel.onExportTarget(it) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.onImportSource(it) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { MasterTopBar(stringResource(R.string.backup_title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            SectionHeader(stringResource(R.string.backup_export_section))
            Text(stringResource(R.string.backup_export_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(stringResource(R.string.backup_export_action), onClick = { exportLauncher.launch(viewModel.suggestedFileName) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth())

            SectionHeader(stringResource(R.string.backup_import_section))
            Text(stringResource(R.string.backup_import_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SecondaryButton(
                stringResource(R.string.backup_import_action),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
            )

            if (state.busy) CircularProgressIndicator()
            when (val m = state.message) {
                null -> Unit
                BackupMessage.ExportDone -> Text(stringResource(R.string.backup_export_done), color = MasterTheme.extra.success, style = MaterialTheme.typography.bodyMedium)
                BackupMessage.ExportFailed -> Text(stringResource(R.string.backup_export_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                BackupMessage.FileUnreadable -> Text(stringResource(R.string.backup_unreadable), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                is BackupMessage.Invalid -> Text(stringResource(R.string.backup_invalid, m.reason), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                BackupMessage.RestoreFailed -> Text(stringResource(R.string.backup_restore_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                is BackupMessage.RestoreDone -> Text(stringResource(R.string.backup_restore_done, m.summary.totalAdded), color = MasterTheme.extra.success, style = MaterialTheme.typography.bodyMedium)
            }

            SectionHeader(stringResource(R.string.backup_what_section))
            Text(stringResource(R.string.backup_what_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    state.preview?.let { preview ->
        val s = preview.plan.summary
        val message = buildString {
            append(stringResource(R.string.backup_preview_intro, preview.data.profileUsername ?: "?")).append("\n\n")
            append(countLine(stringResource(R.string.backup_label_tasks), s.tasks)).append('\n')
            append(countLine(stringResource(R.string.backup_label_categories), s.categories)).append('\n')
            append(countLine(stringResource(R.string.backup_label_habits), s.habits)).append('\n')
            append(countLine(stringResource(R.string.backup_label_checkins), s.completions)).append('\n')
            append(countLine(stringResource(R.string.backup_label_sessions), s.sessions)).append('\n')
            append(countLine(stringResource(R.string.backup_label_notes), s.notes)).append("\n\n")
            append(stringResource(R.string.backup_preview_footer))
        }
        ConfirmDialog(stringResource(R.string.backup_preview_title), message, stringResource(R.string.backup_restore_action), onConfirm = viewModel::confirmRestore, onDismiss = viewModel::cancelPreview)
    }
}
