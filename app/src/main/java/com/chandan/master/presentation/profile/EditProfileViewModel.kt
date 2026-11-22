package com.chandan.master.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandan.master.domain.auth.UsernameError
import com.chandan.master.domain.auth.UsernameValidator
import com.chandan.master.domain.repository.AvatarRepository
import com.chandan.master.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val loaded: Boolean = false,
    val username: String = "",
    val savedUsername: String = "",
    val avatarPath: String? = null,
    val showError: Boolean = false,
    val photoFailed: Boolean = false,
    val saved: Boolean = false,
) {
    val usernameError: UsernameError? get() = UsernameValidator.validate(username)
    val canSave: Boolean get() = loaded && usernameError == null && UsernameValidator.normalize(username) != savedUsername
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val avatars: AvatarRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val first = profiles.observeActiveProfile().filterNotNull().first()
            _state.update { it.copy(loaded = true, username = first.username, savedUsername = first.username, avatarPath = first.avatarUri) }
            profiles.observeActiveProfile().filterNotNull().collect { p -> _state.update { it.copy(avatarPath = p.avatarUri) } }
        }
    }

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v.take(40), showError = false) }

    fun save() {
        val s = _state.value
        if (s.usernameError != null) { _state.update { it.copy(showError = true) }; return }
        viewModelScope.launch {
            profiles.updateUsername(UsernameValidator.normalize(s.username))
            _state.update { it.copy(saved = true) }
        }
    }

    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            val path = avatars.importFromUri(uri)
            if (path == null) _state.update { it.copy(photoFailed = true) }
            else { profiles.updateAvatarPath(path); _state.update { it.copy(photoFailed = false) } }
        }
    }

    /** Back to the default samurai picture. */
    fun onRestoreDefault() {
        viewModelScope.launch { profiles.updateAvatarPath(null) }
    }
}
