package com.safelive.app.presentation.official

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.domain.model.Ticket
import com.safelive.app.domain.repository.OfficialRepository
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

data class IncidentQueueUiState(
    val incidents: List<Ticket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class IncidentQueueViewModel @Inject constructor(
    private val officialRepository: OfficialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentQueueUiState())
    val uiState: StateFlow<IncidentQueueUiState> = _uiState.asStateFlow()

    init {
        loadQueue()
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                loadQueue()
            }
        }
    }

    fun loadQueue() {
        viewModelScope.launch {
            officialRepository.getIncidentQueue().collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update { it.copy(incidents = result.data, isLoading = false) }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    is Resource.OtpRequired -> Unit
                    Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun acceptIncident(id: String) {
        viewModelScope.launch {
            officialRepository.acceptIncident(id)
            loadQueue()
        }
    }

    fun rejectIncident(id: String, reason: String) {
        viewModelScope.launch {
            officialRepository.rejectIncident(id, reason)
            loadQueue()
        }
    }
}
