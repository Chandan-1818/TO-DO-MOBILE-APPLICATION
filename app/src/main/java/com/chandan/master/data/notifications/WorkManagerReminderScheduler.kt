package com.chandan.master.data.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.local.db.dao.HabitDao
import com.chandan.master.data.local.db.dao.TaskDao
import com.chandan.master.data.repository.toDomain
import com.chandan.master.domain.logic.ReminderCalculator
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.Task
import com.chandan.master.domain.repository.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One unique WorkManager job per task and per habit, replaced whenever the item changes and cancelled when it is
 * completed/deleted. WorkManager persists jobs across reboots. Android may delay jobs (battery optimisation, Doze),
 * so reminders are best-effort and not exact to the minute.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: DateTimeProvider,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
) : ReminderScheduler {
    private val work get() = WorkManager.getInstance(context)

    override suspend fun scheduleTask(task: Task) {
        val at = ReminderCalculator.taskTrigger(task, clock.zone(), clock.now())
        if (at == null) { cancelTask(task.id); return }
        val delay = (at.toEpochMilli() - clock.now().toEpochMilli()).coerceAtLeast(0)
        work.enqueueUniqueWork(
            taskName(task.id), ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<TaskReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(workDataOf(KEY_ID to task.id)).build(),
        )
    }

    override suspend fun cancelTask(taskId: Long) { work.cancelUniqueWork(taskName(taskId)) }

    override suspend fun scheduleHabit(habit: Habit) {
        val minute = habit.reminderMinuteOfDay
        val at = if (habit.archived || minute == null) null
        else ReminderCalculator.nextHabitTrigger(habit.scheduleMask, minute, habit.startDate, clock.zone(), clock.now())
        if (at == null) { cancelHabit(habit.id); return }
        val delay = (at.toEpochMilli() - clock.now().toEpochMilli()).coerceAtLeast(0)
        work.enqueueUniqueWork(
            habitName(habit.id), ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<HabitReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(workDataOf(KEY_ID to habit.id)).build(),
        )
    }

    override suspend fun cancelHabit(habitId: Long) { work.cancelUniqueWork(habitName(habitId)) }

    override suspend fun rescheduleAll() {
        taskDao.getAll().forEach { scheduleTask(it.toDomain()) }
        habitDao.getAll().forEach { scheduleHabit(it.toDomain()) }
    }

    override fun scheduleFocusFinish(delayMillis: Long) {
        work.enqueueUniqueWork(
            FOCUS_NAME, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<FocusFinishWorker>().setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS).build(),
        )
    }

    override fun cancelFocusFinish() { work.cancelUniqueWork(FOCUS_NAME) }

    override fun cancelAll() { work.cancelAllWork() }

    companion object {
        const val KEY_ID = "id"
        const val FOCUS_NAME = "focus-finish"
        const val RESCHEDULE_NAME = "reschedule-all"
        fun taskName(id: Long) = "task-reminder-$id"
        fun habitName(id: Long) = "habit-reminder-$id"
    }
}
