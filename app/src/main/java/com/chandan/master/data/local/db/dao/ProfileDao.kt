package com.chandan.master.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chandan.master.data.local.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    /** The active profile. Master supports a single local profile. */
    @Query("SELECT * FROM profiles ORDER BY id ASC LIMIT 1")
    fun observeActive(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity): Long

    @Query("SELECT * FROM profiles ORDER BY id ASC LIMIT 1")
    suspend fun getActive(): ProfileEntity?

    @Query("UPDATE profiles SET username = :username, updated_at = :now WHERE id = (SELECT id FROM profiles ORDER BY id ASC LIMIT 1)")
    suspend fun updateUsername(username: String, now: Long)

    @Query("UPDATE profiles SET avatar_uri = :path, updated_at = :now WHERE id = (SELECT id FROM profiles ORDER BY id ASC LIMIT 1)")
    suspend fun updateAvatar(path: String?, now: Long)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}
