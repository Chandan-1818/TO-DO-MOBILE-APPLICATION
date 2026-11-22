package com.chandan.master.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local profile row. Credentials (PIN hash/salt) are deliberately NOT stored here; they will live in a
 * separate, Keystore-protected store (see CredentialStore). No email or phone columns exist.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    @ColumnInfo(name = "avatar_uri") val avatarUri: String?,
    @ColumnInfo(name = "is_guest") val isGuest: Boolean,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
)
