package com.aistudio.sepatify.domain.usecase.chat

import com.aistudio.sepatify.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetRecentConversationsUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<String>> = chatRepository.getRecentConversations()
}
