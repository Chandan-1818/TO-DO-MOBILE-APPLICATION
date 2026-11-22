package com.chandan.master.di

import android.content.Context
import androidx.room.Room
import com.chandan.master.data.local.db.MasterDatabase
import com.chandan.master.data.local.db.MasterMigrations
import com.chandan.master.data.local.db.dao.ProfileDao
import com.chandan.master.data.local.db.dao.NoteDao
import com.chandan.master.data.local.db.dao.FocusSessionDao
import com.chandan.master.data.local.db.dao.HabitCompletionDao
import com.chandan.master.data.local.db.dao.HabitDao
import com.chandan.master.data.local.db.dao.TaskDao
import com.chandan.master.data.local.db.dao.CategoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MasterDatabase =
        Room.databaseBuilder(context, MasterDatabase::class.java, MasterDatabase.NAME)
            .addMigrations(*MasterMigrations.ALL)
            // No fallbackToDestructiveMigration(): upgrades must never wipe user data.
            .build()

    @Provides fun provideProfileDao(db: MasterDatabase): ProfileDao = db.profileDao()
    @Provides fun provideCategoryDao(db: MasterDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTaskDao(db: MasterDatabase): TaskDao = db.taskDao()
    @Provides fun provideHabitDao(db: MasterDatabase): HabitDao = db.habitDao()
    @Provides fun provideHabitCompletionDao(db: MasterDatabase): HabitCompletionDao = db.habitCompletionDao()
    @Provides fun provideFocusSessionDao(db: MasterDatabase): FocusSessionDao = db.focusSessionDao()
    @Provides fun provideNoteDao(db: MasterDatabase): NoteDao = db.noteDao()
}
