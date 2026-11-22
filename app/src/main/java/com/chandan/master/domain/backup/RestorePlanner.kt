package com.chandan.master.domain.backup

import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.HabitCompletion
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Task
import java.time.LocalDate

data class ExistingData(
    val categories: List<Category>,
    val tasks: List<Task>,
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
    val sessions: List<FocusSession>,
    val notes: List<Note>,
)

data class CountPair(val added: Int, val skippedDuplicates: Int)

data class RestoreSummary(
    val categories: CountPair, val tasks: CountPair, val habits: CountPair,
    val completions: CountPair, val sessions: CountPair, val notes: CountPair,
) {
    val totalAdded: Int get() = categories.added + tasks.added + habits.added + completions.added + sessions.added + notes.added
}

/**
 * Restore = MERGE, never overwrite or delete. Records that already exist (same identity key) are skipped, so
 * restoring the same file twice adds nothing the second time. IDs inside the backup file are only used to
 * connect records to each other; the database assigns fresh IDs.
 */
data class RestorePlan(
    val newCategories: List<Category>,
    val newTasks: List<Task>,
    val newHabits: List<Habit>,
    /** habitId here is the BACKUP habit id. */
    val newCompletions: List<HabitCompletion>,
    val newSessions: List<FocusSession>,
    val newNotes: List<Note>,
    /** Backup category id -> id of an identical existing category (absent = it is new). */
    val existingCategoryFor: Map<Long, Long>,
    /** Backup habit id -> id of an identical existing habit (absent = it is new). */
    val existingHabitFor: Map<Long, Long>,
    val summary: RestoreSummary,
)

object RestorePlanner {
    private fun catKey(c: Category) = c.name.trim().lowercase()
    private fun taskKey(t: Task) = listOf(t.title.trim().lowercase(), t.dueDate, t.dueMinuteOfDay, t.createdAtMs)
    private fun habitKey(h: Habit) = listOf(h.name.trim().lowercase(), h.createdAtMs)
    private fun noteKey(n: Note) = listOf(n.title.trim().lowercase(), n.createdAtMs)

    fun plan(existing: ExistingData, backup: BackupData): RestorePlan {
        // categories
        val catByKey = existing.categories.associate { catKey(it) to it.id }.toMutableMap()
        val existingCategoryFor = HashMap<Long, Long>()
        val newCategories = ArrayList<Category>()
        for (c in backup.categories) {
            val key = catKey(c)
            val hit = catByKey[key]
            if (hit != null) existingCategoryFor[c.id] = hit else { newCategories += c; catByKey[key] = -c.id - 1 } // placeholder marks "seen"
        }
        val categoryDupes = backup.categories.size - newCategories.size

        // tasks
        val seenTasks = existing.tasks.map { taskKey(it) }.toMutableSet()
        val newTasks = backup.tasks.filter { seenTasks.add(taskKey(it)) }

        // habits
        val habitByKey = existing.habits.associate { habitKey(it) to it.id }.toMutableMap()
        val existingHabitFor = HashMap<Long, Long>()
        val newHabits = ArrayList<Habit>()
        for (h in backup.habits) {
            val key = habitKey(h)
            val hit = habitByKey[key]
            if (hit != null && hit >= 0) existingHabitFor[h.id] = hit
            else if (hit == null) { newHabits += h; habitByKey[key] = -h.id - 1 }
            // hit < 0: duplicate inside the backup itself; treated as skipped
        }
        val backupHabitKey = backup.habits.associate { it.id to habitKey(it) }

        // completions: identity = (habit identity, date)
        val existingHabitKey = existing.habits.associate { it.id to habitKey(it) }
        val seenCompletions: MutableSet<Pair<List<Any?>, LocalDate>> = existing.completions
            .mapNotNull { c -> existingHabitKey[c.habitId]?.let { it to c.date } }.toMutableSet()
        val newCompletions = backup.completions.filter { c ->
            val key = backupHabitKey[c.habitId] ?: return@filter false
            seenCompletions.add(key to c.date)
        }

        // sessions & notes
        val seenSessions = existing.sessions.map { it.startedAtMs }.toMutableSet()
        val newSessions = backup.sessions.filter { seenSessions.add(it.startedAtMs) }
        val seenNotes = existing.notes.map { noteKey(it) }.toMutableSet()
        val newNotes = backup.notes.filter { seenNotes.add(noteKey(it)) }

        val summary = RestoreSummary(
            CountPair(newCategories.size, categoryDupes),
            CountPair(newTasks.size, backup.tasks.size - newTasks.size),
            CountPair(newHabits.size, backup.habits.size - newHabits.size),
            CountPair(newCompletions.size, backup.completions.size - newCompletions.size),
            CountPair(newSessions.size, backup.sessions.size - newSessions.size),
            CountPair(newNotes.size, backup.notes.size - newNotes.size),
        )
        return RestorePlan(newCategories, newTasks, newHabits, newCompletions, newSessions, newNotes, existingCategoryFor, existingHabitFor, summary)
    }
}
