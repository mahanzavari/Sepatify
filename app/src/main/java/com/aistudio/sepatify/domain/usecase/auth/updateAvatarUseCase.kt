package com.aistudio.sepatify.domain.usecase.auth

import com.aistudio.sepatify.domain.repository.AuthRepository
import java.io.File

class UpdateAvatarUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(file: File): Result<String> =
        authRepository.uploadAvatar(file)
}
