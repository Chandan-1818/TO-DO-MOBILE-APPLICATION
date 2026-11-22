package com.chandan.master.domain.usecase

import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import com.chandan.master.domain.model.UserProfile
import com.chandan.master.domain.repository.PreferencesRepository
import com.chandan.master.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakePreferencesRepository : PreferencesRepository {
    val state = MutableStateFlow(AppPreferences())
    override val preferences: Flow<AppPreferences> = state
    override suspend fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
    override suspend fun setAccentPalette(palette: AccentPalette) { state.value = state.value.copy(accentPalette = palette) }
    override suspend fun setAutoLockDelay(delay: com.chandan.master.domain.auth.AutoLockDelay) { state.value = state.value.copy(autoLockDelay = delay) }
    override suspend fun setBiometricEnabled(enabled: Boolean) { state.value = state.value.copy(biometricEnabled = enabled) }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { state.value = state.value.copy(notificationsEnabled = enabled) }
    override suspend fun setTaskReminders(enabled: Boolean) { state.value = state.value.copy(taskReminders = enabled) }
    override suspend fun setHabitReminders(enabled: Boolean) { state.value = state.value.copy(habitReminders = enabled) }
    override suspend fun setFocusNotifications(enabled: Boolean) { state.value = state.value.copy(focusNotifications = enabled) }
    override suspend fun setFocusSettings(settings: com.chandan.master.domain.logic.FocusSettings) { state.value = state.value.copy(focusSettings = settings) }
}

private class FakeProfileRepository(initial: UserProfile? = null) : ProfileRepository {
    val state = MutableStateFlow(initial)
    override fun observeActiveProfile(): Flow<UserProfile?> = state
    override suspend fun saveProfile(profile: UserProfile): Long { state.value = profile; return profile.id }
    override suspend fun updateUsername(username: String) { state.value = state.value?.copy(username = username) }
    override suspend fun updateAvatarPath(path: String?) { state.value = state.value?.copy(avatarUri = path) }
}

class PreferenceUseCasesTest {
    @Test fun setThemeModeUpdatesObservedPreferences() = runTest {
        val repo = FakePreferencesRepository()
        SetThemeModeUseCase(repo)(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ObserveAppPreferencesUseCase(repo)().first().themeMode)
    }

    @Test fun setAccentKeepsThemeMode() = runTest {
        val repo = FakePreferencesRepository()
        SetThemeModeUseCase(repo)(ThemeMode.LIGHT)
        SetAccentPaletteUseCase(repo)(AccentPalette.CRIMSON)
        assertEquals(AppPreferences(ThemeMode.LIGHT, AccentPalette.CRIMSON), ObserveAppPreferencesUseCase(repo)().first())
    }

    @Test fun noProfileEmitsNull() = runTest {
        assertNull(ObserveActiveProfileUseCase(FakeProfileRepository())().first())
    }

    @Test fun savedProfileIsObserved() = runTest {
        val repo = FakeProfileRepository()
        val profile = UserProfile(1, "ronin", null, isGuest = false, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        repo.saveProfile(profile)
        assertEquals(profile, ObserveActiveProfileUseCase(repo)().first())
    }
}
