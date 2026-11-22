package com.chandan.master.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chandan.master.data.local.db.MasterDatabase
import com.chandan.master.data.local.db.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {
    private lateinit var db: MasterDatabase

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MasterDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    private fun entity(name: String, id: Long = 0) = ProfileEntity(id, name, null, false, 1L, 1L)

    @Test fun emptyDatabaseHasNoActiveProfile() = runTest {
        assertNull(db.profileDao().observeActive().first())
        assertEquals(0, db.profileDao().count())
    }

    @Test fun insertedProfileIsObserved() = runTest {
        db.profileDao().upsert(entity("ronin"))
        assertEquals("ronin", db.profileDao().observeActive().first()?.username)
    }

    @Test fun upsertWithSameIdEditsTheExistingRowInsteadOfDuplicating() = runTest {
        val id = db.profileDao().upsert(entity("ronin"))
        db.profileDao().upsert(entity("shogun", id))
        assertEquals(1, db.profileDao().count())
        assertEquals("shogun", db.profileDao().observeActive().first()?.username)
    }
}
