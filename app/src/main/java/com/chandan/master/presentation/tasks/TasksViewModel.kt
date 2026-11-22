package com.chandan.master.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.domain.logic.TaskFilter
import com.chandan.master.domain.logic.TaskRules
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.CategoryRepository
import com.chandan.master.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val query: String = "",
    val categoryId: Long? = null,
    val now: LocalDateTime = LocalDateTime.now(),
    val loaded: Boolean = false,
    val hasAnyTasks: Boolean = false,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val tasks: TaskRepository,
    categories: CategoryRepository,
    private val clock: DateTimeProvider,
) : ViewModel() {
    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val query = MutableStateFlow("")
    private val categoryId = MutableStateFlow<Long?>(null)
    private val minuteTicker = flow { while (true) { emit(Unit); delay(60_000) } }

    /** The most recently deleted task, kept so the screen can offer "Undo". */
    private val _lastDeleted = MutableStateFlow<Task?>(null)
    val lastDeleted: StateFlow<Task?> = _lastDeleted

    val uiState: StateFlow<TasksUiState> = combine(
        tasks.observeTasks(), categories.observeCategories(), filter, query, categoryId,
    ) { all, cats, f, q, c ->
        val now = clock.nowLocal()
        TasksUiState(TaskRules.apply(all, f, q, c, now), cats, f, q, c, now, loaded = true, hasAnyTasks = all.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun onFilter(f: TaskFilter) { filter.value = f }
    fun onQuery(q: String) { query.value = q.take(100) }
    fun onCategory(id: Long?) { categoryId.update { if (it == id) null else id } }

    fun toggle(task: Task) { viewModelScope.launch { tasks.setCompleted(task.id, !task.completed) } }

    fun delete(task: Task) {
        viewModelScope.launch { tasks.delete(task.id); _lastDeleted.value = task }
    }

    fun undoDelete() {
        val t = _lastDeleted.value ?: return
        _lastDeleted.value = null
        viewModelScope.launch { tasks.save(t.copy(id = 0)) }
    }

    fun clearUndo() { _lastDeleted.value = null }
}
