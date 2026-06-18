package com.safelive.app.presentation.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.usecase.incident.GetNearbyIncidentsUseCase
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val incidents: List<Incident> = emptyList(),
    val selectedIncident: Incident? = null,
    val currentLocation: LatLng? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getNearbyIncidentsUseCase: GetNearbyIncidentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadIncidentsNearLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getNearbyIncidentsUseCase(latitude, longitude)) {
                is Resource.Success -> _uiState.update {
                    it.copy(incidents = result.data, isLoading = false,
                        currentLocation = LatLng(latitude, longitude))
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                Resource.Loading -> Unit
            }
        }
    }

    fun selectIncident(incident: Incident?) = _uiState.update { it.copy(selectedIncident = incident) }
    fun setCurrentLocation(latLng: LatLng) = _uiState.update { it.copy(currentLocation = latLng) }
}
