package com.chandan.master.presentation.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditUiState(
    val loaded: Boolean = false, val isNew: Boolean = true, val title: String = "", val content: String = "", val pinned: Boolean = false, val finished: Boolean = false,
) { val isBlank: Boolean get() = title.isBlank() && content.isBlank() }

@HiltViewModel
class NoteEditViewModel @Inject constructor(private val repo: NoteRepository, savedState: SavedStateHandle) : ViewModel() {
    private val noteId: Long = savedState.get<Long>("noteId") ?: -1L
    private val _state = MutableStateFlow(NoteEditUiState())
    val state: StateFlow<NoteEditUiState> = _state.asStateFlow()
    private var original: Note? = null
    private var savedOnce = false

    init {
        viewModelScope.launch {
            if (noteId > 0) {
                val n = repo.getNote(noteId)
                if (n == null) { _state.update { it.copy(loaded = true, finished = true) }; return@launch }
                original = n
                _state.value = NoteEditUiState(true, false, n.title, n.content, n.pinned)
            } else _state.value = NoteEditUiState(loaded = true)
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v.take(200)) }
    fun onContent(v: String) = _state.update { it.copy(content = v.take(200_000)) }
    fun onTogglePin() = _state.update { it.copy(pinned = !it.pinned) }

    /** Saves (unless the note is empty or unchanged) and leaves. Used by Back, the system back gesture and the Save action. */
    fun saveAndExit() {
        val s = _state.value
        if (savedOnce) return
        savedOnce = true
        viewModelScope.launch {
            val o = original
            val unchanged = o != null && o.title == s.title && o.content == s.content && o.pinned == s.pinned
            if (!s.isBlank && !unchanged) repo.save((o ?: Note(title = "", content = "")).copy(title = s.title, content = s.content, pinned = s.pinned))
            _state.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val id = original?.id
        savedOnce = true
        viewModelScope.launch { if (id != null) repo.delete(id); _state.update { it.copy(finished = true) } }
    }
}
