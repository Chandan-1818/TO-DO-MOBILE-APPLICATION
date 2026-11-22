package com.chandan.master.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(val notes: List<Note> = emptyList(), val query: String = "", val loaded: Boolean = false, val hasAny: Boolean = false)

@HiltViewModel
class NotesViewModel @Inject constructor(private val repo: NoteRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _lastDeleted = MutableStateFlow<Note?>(null)
    val lastDeleted: StateFlow<Note?> = _lastDeleted

    val uiState: StateFlow<NotesUiState> = combine(repo.observeNotes(), query) { all, q ->
        val filtered = all.filter { q.isBlank() || it.title.contains(q.trim(), true) || it.content.contains(q.trim(), true) }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAtMs })
        NotesUiState(filtered, q, loaded = true, hasAny = all.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun onQuery(q: String) { query.value = q.take(100) }
    fun togglePin(n: Note) { viewModelScope.launch { repo.setPinned(n.id, !n.pinned) } }
    fun delete(n: Note) { viewModelScope.launch { repo.delete(n.id); _lastDeleted.value = n } }
    fun undoDelete() { val n = _lastDeleted.value ?: return; _lastDeleted.value = null; viewModelScope.launch { repo.save(n.copy(id = 0)) } }
    fun clearUndo() { _lastDeleted.value = null }
}
