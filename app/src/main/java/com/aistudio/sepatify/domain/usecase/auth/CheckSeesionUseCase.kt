package com.aistudio.sepatify.domain.usecase.auth

import com.aistudio.sepatify.domain.model.UserProfile
import com.aistudio.sepatify.domain.repository.AuthRepository

class CheckSessionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Pair<Boolean, UserProfile?> {
        val hasSession = authRepository.hasValidSession()
        val profile = if (hasSession) authRepository.currentProfile() else null
        return hasSession to profile
    }
}
