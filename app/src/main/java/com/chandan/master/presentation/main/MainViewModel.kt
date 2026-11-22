package com.chandan.master.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.usecase.ObserveAppPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val preferences: AppPreferences) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    observePreferences: ObserveAppPreferencesUseCase,
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = observePreferences()
        .map<AppPreferences, MainUiState> { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}
