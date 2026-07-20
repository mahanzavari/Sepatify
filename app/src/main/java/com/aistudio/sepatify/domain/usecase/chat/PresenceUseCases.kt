package com.aistudio.sepatify.domain.usecase.chat

import com.aistudio.sepatify.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class TrackPresenceUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() = chatRepository.trackPresence()
}

class UntrackPresenceUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() = chatRepository.untrackPresence()
}

class GetOnlineUsersUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<Set<String>> = chatRepository.getOnlineUsers()
}
