package com.chandan.master.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import com.chandan.master.R

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val TASKS = "tasks"
    const val HABITS = "habits"
    const val INSIGHTS = "insights"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val SECURITY = "security"
    const val WELCOME = "welcome"
    const val CREATE_ACCOUNT = "create_account"
    const val FOCUS = "focus"
    const val CALENDAR = "calendar"
    const val NOTES = "notes"
    const val NOTIFICATIONS = "notifications"
    const val BACKUP = "backup"
    const val CATEGORIES = "categories"
    const val ABOUT = "about"
    const val TASK_EDIT = "task_edit?taskId={taskId}&date={date}"
    const val HABIT_EDIT = "habit_edit?habitId={habitId}"
    const val NOTE_EDIT = "note_edit?noteId={noteId}"

    /** Pass null id for "new". [date] pre-fills the due date (epoch day). */
    fun taskEdit(taskId: Long? = null, date: java.time.LocalDate? = null) = "task_edit?taskId=${taskId ?: -1L}&date=${date?.toEpochDay() ?: Long.MIN_VALUE}"
    fun habitEdit(habitId: Long? = null) = "habit_edit?habitId=${habitId ?: -1L}"
    fun noteEdit(noteId: Long? = null) = "note_edit?noteId=${noteId ?: -1L}"
}

/** The four bottom-navigation destinations. Everything else is reached from them (Profile from Home). */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    TASKS(Routes.TASKS, R.string.nav_tasks, Icons.Outlined.TaskAlt, Icons.Filled.TaskAlt),
    HABITS(Routes.HABITS, R.string.nav_habits, Icons.Outlined.Repeat, Icons.Filled.Repeat),
    INSIGHTS(Routes.INSIGHTS, R.string.nav_insights, Icons.Outlined.BarChart, Icons.Filled.BarChart),
}
