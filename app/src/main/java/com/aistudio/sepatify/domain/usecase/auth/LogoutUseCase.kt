package com.aistudio.sepatify.domain.usecase.auth

import com.aistudio.sepatify.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.signOut()
}
