package com.chandan.master.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.local.db.MasterDatabase
import com.chandan.master.data.local.preferences.PreferencesDataSource
import com.chandan.master.data.repository.AccountRepositoryImpl
import com.chandan.master.data.security.CredentialStore
import com.chandan.master.data.security.KeystoreCrypto
import com.chandan.master.domain.auth.BiometricCipherResult
import com.chandan.master.domain.auth.BiometricKeyProvider
import com.chandan.master.domain.auth.PinVerification
import com.chandan.master.domain.repository.AvatarRepository
import com.chandan.master.domain.repository.ChangePinResult
import com.chandan.master.domain.repository.CreateAccountResult
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device/emulator with the REAL Android Keystore, a real Room DB and real DataStore files. */
@RunWith(AndroidJUnit4::class)
class AccountRepositoryTest {
    private class Clock(var now: Instant = Instant.parse("2026-01-01T00:00:00Z")) : DateTimeProvider {
        override fun now() = now
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }
    private class NoAvatars : AvatarRepository {
        override suspend fun importFromUri(uri: String): String? = null
        override suspend fun delete(path: String?) = Unit
        override suspend fun deleteAll() = Unit
    }
    private class NoReminders : com.chandan.master.domain.repository.ReminderScheduler {
        override suspend fun scheduleTask(task: com.chandan.master.domain.model.Task) = Unit
        override suspend fun cancelTask(taskId: Long) = Unit
        override suspend fun scheduleHabit(habit: com.chandan.master.domain.model.Habit) = Unit
        override suspend fun cancelHabit(habitId: Long) = Unit
        override suspend fun rescheduleAll() = Unit
        override fun scheduleFocusFinish(delayMillis: Long) = Unit
        override fun cancelFocusFinish() = Unit
        override fun cancelAll() = Unit
    }
    private class NoBiometrics : BiometricKeyProvider {
        override fun prepareEnrollment() = BiometricCipherResult.Invalidated
        override fun cipherForUnlock() = BiometricCipherResult.Invalidated
        override fun deleteKey() = Unit
    }

    private lateinit var db: MasterDatabase
    private lateinit var repo: AccountRepositoryImpl
    private lateinit var credentials: CredentialStore
    private val clock = Clock()
    private val files = mutableListOf<File>()

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MasterDatabase::class.java).allowMainThreadQueries().build()
        fun store(name: String) = PreferenceDataStoreFactory.create { File(context.cacheDir, "$name-${System.nanoTime()}.preferences_pb").also(files::add) }
        credentials = CredentialStore(store("cred"), KeystoreCrypto())
        repo = AccountRepositoryImpl(db.profileDao(), credentials, NoAvatars(), db, clock, NoBiometrics(), PreferencesDataSource(store("settings")), NoReminders(), com.chandan.master.data.focus.FocusStateStore(store("focus")))
        runBlocking { credentials.clear() }
    }

    @After fun tearDown() { db.close(); files.forEach { it.delete() }; runBlocking { credentials.clear() } }

    @Test fun createAccountThenCorrectPinSucceedsAndWrongPinFails() = runBlocking {
        assertEquals(CreateAccountResult.Success, repo.createAccount("ronin", "2580", null))
        assertTrue(repo.hasAccount.first())
        assertEquals(PinVerification.Success, repo.verifyPin("2580"))
        val wrong = repo.verifyPin("0000")
        assertTrue(wrong is PinVerification.Wrong && wrong.attemptsLeftBeforeDelay == 4)
    }

    @Test fun pinIsNeverStoredInPlainText() = runBlocking {
        repo.createAccount("ronin", "2580", null)
        val dump = files.filter { it.exists() }.joinToString { String(it.readBytes(), Charsets.ISO_8859_1) }
        assertFalse(dump.contains("2580"))
    }

    @Test fun fiveWrongAttemptsStartADelayThatBlocksEvenTheCorrectPinUntilItExpires() = runBlocking {
        repo.createAccount("ronin", "2580", null)
        repeat(5) { repo.verifyPin("1111") }
        assertTrue(repo.verifyPin("2580") is PinVerification.LockedOut)
        clock.now = clock.now.plusSeconds(31)
        assertEquals(PinVerification.Success, repo.verifyPin("2580"))
    }

    @Test fun successResetsTheFailureCounter() = runBlocking {
        repo.createAccount("ronin", "2580", null)
        repeat(3) { repo.verifyPin("1111") }
        assertEquals(PinVerification.Success, repo.verifyPin("2580"))
        val wrong = repo.verifyPin("1111")
        assertTrue(wrong is PinVerification.Wrong && wrong.attemptsLeftBeforeDelay == 4)
    }

    @Test fun changePinRequiresTheCurrentPin() = runBlocking {
        repo.createAccount("ronin", "2580", null)
        assertTrue(repo.changePin("9999", "1357") is ChangePinResult.CurrentPinRejected)
        assertEquals(ChangePinResult.Success, repo.changePin("2580", "135790"))
        assertEquals(PinVerification.Success, repo.verifyPin("135790"))
        assertTrue(repo.verifyPin("2580") is PinVerification.Wrong)
        assertEquals(6, repo.pinLength.first())
    }

    @Test fun invalidInputIsRejectedAndNothingIsCreated() = runBlocking {
        assertTrue(repo.createAccount("x", "12", null) is CreateAccountResult.Invalid)
        assertFalse(repo.hasAccount.first())
    }

    @Test fun secondAccountIsRefused() = runBlocking {
        repo.createAccount("ronin", "2580", null)
        assertEquals(CreateAccountResult.AlreadyExists, repo.createAccount("other", "1357", null))
    }

    @Test fun resetDeletesEverythingAndAllowsANewAccount() = runBlocking {
        repo.createAccount("ronin", "2580", null)
        repo.resetAccount()
        assertFalse(repo.hasAccount.first())
        assertEquals(null, repo.pinLength.first())
        assertEquals(PinVerification.NoAccount, repo.verifyPin("2580"))
        assertEquals(CreateAccountResult.Success, repo.createAccount("new", "1357", null))
    }
}
