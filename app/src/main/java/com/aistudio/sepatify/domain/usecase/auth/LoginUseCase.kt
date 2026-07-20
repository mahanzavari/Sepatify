package com.aistudio.sepatify.domain.usecase.auth

import com.aistudio.sepatify.domain.model.UserProfile
import com.aistudio.sepatify.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserProfile?> {
        return authRepository.signIn(email, password).mapCatching {
            authRepository.currentProfile()
        }
    }
}
