package com.safelive.app.presentation.official

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.data.websocket.WebSocketManager
import com.safelive.app.data.websocket.SocketEvent
import com.safelive.app.domain.model.Ticket
import com.safelive.app.domain.model.TicketStats
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.domain.repository.OfficialRepository
import com.safelive.app.domain.usecase.auth.LogoutUseCase
import com.safelive.app.domain.usecase.notification.GetUnreadCountUseCase
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

data class OfficialDashboardUiState(
    val stats: TicketStats? = null,
    val recentTickets: List<Ticket> = emptyList(),
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
    private val officialRepository: OfficialRepository,
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
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                loadDashboard(refreshing = true)
            }
        }
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
            when (val result = officialRepository.getIncidentQueue().first { it !is Resource.Loading }) {
                is Resource.Success -> {
                    val tickets = result.data
                    val stats = TicketStats(
                        totalTickets = tickets.size,
                        openTickets = tickets.count { it.status.equals("open", true) },
                        pendingTickets = tickets.count { it.status.equals("pending", true) },
                        inProgress = tickets.count { it.status.equals("in_progress", true) },
                        resolvedToday = tickets.count { it.status.equals("resolved", true) || it.status.equals("verified", true) },
                        avgResponseTime = "N/A",
                        resolutionRate = if (tickets.isEmpty()) 0.0 else tickets.count { it.status.equals("resolved", true) || it.status.equals("verified", true) }.toDouble() / tickets.size
                    )
                    _uiState.update { it.copy(stats = stats, recentTickets = tickets.sortedByDescending { ticket -> ticket.createdAt }.take(5), isLoading = false, error = null) }
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                is Resource.OtpRequired -> Unit
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
                    is SocketEvent.IncidentUpdated,
                    is SocketEvent.IncidentResolved,
                    is SocketEvent.StatusChange,
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
