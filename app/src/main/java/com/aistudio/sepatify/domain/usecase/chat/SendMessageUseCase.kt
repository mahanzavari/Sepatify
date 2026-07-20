package com.aistudio.sepatify.domain.usecase.chat

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.ChatRepository

class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(otherUser: String, text: String, songShare: Song? = null) =
        chatRepository.sendMessage(otherUser, text, songShare)
}
