package com.chandan.master.data.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chandan.master.di.CredentialsStore
import com.chandan.master.domain.auth.AttemptState
import com.chandan.master.domain.auth.PinRecord
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.datastore.preferences.core.emptyPreferences

class StoredCredentials(val record: PinRecord, val pinLength: Int)

/** Persists the salted PIN verifier (encrypted by the Keystore) and the failed-attempt counters. Never logs. */
@Singleton
class CredentialStore @Inject constructor(
    @CredentialsStore private val dataStore: DataStore<Preferences>,
    private val crypto: KeystoreCrypto,
) {
    private val data: Flow<Preferences> = dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val hasCredentials: Flow<Boolean> = data.map { it.contains(Keys.WRAPPED_HASH) }.distinctUntilChanged()
    val pinLength: Flow<Int?> = data.map { it[Keys.PIN_LENGTH] }.distinctUntilChanged()

    suspend fun save(record: PinRecord, pinLength: Int) {
        val wrapped = crypto.encrypt(record.hash)
        dataStore.edit {
            it[Keys.SALT] = record.salt.b64()
            it[Keys.ITERATIONS] = record.iterations
            it[Keys.WRAPPED_HASH] = wrapped.b64()
            it[Keys.PIN_LENGTH] = pinLength
            it[Keys.FAILED] = 0
            it[Keys.LOCKED_UNTIL] = 0L
        }
    }

    /** Null when no PIN is set. Throws [CredentialsUnreadableException] if data exists but cannot be decrypted. */
    suspend fun load(): StoredCredentials? {
        val p = data.first()
        val wrapped = p[Keys.WRAPPED_HASH] ?: return null
        val salt = p[Keys.SALT]; val iterations = p[Keys.ITERATIONS]; val length = p[Keys.PIN_LENGTH]
        if (salt == null || iterations == null || length == null) throw CredentialsUnreadableException()
        val hash = try { Base64.decode(wrapped, Base64.NO_WRAP).let(crypto::decrypt) } catch (e: IllegalArgumentException) { throw CredentialsUnreadableException(e) }
        return StoredCredentials(PinRecord(Base64.decode(salt, Base64.NO_WRAP), iterations, hash), length)
    }

    suspend fun loadAttempts(): AttemptState {
        val p = data.first()
        return AttemptState(p[Keys.FAILED] ?: 0, p[Keys.LOCKED_UNTIL] ?: 0L)
    }

    suspend fun saveAttempts(state: AttemptState) {
        dataStore.edit { it[Keys.FAILED] = state.failedAttempts; it[Keys.LOCKED_UNTIL] = state.lockedUntilEpochMillis }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
        crypto.deleteKey()
    }

    private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private object Keys {
        val SALT = stringPreferencesKey("pin_salt")
        val ITERATIONS = intPreferencesKey("pin_iterations")
        val WRAPPED_HASH = stringPreferencesKey("pin_hash_wrapped")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val FAILED = intPreferencesKey("failed_attempts")
        val LOCKED_UNTIL = longPreferencesKey("locked_until_epoch_ms")
    }
}
