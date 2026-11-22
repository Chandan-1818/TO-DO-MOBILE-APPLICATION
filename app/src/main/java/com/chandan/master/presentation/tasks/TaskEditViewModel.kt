package com.chandan.master.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.logic.TaskRules
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.Priority
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.CategoryRepository
import com.chandan.master.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val dueMinute: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val categoryId: Long? = null,
    val reminderOffset: Int? = null,
    val showTitleError: Boolean = false,
    val finished: Boolean = false,
) {
    val titleInvalid: Boolean get() = TaskRules.titleError(title)
}

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val categoryRepository: CategoryRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val taskId: Long = savedState.get<Long>("taskId") ?: -1L
    private val presetDay: Long = savedState.get<Long>("date") ?: Long.MIN_VALUE

    private val _state = MutableStateFlow(TaskEditUiState())
    val state: StateFlow<TaskEditUiState> = _state.asStateFlow()
    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var original: Task? = null

    init {
        viewModelScope.launch {
            if (taskId > 0) {
                val t = tasks.getTask(taskId)
                if (t == null) { _state.update { it.copy(loaded = true, finished = true) }; return@launch }
                original = t
                _state.value = TaskEditUiState(
                    loaded = true, isNew = false, title = t.title, description = t.description, dueDate = t.dueDate, dueMinute = t.dueMinuteOfDay,
                    priority = t.priority, categoryId = t.categoryId, reminderOffset = t.reminderOffsetMinutes,
                )
            } else {
                _state.value = TaskEditUiState(loaded = true, dueDate = if (presetDay != Long.MIN_VALUE) LocalDate.ofEpochDay(presetDay) else null)
            }
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v.take(120), showTitleError = false) }
    fun onDescription(v: String) = _state.update { it.copy(description = v.take(2000)) }
    fun onDueDate(d: LocalDate?) = _state.update { if (d == null) it.copy(dueDate = null, dueMinute = null, reminderOffset = null) else it.copy(dueDate = d) }
    fun onDueMinute(m: Int?) = _state.update { it.copy(dueMinute = m) }
    fun onPriority(p: Priority) = _state.update { it.copy(priority = p) }
    fun onCategory(id: Long?) = _state.update { it.copy(categoryId = id) }
    fun onReminder(offset: Int?) = _state.update { it.copy(reminderOffset = if (it.dueDate == null) null else offset) }

    fun createCategory(name: String) {
        viewModelScope.launch { runCatching { categoryRepository.createOrGet(name) }.onSuccess { id -> _state.update { it.copy(categoryId = id) } } }
    }

    fun save() {
        val s = _state.value
        if (s.titleInvalid) { _state.update { it.copy(showTitleError = true) }; return }
        viewModelScope.launch {
            val base = original ?: Task(title = "")
            tasks.save(
                base.copy(
                    title = s.title, description = s.description, dueDate = s.dueDate, dueMinuteOfDay = s.dueMinute, priority = s.priority,
                    categoryId = s.categoryId, reminderOffsetMinutes = if (s.dueDate == null) null else s.reminderOffset,
                ),
            )
            _state.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val id = original?.id ?: return
        viewModelScope.launch { tasks.delete(id); _state.update { it.copy(finished = true) } }
    }
}
