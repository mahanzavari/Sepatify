package com.aistudio.sepatify.domain.usecase.auth

import com.aistudio.sepatify.domain.repository.AuthRepository

class UpdateDisplayNameUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String): Result<Unit> =
        authRepository.updateDisplayName(name)
}
