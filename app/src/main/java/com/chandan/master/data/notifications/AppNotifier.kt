package com.chandan.master.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chandan.master.MainActivity
import com.chandan.master.R
import com.chandan.master.domain.logic.SessionKind
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

object NotificationChannels {
    const val TASKS = "task_reminders"
    const val HABITS = "habit_reminders"
    const val FOCUS = "focus_timer"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(TASKS, context.getString(R.string.channel_tasks), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(HABITS, context.getString(R.string.channel_habits), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(FOCUS, context.getString(R.string.channel_focus), NotificationManager.IMPORTANCE_DEFAULT))
    }
}

/**
 * Posts Master's notifications, honouring the user's switches and Android's own permission/channel settings.
 * Content is private on the lock screen (a generic text is shown until the phone is unlocked).
 */
@Singleton
class AppNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesRepository,
) {
    suspend fun notifyTask(task: Task) {
        val p = prefs.preferences.first()
        if (!p.notificationsEnabled || !p.taskReminders) return
        post(NotificationChannels.TASKS, "task-${task.id}".hashCode(), context.getString(R.string.notif_task_title), task.title)
    }

    suspend fun notifyHabit(habit: Habit) {
        val p = prefs.preferences.first()
        if (!p.notificationsEnabled || !p.habitReminders) return
        post(NotificationChannels.HABITS, "habit-${habit.id}".hashCode(), context.getString(R.string.notif_habit_title), habit.name)
    }

    suspend fun notifyFocusFinished(kind: SessionKind) {
        val p = prefs.preferences.first()
        if (!p.notificationsEnabled || !p.focusNotifications) return
        val (title, text) = if (kind == SessionKind.FOCUS) context.getString(R.string.notif_focus_done_title) to context.getString(R.string.notif_focus_done_text)
        else context.getString(R.string.notif_break_done_title) to context.getString(R.string.notif_break_done_text)
        post(NotificationChannels.FOCUS, FOCUS_NOTIFICATION_ID, title, text)
    }

    private fun post(channel: String, id: Int, title: String, text: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val publicVersion = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.app_name)).setContentText(title).build()
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text)
            .setAutoCancel(true).setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publicVersion)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        try { nm.notify(id, n) } catch (e: SecurityException) { /* permission revoked meanwhile: nothing to do */ }
    }

    private companion object { const val FOCUS_NOTIFICATION_ID = 7001 }
}
