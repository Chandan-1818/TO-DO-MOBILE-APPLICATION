package com.chandan.master.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.backup.BackupFiles
import com.chandan.master.domain.backup.BackupFormatException
import com.chandan.master.domain.backup.RestoreSummary
import com.chandan.master.domain.repository.BackupPreview
import com.chandan.master.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BackupMessage {
    data object ExportDone : BackupMessage
    data object ExportFailed : BackupMessage
    data object FileUnreadable : BackupMessage
    data class Invalid(val reason: String) : BackupMessage
    data class RestoreDone(val summary: RestoreSummary) : BackupMessage
    data object RestoreFailed : BackupMessage
}

data class BackupUiState(val busy: Boolean = false, val preview: BackupPreview? = null, val message: BackupMessage? = null)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backups: BackupRepository,
    private val files: BackupFiles,
    private val clock: DateTimeProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    val suggestedFileName: String get() = "master-backup-${clock.today()}.json"

    fun onExportTarget(uri: Uri?) {
        if (uri == null) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val ok = try { files.write(uri, backups.createBackup()) } catch (e: Exception) { false }
            _state.update { it.copy(busy = false, message = if (ok) BackupMessage.ExportDone else BackupMessage.ExportFailed) }
        }
    }

    fun onImportSource(uri: Uri?) {
        if (uri == null) return
        _state.update { it.copy(busy = true, message = null, preview = null) }
        viewModelScope.launch {
            val text = files.read(uri)
            if (text == null) { _state.update { it.copy(busy = false, message = BackupMessage.FileUnreadable) }; return@launch }
            try {
                val preview = backups.inspect(text)
                _state.update { it.copy(busy = false, preview = preview) }
            } catch (e: BackupFormatException) {
                _state.update { it.copy(busy = false, message = BackupMessage.Invalid(e.message ?: "")) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = BackupMessage.FileUnreadable) }
            }
        }
    }

    fun confirmRestore() {
        val preview = _state.value.preview ?: return
        _state.update { it.copy(busy = true, preview = null) }
        viewModelScope.launch {
            val result = try { BackupMessage.RestoreDone(backups.restore(preview.data)) } catch (e: Exception) { BackupMessage.RestoreFailed }
            _state.update { it.copy(busy = false, message = result) }
        }
    }

    fun cancelPreview() = _state.update { it.copy(preview = null) }
    fun dismissMessage() = _state.update { it.copy(message = null) }
}
