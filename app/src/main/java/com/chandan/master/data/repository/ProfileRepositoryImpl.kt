package com.chandan.master.data.repository

import com.chandan.master.core.time.DateTimeProvider
import com.chandan.master.data.local.db.dao.ProfileDao
import com.chandan.master.data.local.db.entity.ProfileEntity
import com.chandan.master.domain.model.UserProfile
import com.chandan.master.domain.repository.AvatarRepository
import com.chandan.master.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val dao: ProfileDao,
    private val avatars: AvatarRepository,
    private val clock: DateTimeProvider,
) : ProfileRepository {
    override fun observeActiveProfile(): Flow<UserProfile?> = dao.observeActive().map { it?.toDomain() }

    override suspend fun saveProfile(profile: UserProfile): Long = dao.upsert(profile.toEntity())

    override suspend fun updateUsername(username: String) = dao.updateUsername(username, clock.now().toEpochMilli())

    override suspend fun updateAvatarPath(path: String?) {
        val old = dao.getActive()?.avatarUri
        dao.updateAvatar(path, clock.now().toEpochMilli())
        if (old != null && old != path) avatars.delete(old)
    }
}

internal fun ProfileEntity.toDomain() = UserProfile(
    id = id, username = username, avatarUri = avatarUri, isGuest = isGuest,
    createdAtEpochMillis = createdAtEpochMillis, updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun UserProfile.toEntity() = ProfileEntity(
    id = id, username = username, avatarUri = avatarUri, isGuest = isGuest,
    createdAtEpochMillis = createdAtEpochMillis, updatedAtEpochMillis = updatedAtEpochMillis,
)
