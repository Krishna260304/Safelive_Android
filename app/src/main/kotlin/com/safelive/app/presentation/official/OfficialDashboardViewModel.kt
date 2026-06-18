package com.safelive.app.presentation.official

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.data.websocket.WebSocketManager
import com.safelive.app.data.websocket.SocketEvent
import com.safelive.app.domain.model.DashboardStats
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.domain.usecase.auth.LogoutUseCase
import com.safelive.app.domain.usecase.incident.GetDashboardStatsUseCase
import com.safelive.app.domain.usecase.notification.GetUnreadCountUseCase
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OfficialDashboardUiState(
    val stats: DashboardStats? = null,
    val officialName: String = "",
    val officialRole: String = "",
    val userType: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
    val loggedOut: Boolean = false
) {
    val displayRole: String
        get() = if (userType.equals("official", ignoreCase = true) && officialRole.isNotBlank()) {
            officialRole.split("_", " ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else {
            userType.replaceFirstChar { it.uppercase() }
        }
}

@HiltViewModel
class OfficialDashboardViewModel @Inject constructor(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authRepository: AuthRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfficialDashboardUiState())
    val uiState: StateFlow<OfficialDashboardUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadDashboard()
        observeUserName()
        observeNotifications()
        webSocketManager.connect()
        observeWebSocket()
    }

    private fun loadDashboard(refreshing: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refreshing,
                    loggedOut = it.loggedOut
                )
            }
            when (val result = getDashboardStatsUseCase()) {
                is Resource.Success -> _uiState.update { it.copy(stats = result.data, isLoading = false) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                Resource.Loading -> Unit
            }
        }
    }

    private fun observeUserName() {
        viewModelScope.launch {
            combine(
                authRepository.getUserName(),
                authRepository.getOfficialRole(),
                authRepository.getUserType()
            ) { name, role, type ->
                Triple(name ?: "Official", role ?: "", type ?: "")
            }.collect { (name, role, type) ->
                _uiState.update {
                    it.copy(
                        officialName = name,
                        officialRole = role,
                        userType = type
                    )
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            getUnreadCountUseCase().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.socketEvents.collect { event ->
                when (event) {
                    is SocketEvent.IncidentCreated,
                    is SocketEvent.OfficialAssignment -> loadDashboard()
                    else -> Unit
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            webSocketManager.disconnect()
            logoutUseCase()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }
}
