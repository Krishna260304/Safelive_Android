package com.safelive.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.domain.model.Notification
import com.safelive.app.domain.usecase.notification.*
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val markReadUseCase: MarkNotificationReadUseCase,
    private val markAllReadUseCase: MarkAllNotificationsReadUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        observeUnreadCount()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            getNotificationsUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update { it.copy(notifications = result.data, isLoading = false) }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    is Resource.OtpRequired -> Unit
                    Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            getUnreadCountUseCase().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { markReadUseCase(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { markAllReadUseCase() }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch { deleteNotificationUseCase(id) }
    }
}
