package com.safelive.app.presentation.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.data.remote.api.IncidentApi
import com.safelive.app.data.remote.api.TicketApi
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.model.Ticket
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.domain.usecase.incident.GetIncidentsUseCase
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapLocation(val latitude: Double, val longitude: Double)

data class MapUiState(
    val incidents: List<Incident> = emptyList(),
    val tickets: List<Ticket> = emptyList(),
    val heatmap: List<HeatmapPoint> = emptyList(),
    val selectedIncident: Incident? = null,
    val selectedTicket: Ticket? = null,
    val currentLocation: MapLocation? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HeatmapPoint(val latitude: Double, val longitude: Double, val weight: Double)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getIncidentsUseCase: GetIncidentsUseCase,
    private val authRepository: AuthRepository,
    private val ticketApi: TicketApi,
    private val incidentApi: IncidentApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadIncidents()
    }

    fun loadIncidents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var incidentResult: Resource<List<Incident>>? = null
            getIncidentsUseCase(page = 1).collect { result ->
                if (result !is Resource.Loading) incidentResult = result
            }

            when (val result = incidentResult) {
                is Resource.Success -> {
                    val isOfficial = authRepository.getOfficialRole().firstOrNull()?.isNotBlank() == true ||
                        authRepository.getUserType().firstOrNull()?.equals("official", true) == true
                    var tickets = emptyList<Ticket>()
                    var heatmap = emptyList<HeatmapPoint>()
                    if (isOfficial) {
                        runCatching {
                            val ticketResponse = ticketApi.getTickets()
                            if (ticketResponse.success) {
                                tickets = ticketResponse.data.orEmpty().map { it.toDomain() }
                            }
                        }
                        runCatching {
                            val heatmapResponse = incidentApi.getHeatmap()
                            if (heatmapResponse.success) {
                                heatmap = heatmapResponse.data.orEmpty().map {
                                    HeatmapPoint(it.lat, it.lng, it.weight ?: 1.0)
                                }
                            }
                        }
                    }
                    _uiState.update {
                        it.copy(incidents = result.data, tickets = tickets, heatmap = heatmap, isLoading = false, error = null)
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                null, Resource.Loading -> _uiState.update { it.copy(isLoading = false) }
                is Resource.OtpRequired -> Unit
            }
        }
    }

    /** Kept as a compatibility entry point for the refresh action. */
    fun loadIncidentsNearLocation(latitude: Double, longitude: Double) = loadIncidents()

    fun selectIncident(incident: Incident?) = _uiState.update { it.copy(selectedIncident = incident, selectedTicket = null) }
    fun selectTicket(ticket: Ticket?) = _uiState.update { it.copy(selectedTicket = ticket, selectedIncident = null) }
    fun setCurrentLocation(latitude: Double, longitude: Double) =
        _uiState.update { it.copy(currentLocation = MapLocation(latitude, longitude)) }
}
