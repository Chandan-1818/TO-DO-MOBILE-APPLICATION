package com.chandan.master.domain.usecase

import com.chandan.master.domain.model.UserProfile
import com.chandan.master.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveActiveProfileUseCase @Inject constructor(private val repo: ProfileRepository) {
    operator fun invoke(): Flow<UserProfile?> = repo.observeActiveProfile()
}
