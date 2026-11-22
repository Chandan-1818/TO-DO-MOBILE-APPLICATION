package com.chandan.master.domain.repository

/** Stores profile pictures privately inside the app (never in shared storage). */
interface AvatarRepository {
    /** Copies, downsizes (max 512 px) and stores the picked image. Returns the private file path, or null on failure. */
    suspend fun importFromUri(uri: String): String?
    suspend fun delete(path: String?)
    suspend fun deleteAll()
}
