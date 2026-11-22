package com.chandan.master.domain.repository

import com.chandan.master.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeActiveProfile(): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile): Long
    suspend fun updateUsername(username: String)
    /** Replaces the picture with the already-imported private file [path] (or restores the default when null) and deletes the old file. */
    suspend fun updateAvatarPath(path: String?)
}
