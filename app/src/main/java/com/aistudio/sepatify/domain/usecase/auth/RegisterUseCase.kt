package com.aistudio.sepatify.domain.usecase.auth

import com.aistudio.sepatify.domain.model.UserProfile
import com.aistudio.sepatify.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        displayName: String,
        password: String
    ): Result<UserProfile?> {
        return authRepository.signUp(email, password, displayName).mapCatching {
            authRepository.currentProfile()
        }
    }
}
