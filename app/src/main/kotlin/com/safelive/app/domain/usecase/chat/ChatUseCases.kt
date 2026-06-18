package com.safelive.app.domain.usecase.chat

import com.safelive.app.domain.repository.ChatRepository
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(private val repo: ChatRepository) {
    operator fun invoke(chatId: String) = repo.getMessages(chatId)
}

class SendMessageUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: String, content: String) =
        repo.sendMessage(chatId, content)
}
