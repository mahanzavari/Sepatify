package com.aistudio.sepatify.domain.usecase.chat

import androidx.paging.PagingData
import com.aistudio.sepatify.domain.model.ChatMessage
import com.aistudio.sepatify.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(otherUser: String): Flow<List<ChatMessage>> =
        chatRepository.getMessages(otherUser)
}

class GetMessagesPagedUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(otherUser: String): Flow<PagingData<ChatMessage>> =
        chatRepository.getMessagesPaged(otherUser)
}
