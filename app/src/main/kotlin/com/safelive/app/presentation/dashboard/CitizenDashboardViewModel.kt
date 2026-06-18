package com.safelive.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.data.websocket.WebSocketManager
import com.safelive.app.data.websocket.SocketEvent
import com.safelive.app.domain.model.DashboardStats
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.usecase.auth.LogoutUseCase
import com.safelive.app.domain.usecase.incident.GetDashboardStatsUseCase
import com.safelive.app.domain.usecase.notification.GetUnreadCountUseCase
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val userName: String = "",
    val userType: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val unreadNotifications: Int = 0,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class CitizenDashboardViewModel @Inject constructor(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authRepository: AuthRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    init {
        loadDashboard()
        observeUserInfo()
        observeNotifications()
        connectWebSocket()
        observeWebSocketEvents()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getDashboardStatsUseCase()) {
                is Resource.Success -> _uiState.update { it.copy(stats = result.data, isLoading = false) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                Resource.Loading -> Unit
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = getDashboardStatsUseCase()) {
                is Resource.Success -> _uiState.update { it.copy(stats = result.data, isRefreshing = false) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isRefreshing = false) }
                Resource.Loading -> Unit
            }
        }
    }

    private fun observeUserInfo() {
        viewModelScope.launch {
            combine(
                authRepository.getUserName(),
                authRepository.getUserType()
            ) { name, type ->
                Pair(name ?: "", type ?: "citizen")
            }.collect { (name, type) ->
                _uiState.update { it.copy(userName = name, userType = type) }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            getUnreadCountUseCase().collect { count ->
                _uiState.update { it.copy(unreadNotifications = count) }
            }
        }
    }

    private fun connectWebSocket() {
        webSocketManager.connect()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketManager.socketEvents.collect { event ->
                when (event) {
                    is SocketEvent.IncidentCreated,
                    is SocketEvent.IncidentUpdated,
                    is SocketEvent.IncidentResolved,
                    is SocketEvent.StatusChange -> {

                        refresh()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            webSocketManager.disconnect()
            logoutUseCase()
            _loggedOut.value = true
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
