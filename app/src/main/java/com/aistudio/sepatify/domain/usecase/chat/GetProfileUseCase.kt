package com.aistudio.sepatify.domain.usecase.chat

import com.aistudio.sepatify.domain.model.UserProfile
import com.aistudio.sepatify.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetProfileUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(username: String): Flow<UserProfile?> =
        chatRepository.getProfileFlow(username)
}

class GetUserProfileDetailsUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(username: String) = chatRepository.getUserProfileDetails(username)
}
