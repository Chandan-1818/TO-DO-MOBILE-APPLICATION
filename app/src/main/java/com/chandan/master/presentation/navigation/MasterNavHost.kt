package com.chandan.master.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chandan.master.presentation.calendar.CalendarScreen
import com.chandan.master.presentation.focus.FocusScreen
import com.chandan.master.presentation.habits.HabitEditScreen
import com.chandan.master.presentation.habits.HabitsScreen
import com.chandan.master.presentation.home.HomeActions
import com.chandan.master.presentation.home.HomeScreen
import com.chandan.master.presentation.insights.InsightsScreen
import com.chandan.master.presentation.notes.NoteEditScreen
import com.chandan.master.presentation.notes.NotesScreen
import com.chandan.master.presentation.profile.AboutScreen
import com.chandan.master.presentation.profile.BackupScreen
import com.chandan.master.presentation.profile.CategoriesScreen
import com.chandan.master.presentation.profile.EditProfileScreen
import com.chandan.master.presentation.profile.NotificationSettingsScreen
import com.chandan.master.presentation.profile.ProfileScreen
import com.chandan.master.presentation.profile.SecurityScreen
import com.chandan.master.presentation.splash.SplashScreen
import com.chandan.master.presentation.tasks.TaskEditScreen
import com.chandan.master.presentation.tasks.TasksScreen

@Composable
fun MasterNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    fun go(route: String) = navController.navigate(route) { launchSingleTop = true }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
        // Restrained transitions: short cross-fades only.
        enterTransition = { fadeIn(tween(180)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = { fadeOut(tween(120)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = { navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } } })
        }
        composable(Routes.HOME) {
            HomeScreen(
                HomeActions(
                    onOpenProfile = { go(Routes.PROFILE) }, onAddTask = { go(Routes.taskEdit()) }, onAddHabit = { go(Routes.habitEdit()) },
                    onOpenFocus = { go(Routes.FOCUS) }, onAddNote = { go(Routes.noteEdit()) }, onOpenCalendar = { go(Routes.CALENDAR) },
                    onOpenNotes = { go(Routes.NOTES) }, onOpenTask = { go(Routes.taskEdit(it)) },
                ),
            )
        }
        composable(Routes.TASKS) { TasksScreen(onAddTask = { go(Routes.taskEdit()) }, onOpenTask = { go(Routes.taskEdit(it)) }) }
        composable(Routes.HABITS) { HabitsScreen(onAddHabit = { go(Routes.habitEdit()) }, onOpenHabit = { go(Routes.habitEdit(it)) }) }
        composable(Routes.INSIGHTS) { InsightsScreen() }

        composable(
            Routes.TASK_EDIT,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("date") { type = NavType.LongType; defaultValue = Long.MIN_VALUE },
            ),
        ) { TaskEditScreen(onBack = back) }
        composable(Routes.HABIT_EDIT, arguments = listOf(navArgument("habitId") { type = NavType.LongType; defaultValue = -1L })) { HabitEditScreen(onBack = back) }
        composable(Routes.NOTE_EDIT, arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = -1L })) { NoteEditScreen(onBack = back) }

        composable(Routes.FOCUS) { FocusScreen(onBack = back) }
        composable(Routes.CALENDAR) {
            CalendarScreen(onBack = back, onAddTask = { go(Routes.taskEdit(date = it)) }, onOpenTask = { go(Routes.taskEdit(it)) })
        }
        composable(Routes.NOTES) { NotesScreen(onBack = back, onAddNote = { go(Routes.noteEdit()) }, onOpenNote = { go(Routes.noteEdit(it)) }) }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = back, onEditProfile = { go(Routes.EDIT_PROFILE) }, onOpenSecurity = { go(Routes.SECURITY) },
                onOpenNotifications = { go(Routes.NOTIFICATIONS) }, onOpenBackup = { go(Routes.BACKUP) },
                onOpenCategories = { go(Routes.CATEGORIES) }, onOpenAbout = { go(Routes.ABOUT) },
            )
        }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen(onBack = back) }
        composable(Routes.SECURITY) { SecurityScreen(onBack = back) }
        composable(Routes.NOTIFICATIONS) { NotificationSettingsScreen(onBack = back) }
        composable(Routes.BACKUP) { BackupScreen(onBack = back) }
        composable(Routes.CATEGORIES) { CategoriesScreen(onBack = back) }
        composable(Routes.ABOUT) { AboutScreen(onBack = back) }
    }
}
