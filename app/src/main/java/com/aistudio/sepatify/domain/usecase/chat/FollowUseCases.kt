package com.aistudio.sepatify.domain.usecase.chat

import com.aistudio.sepatify.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ToggleFollowUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(username: String) = chatRepository.toggleFollowUser(username)
}

class IsFollowingUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(username: String): Flow<Boolean> = chatRepository.isFollowing(username)
}

class GetFollowedUsersUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<String>> = chatRepository.getFollowedUsers()
}
