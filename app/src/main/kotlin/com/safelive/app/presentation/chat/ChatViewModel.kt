package com.safelive.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.data.websocket.WebSocketManager
import com.safelive.app.data.websocket.SocketEvent
import com.safelive.app.domain.model.Message
import com.safelive.app.domain.usecase.chat.GetMessagesUseCase
import com.safelive.app.domain.usecase.chat.SendMessageUseCase
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val currentUserId: String = "",
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTyping: Boolean = false,
    val otherUserTyping: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val authRepository: AuthRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentChatId: String = ""
    private var messagesJob: Job? = null
    private var webSocketJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.getUserId().firstOrNull()?.let { id ->
                _uiState.update { it.copy(currentUserId = id) }
            }
        }
        observeWebSocket()
    }

    fun loadChat(chatId: String) {
        currentChatId = chatId
        _uiState.update { it.copy(isLoading = true, error = null, otherUserTyping = false) }
        loadMessages(chatId)
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }

        if (currentChatId.isNotBlank()) {
            webSocketManager.sendMessage("typing", mapOf("chat_id" to currentChatId, "is_typing" to true))
        }
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "") }
            when (val result = sendMessageUseCase(currentChatId, content)) {
                is Resource.Success -> {
                    val updatedMessages = _uiState.value.messages + result.data
                    _uiState.update { it.copy(messages = updatedMessages) }
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    private fun loadMessages(chatId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getMessagesUseCase(chatId).collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update { it.copy(messages = result.data, isLoading = false) }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    is Resource.OtpRequired -> Unit
                    Resource.Loading -> Unit
                }
            }
        }
    }

    private fun refreshMessages() {
        if (currentChatId.isNotBlank()) {
            loadMessages(currentChatId)
        }
    }

    private fun observeWebSocket() {
        if (webSocketJob?.isActive == true) return
        webSocketJob = viewModelScope.launch {
            webSocketManager.socketEvents.collect { event ->
                when (event) {
                    is SocketEvent.TicketMessage -> {
                        if (event.chatId == currentChatId) {
                            refreshMessages()
                        }
                    }
                    is SocketEvent.TypingIndicator -> {
                        if (event.chatId == currentChatId && event.userId != _uiState.value.currentUserId) {
                            _uiState.update { it.copy(otherUserTyping = event.isTyping) }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}
