package com.chandan.master.domain.model

/** Local profile. Contains no email address and no phone number, by design. */
data class UserProfile(
    val id: Long,
    val username: String,
    /** Content URI string of a user-selected picture, or null to use the default samurai avatar. */
    val avatarUri: String?,
    val isGuest: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
