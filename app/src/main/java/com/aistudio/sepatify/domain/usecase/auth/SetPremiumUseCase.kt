package com.aistudio.sepatify.domain.usecase.auth

import com.aistudio.sepatify.domain.repository.AuthRepository

class SetPremiumUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(isPremium: Boolean): Result<Unit> =
        authRepository.setPremium(isPremium)
}
