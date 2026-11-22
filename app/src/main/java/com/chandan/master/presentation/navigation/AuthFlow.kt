package com.chandan.master.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chandan.master.presentation.onboarding.CreateAccountScreen
import com.chandan.master.presentation.onboarding.WelcomeScreen

/** Shown only while no local account exists. Finishing "Create account" switches the whole app to the main UI. */
@Composable
fun AuthFlow() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) { WelcomeScreen(onCreateAccount = { nav.navigate(Routes.CREATE_ACCOUNT) }) }
        composable(Routes.CREATE_ACCOUNT) { CreateAccountScreen(onBack = { nav.popBackStack() }) }
    }
}
