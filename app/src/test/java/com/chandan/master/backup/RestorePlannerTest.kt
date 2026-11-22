package com.chandan.master.backup

import com.chandan.master.domain.backup.BackupData
import com.chandan.master.domain.backup.ExistingData
import com.chandan.master.domain.backup.RestorePlanner
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.HabitCompletion
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Task
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestorePlannerTest {
    private val d = LocalDate.of(2026, 3, 10)
    private val none = ExistingData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    private fun backup(
        cats: List<Category> = emptyList(), tasks: List<Task> = emptyList(), habits: List<Habit> = emptyList(),
        comps: List<HabitCompletion> = emptyList(), sessions: List<FocusSession> = emptyList(), notes: List<Note> = emptyList(),
    ) = BackupData(exportedAtMs = 0, appVersion = "t", profileUsername = null, preferences = null, categories = cats, tasks = tasks, habits = habits, completions = comps, sessions = sessions, notes = notes)

    private val cats = listOf(Category(1, "Work"))
    private val tasks = listOf(Task(10, "Report", dueDate = d, categoryId = 1, createdAtMs = 5), Task(11, "Gym", createdAtMs = 6))
    private val habits = listOf(Habit(20, "Read", startDate = d.minusDays(5), createdAtMs = 3))
    private val comps = listOf(HabitCompletion(20, d), HabitCompletion(20, d.minusDays(1)))
    private val sessions = listOf(FocusSession(30, 1000, 1500, 1500, true))
    private val notes = listOf(Note(40, "Idea", "text", createdAtMs = 9))
    private val full = backup(cats, tasks, habits, comps, sessions, notes)

    @Test fun everythingIsNewIntoAnEmptyDatabase() {
        val p = RestorePlanner.plan(none, full)
        assertEquals(1, p.summary.categories.added); assertEquals(2, p.summary.tasks.added); assertEquals(1, p.summary.habits.added)
        assertEquals(2, p.summary.completions.added); assertEquals(1, p.summary.sessions.added); assertEquals(1, p.summary.notes.added)
        assertEquals(0, p.summary.tasks.skippedDuplicates); assertEquals(8, p.summary.totalAdded)
    }

    @Test fun restoringTheSameDataAgainAddsNothing() {
        val existing = ExistingData(
            listOf(Category(100, "work")), // different id and case, same identity
            tasks.mapIndexed { i, t -> t.copy(id = 200L + i) }, habits.map { it.copy(id = 300) },
            comps.map { it.copy(habitId = 300) }, sessions.map { it.copy(id = 400) }, notes.map { it.copy(id = 500) },
        )
        val p = RestorePlanner.plan(existing, full)
        assertEquals(0, p.summary.totalAdded)
        assertEquals(1, p.summary.categories.skippedDuplicates); assertEquals(2, p.summary.completions.skippedDuplicates)
        assertEquals(100L, p.existingCategoryFor[1L]); assertEquals(300L, p.existingHabitFor[20L])
    }

    @Test fun onlyMissingRecordsAreAddedWhenPartiallyPresent() {
        val existing = none.copy(tasks = listOf(tasks[0].copy(id = 9)), habits = listOf(habits[0].copy(id = 8)), completions = listOf(HabitCompletion(8, d)))
        val p = RestorePlanner.plan(existing, full)
        assertEquals(listOf("Gym"), p.newTasks.map { it.title })
        assertEquals(listOf(d.minusDays(1)), p.newCompletions.map { it.date })
        assertTrue(p.newHabits.isEmpty()); assertEquals(8L, p.existingHabitFor[20L])
    }

    @Test fun duplicatesInsideTheBackupItselfAreCollapsed() {
        val twice = backup(
            cats = listOf(Category(1, "Work"), Category(2, "WORK")), tasks = listOf(tasks[0], tasks[0].copy(id = 99)),
            habits = listOf(habits[0], habits[0].copy(id = 21)), sessions = listOf(sessions[0], sessions[0].copy(id = 31)), notes = listOf(notes[0], notes[0].copy(id = 41)),
        )
        val p = RestorePlanner.plan(none, twice)
        assertEquals(1, p.newCategories.size); assertEquals(1, p.newTasks.size); assertEquals(1, p.newHabits.size); assertEquals(1, p.newSessions.size); assertEquals(1, p.newNotes.size)
    }

    @Test fun sameTitleDifferentCreationTimeIsADifferentTask() {
        val existing = none.copy(tasks = listOf(Task(1, "Gym", createdAtMs = 1000)))
        assertEquals(1, RestorePlanner.plan(existing, backup(tasks = listOf(Task(11, "Gym", createdAtMs = 6)))).newTasks.size)
    }

    @Test fun planDoesNotMutateItsInputs() {
        val existing = none.copy(categories = listOf(Category(1, "Work")))
        RestorePlanner.plan(existing, full)
        assertEquals(1, existing.categories.size)
    }
}
