package com.chandan.master.data.repository

import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.local.db.MasterDatabase
import com.chandan.master.data.local.db.dao.ProfileDao
import com.chandan.master.data.local.db.entity.ProfileEntity
import com.chandan.master.data.local.preferences.PreferencesDataSource
import com.chandan.master.data.security.CredentialStore
import com.chandan.master.data.security.CredentialsUnreadableException
import com.chandan.master.domain.auth.AttemptTracker
import com.chandan.master.domain.auth.BiometricKeyProvider
import com.chandan.master.domain.auth.LockoutPolicy
import com.chandan.master.domain.auth.PinHasher
import com.chandan.master.domain.auth.PinValidator
import com.chandan.master.domain.auth.PinVerification
import com.chandan.master.domain.auth.UsernameValidator
import com.chandan.master.domain.repository.AccountRepository
import com.chandan.master.domain.repository.AvatarRepository
import com.chandan.master.domain.repository.ChangePinResult
import com.chandan.master.domain.repository.CreateAccountResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val credentials: CredentialStore,
    private val avatars: AvatarRepository,
    private val database: MasterDatabase,
    private val clock: DateTimeProvider,
    private val biometricKeys: BiometricKeyProvider,
    private val settings: PreferencesDataSource,
    private val reminders: com.chandan.master.domain.repository.ReminderScheduler,
    private val focusState: com.chandan.master.data.focus.FocusStateStore,
) : AccountRepository {

    /** Serialises PIN checks so parallel attempts cannot bypass the failed-attempt counter. */
    private val pinMutex = Mutex()

    /**
     * An account exists when a profile row exists. If its PIN credentials are missing or unreadable the user
     * still gets the lock screen, which explains the problem and offers the (warned) reset.
     */
    override val hasAccount: Flow<Boolean> = profileDao.observeActive().map { it != null }.distinctUntilChanged()

    override val pinLength: Flow<Int?> = credentials.pinLength

    override suspend fun createAccount(username: String, pin: String, avatarPath: String?): CreateAccountResult {
        val name = UsernameValidator.normalize(username)
        val usernameError = UsernameValidator.validate(name)
        val pinError = PinValidator.validate(pin)
        if (usernameError != null || pinError != null) return CreateAccountResult.Invalid(usernameError, pinError)
        if (hasAccount.first()) return CreateAccountResult.AlreadyExists

        return try {
            // Leftovers from an interrupted earlier reset must never leak into a new account.
            withContext(Dispatchers.IO) { database.clearAllTables() }
            val record = withContext(Dispatchers.Default) { PinHasher.create(pin.toCharArray()) }
            credentials.save(record, pin.length)
            val now = clock.now().toEpochMilli()
            profileDao.upsert(ProfileEntity(username = name, avatarUri = avatarPath, isGuest = false, createdAtEpochMillis = now, updatedAtEpochMillis = now))
            CreateAccountResult.Success
        } catch (e: Exception) {
            runCatching { credentials.clear() }
            CreateAccountResult.StorageFailure
        }
    }

    override suspend fun verifyPin(pin: String): PinVerification {
        return pinMutex.withLock {
            val now = clock.now().toEpochMilli()
            val attempts = credentials.loadAttempts()
            val remaining = AttemptTracker.remainingLockMillis(attempts, now)
            if (remaining > 0) return@withLock PinVerification.LockedOut(remaining)

            val stored = try {
                credentials.load() ?: return@withLock PinVerification.NoAccount
            } catch (e: CredentialsUnreadableException) {
                return@withLock PinVerification.CredentialsUnavailable
            }

            val ok = withContext(Dispatchers.Default) { PinHasher.verify(pin.toCharArray(), stored.record) }
            if (ok) {
                credentials.saveAttempts(AttemptTracker.onSuccess())
                PinVerification.Success
            } else {
                val next = AttemptTracker.onFailure(attempts, now)
                credentials.saveAttempts(next)
                PinVerification.Wrong(LockoutPolicy.freeAttemptsLeft(next.failedAttempts), AttemptTracker.remainingLockMillis(next, now))
            }
        }
    }

    override suspend fun lockoutRemainingMillis(): Long =
        AttemptTracker.remainingLockMillis(credentials.loadAttempts(), clock.now().toEpochMilli())

    override suspend fun changePin(currentPin: String, newPin: String): ChangePinResult {
        PinValidator.validate(newPin)?.let { return ChangePinResult.InvalidNewPin(it) }
        val check = verifyPin(currentPin)
        if (check != PinVerification.Success) return ChangePinResult.CurrentPinRejected(check)
        return try {
            val record = withContext(Dispatchers.Default) { PinHasher.create(newPin.toCharArray()) }
            credentials.save(record, newPin.length)
            ChangePinResult.Success
        } catch (e: Exception) {
            ChangePinResult.StorageFailure
        }
    }

    override suspend fun resetAccount() {
        reminders.cancelAll()
        focusState.clear()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        avatars.deleteAll()
        biometricKeys.deleteKey()
        settings.setBiometricEnabled(false)
        credentials.clear()
    }
}
