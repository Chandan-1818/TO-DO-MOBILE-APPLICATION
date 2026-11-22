package com.chandan.master.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.repository.CategoryRepository
import com.chandan.master.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryItem(val category: Category, val taskCount: Int)

data class CategoriesUiState(val items: List<CategoryItem> = emptyList(), val loaded: Boolean = false, val nameError: Boolean = false)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categories: CategoryRepository,
    tasks: TaskRepository,
) : ViewModel() {
    private val nameError = MutableStateFlow(false)

    val uiState: StateFlow<CategoriesUiState> = combine(categories.observeCategories(), tasks.observeTasks(), nameError) { cats, all, err ->
        val counts = all.groupingBy { it.categoryId }.eachCount()
        CategoriesUiState(cats.map { CategoryItem(it, counts[it.id] ?: 0) }, loaded = true, nameError = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    /** Returns via [onDone] whether the dialog may close (false = name blank or already used). */
    fun create(name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { runCatching { categories.createOrGet(name) }.onSuccess { onDone(true) }.onFailure { onDone(false) } }
    }

    fun rename(id: Long, name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(categories.rename(id, name)) }
    }

    fun delete(id: Long) { viewModelScope.launch { categories.delete(id) } }
    fun clearError() = nameError.update { false }
}
