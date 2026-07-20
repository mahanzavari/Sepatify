package com.aistudio.sepatify.domain.usecase.chat

import com.aistudio.sepatify.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class SearchUsersUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(query: String): Flow<List<String>> = chatRepository.searchUsers(query)
}
