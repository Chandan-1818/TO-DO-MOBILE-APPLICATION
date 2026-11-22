package com.chandan.master.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chandan.master.data.local.db.MasterDatabase
import com.chandan.master.data.local.db.entity.CategoryEntity
import com.chandan.master.data.local.db.entity.FocusSessionEntity
import com.chandan.master.data.local.db.entity.HabitCompletionEntity
import com.chandan.master.data.local.db.entity.HabitEntity
import com.chandan.master.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room (in-memory) with foreign keys on: proves no orphans, no duplicate check-ins/sessions, and persistence of edits. */
@RunWith(AndroidJUnit4::class)
class ProductivityDaoTest {
    private lateinit var db: MasterDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MasterDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun tearDown() = db.close()

    private fun task(title: String, cat: Long? = null) = TaskEntity(title = title, description = "", dueDay = 100, dueMinute = null, priority = 1, categoryId = cat, reminderOffset = null, completed = false, completedAt = null, createdAt = 1, updatedAt = 1)
    private fun habit(name: String) = HabitEntity(name = name, description = "", scheduleMask = 127, reminderMinute = null, startDay = 1, archived = false, createdAt = 1)

    @Test fun editUpdatesTheSameRowAndCompletionIsStored() = runBlocking {
        val id = db.taskDao().insert(task("a"))
        db.taskDao().update(db.taskDao().getById(id)!!.copy(title = "b"))
        db.taskDao().setCompleted(id, true, 55, 56)
        val t = db.taskDao().getById(id)!!
        assertEquals("b", t.title); assertEquals(true, t.completed); assertEquals(55L, t.completedAt)
        assertEquals(1, db.taskDao().getAll().size)
    }

    @Test fun deletingACategoryKeepsItsTasksButClearsTheLink() = runBlocking {
        val cat = db.categoryDao().insert(CategoryEntity(name = "Work"))
        val id = db.taskDao().insert(task("a", cat))
        db.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = $cat")
        assertNull(db.taskDao().getById(id)!!.categoryId)
    }

    @Test fun deletingAHabitRemovesItsHistory() = runBlocking {
        val h = db.habitDao().insert(habit("Read"))
        db.habitCompletionDao().insertIgnore(HabitCompletionEntity(habitId = h, day = 10))
        db.habitDao().delete(h)
        assertEquals(0, db.habitCompletionDao().getAll().size)
    }

    @Test fun duplicateCheckInsAndSessionsAreIgnored() = runBlocking {
        val h = db.habitDao().insert(habit("Read"))
        db.habitCompletionDao().insertIgnore(HabitCompletionEntity(habitId = h, day = 10))
        db.habitCompletionDao().insertIgnore(HabitCompletionEntity(habitId = h, day = 10))
        assertEquals(1, db.habitCompletionDao().getAll().size)
        db.habitCompletionDao().delete(h, 10)
        assertEquals(0, db.habitCompletionDao().observeAll().first().size)
        db.focusSessionDao().insertIgnore(FocusSessionEntity(startedAt = 5, plannedSeconds = 1500, actualSeconds = 1500, completed = true))
        db.focusSessionDao().insertIgnore(FocusSessionEntity(startedAt = 5, plannedSeconds = 1500, actualSeconds = 1500, completed = true))
        assertEquals(1, db.focusSessionDao().getAll().size)
    }
}
