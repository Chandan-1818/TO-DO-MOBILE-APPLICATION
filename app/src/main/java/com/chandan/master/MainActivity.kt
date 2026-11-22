package com.chandan.master

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import com.chandan.master.presentation.auth.AuthState
import com.chandan.master.presentation.auth.AuthViewModel
import com.chandan.master.presentation.main.MainUiState
import com.chandan.master.presentation.main.MainViewModel
import com.chandan.master.presentation.navigation.MasterAppRoot
import com.chandan.master.presentation.theme.MasterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() { // FragmentActivity is required by BiometricPrompt
    private val viewModel: MainViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the system splash only until preferences are read, so the first frame uses the right theme.
        splash.setKeepOnScreenCondition {
            viewModel.uiState.value is MainUiState.Loading || authViewModel.authState.value is AuthState.Loading
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val prefs = (state as? MainUiState.Ready)?.preferences ?: AppPreferences()
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            MasterTheme(darkTheme = darkTheme, accent = prefs.accentPalette) {
                MasterAppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        authViewModel.onAppStarted(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        // Rotation etc. recreates the activity; that is not "leaving the app".
        if (!isChangingConfigurations) authViewModel.onAppStopped(SystemClock.elapsedRealtime())
        super.onStop()
    }
}
