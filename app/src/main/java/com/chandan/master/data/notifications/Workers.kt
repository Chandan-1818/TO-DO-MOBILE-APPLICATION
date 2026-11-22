package com.chandan.master.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.focus.FocusTimerController
import com.chandan.master.domain.repository.HabitRepository
import com.chandan.master.domain.repository.ReminderScheduler
import com.chandan.master.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerDependencies {
    fun tasks(): TaskRepository
    fun habits(): HabitRepository
    fun notifier(): AppNotifier
    fun scheduler(): ReminderScheduler
    fun focus(): FocusTimerController
    fun clock(): DateTimeProvider
}

private fun Context.deps(): WorkerDependencies = EntryPointAccessors.fromApplication(applicationContext, WorkerDependencies::class.java)

class TaskReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val d = applicationContext.deps()
        val task = d.tasks().getTask(inputData.getLong(WorkManagerReminderScheduler.KEY_ID, -1)) ?: return Result.success()
        if (!task.completed) d.notifier().notifyTask(task)
        return Result.success()
    }
}

class HabitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val d = applicationContext.deps()
        val habit = d.habits().getHabit(inputData.getLong(WorkManagerReminderScheduler.KEY_ID, -1)) ?: return Result.success()
        if (habit.archived) return Result.success()
        // Only nudge if today's check-in is still missing.
        val today = d.clock().today()
        val doneToday = d.habits().observeCompletions().first()[habit.id].orEmpty().contains(today)
        if (!doneToday) d.notifier().notifyHabit(habit)
        d.scheduler().scheduleHabit(habit) // chain: schedule the next occurrence
        return Result.success()
    }
}

class FocusFinishWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.deps().focus().checkFinished(notify = true)
        return Result.success()
    }
}

class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.deps().scheduler().rescheduleAll()
        return Result.success()
    }
}
