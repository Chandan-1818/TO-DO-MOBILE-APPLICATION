package com.chandan.master.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(private val prefs: PreferencesRepository) : ViewModel() {
    val state: StateFlow<AppPreferences> = prefs.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())
    fun setMaster(v: Boolean) { viewModelScope.launch { prefs.setNotificationsEnabled(v) } }
    fun setTasks(v: Boolean) { viewModelScope.launch { prefs.setTaskReminders(v) } }
    fun setHabits(v: Boolean) { viewModelScope.launch { prefs.setHabitReminders(v) } }
    fun setFocus(v: Boolean) { viewModelScope.launch { prefs.setFocusNotifications(v) } }
}
