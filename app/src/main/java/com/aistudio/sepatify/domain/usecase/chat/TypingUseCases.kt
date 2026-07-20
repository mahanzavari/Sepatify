package com.aistudio.sepatify.domain.usecase.chat

import com.aistudio.sepatify.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetTypingStateUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(otherUser: String): Flow<Boolean> =
        chatRepository.getTypingState(otherUser)
}

class SetTypingUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(otherUser: String, isTyping: Boolean) =
        chatRepository.setTyping(otherUser, isTyping)
}
