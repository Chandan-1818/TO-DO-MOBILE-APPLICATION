package com.chandan.master

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.chandan.master.data.notifications.NotificationChannels
import com.chandan.master.data.notifications.RescheduleWorker
import com.chandan.master.data.notifications.WorkManagerReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MasterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        // Self-healing: recompute every reminder on each launch (covers edits made while the app was killed, clock changes, etc.).
        WorkManager.getInstance(this).enqueueUniqueWork(
            WorkManagerReminderScheduler.RESCHEDULE_NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RescheduleWorker>().build(),
        )
    }
}
