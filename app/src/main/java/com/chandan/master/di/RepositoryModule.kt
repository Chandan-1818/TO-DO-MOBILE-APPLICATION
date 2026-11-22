package com.chandan.master.di

import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.core.time.SystemDateTimeProvider
import com.chandan.master.data.backup.BackupRepositoryImpl
import com.chandan.master.data.notifications.WorkManagerReminderScheduler
import com.chandan.master.data.repository.AccountRepositoryImpl
import com.chandan.master.data.repository.CategoryRepositoryImpl
import com.chandan.master.data.repository.FocusRepositoryImpl
import com.chandan.master.data.repository.HabitRepositoryImpl
import com.chandan.master.data.repository.NoteRepositoryImpl
import com.chandan.master.data.repository.TaskRepositoryImpl
import com.chandan.master.domain.repository.BackupRepository
import com.chandan.master.domain.repository.CategoryRepository
import com.chandan.master.domain.repository.FocusRepository
import com.chandan.master.domain.repository.HabitRepository
import com.chandan.master.domain.repository.NoteRepository
import com.chandan.master.domain.repository.ReminderScheduler
import com.chandan.master.domain.repository.TaskRepository
import com.chandan.master.data.repository.PreferencesRepositoryImpl
import com.chandan.master.data.security.AndroidBiometricKeyProvider
import com.chandan.master.data.security.AvatarStorage
import com.chandan.master.domain.auth.BiometricKeyProvider
import com.chandan.master.domain.repository.AccountRepository
import com.chandan.master.domain.repository.AvatarRepository
import com.chandan.master.data.repository.ProfileRepositoryImpl
import com.chandan.master.domain.repository.PreferencesRepository
import com.chandan.master.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
    @Binds abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
    @Binds abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository
    @Binds abstract fun bindFocusRepository(impl: FocusRepositoryImpl): FocusRepository
    @Binds abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    @Binds abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
    @Binds abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
    @Binds abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
    @Binds abstract fun bindAvatarRepository(impl: AvatarStorage): AvatarRepository
    @Binds abstract fun bindBiometricKeyProvider(impl: AndroidBiometricKeyProvider): BiometricKeyProvider

    companion object {
        @Provides @Singleton
        fun provideDateTimeProvider(): DateTimeProvider = SystemDateTimeProvider()
    }
}
