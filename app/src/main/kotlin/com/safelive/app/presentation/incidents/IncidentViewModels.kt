package com.safelive.app.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.domain.model.LogbookEntry
import com.safelive.app.data.websocket.WebSocketManager
import com.safelive.app.data.websocket.SocketEvent
import com.safelive.app.domain.model.DraftIncident
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.usecase.incident.*
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.safelive.app.utils.LocationUtils

data class IncidentListUiState(
    val incidents: List<Incident> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedStatus: String? = null,
    val selectedCategory: String? = null,
    val selectedPriority: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    private val getIncidentsUseCase: GetIncidentsUseCase,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentListUiState())
    val uiState: StateFlow<IncidentListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadIncidents()
        observeWebSocketEvents()
    }

    fun loadIncidents(page: Int = 1) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getIncidentsUseCase(
                page = page,
                status = _uiState.value.selectedStatus,
                category = _uiState.value.selectedCategory,
                priority = _uiState.value.selectedPriority,
                search = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            ).collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update { it.copy(incidents = result.data, isLoading = false, isRefreshing = false) }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false, isRefreshing = false) }
                    Resource.Loading -> Unit
                }
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            loadIncidents()
        }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadIncidents()
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadIncidents()
    }

    fun setPriorityFilter(priority: String?) {
        _uiState.update { it.copy(selectedPriority = priority) }
        loadIncidents()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadIncidents()
    }

    fun clearFilters() {
        _uiState.update { it.copy(selectedStatus = null, selectedCategory = null, selectedPriority = null, searchQuery = "") }
        loadIncidents()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketManager.socketEvents.collect { event ->
                when (event) {
                    is SocketEvent.IncidentCreated,
                    is SocketEvent.IncidentUpdated,
                    is SocketEvent.StatusChange -> loadIncidents()
                    else -> Unit
                }
            }
        }
    }
}

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val logbook: List<LogbookEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingLogbook: Boolean = false,
    val error: String? = null,
    val officialRole: String = ""
)

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    private val getIncidentDetailUseCase: GetIncidentDetailUseCase,
    private val getIncidentLogbookUseCase: GetIncidentLogbookUseCase,
    private val updateIncidentStatusUseCase: UpdateIncidentStatusUseCase,
    private val webSocketManager: WebSocketManager,
    private val authRepository: com.safelive.app.domain.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState: StateFlow<IncidentDetailUiState> = _uiState.asStateFlow()

    private var currentIncidentId: String = ""
    private var loadJob: Job? = null
    private var webSocketJob: Job? = null

    init {
        observeWebSocketForIncident()
        viewModelScope.launch {
            authRepository.getOfficialRole().collect { role ->
                _uiState.update { it.copy(officialRole = role ?: "") }
            }
        }
    }

    fun loadIncident(id: String) {
        currentIncidentId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getIncidentDetailUseCase(id)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(incident = result.data, isLoading = false, error = null) }
                    loadLogbook(id)
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                Resource.Loading -> Unit
            }
        }
    }

    fun refreshLogbook() {
        if (currentIncidentId.isNotBlank()) loadLogbook(currentIncidentId)
    }

    private fun loadLogbook(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLogbook = true) }
            when (val result = getIncidentLogbookUseCase(id)) {
                is Resource.Success -> _uiState.update { it.copy(logbook = result.data, isLoadingLogbook = false) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoadingLogbook = false) }
                Resource.Loading -> Unit
            }
        }
    }

    private fun observeWebSocketForIncident() {
        if (webSocketJob?.isActive == true) return
        webSocketJob = viewModelScope.launch {
            webSocketManager.socketEvents.collect { event ->
                when {
                    event is SocketEvent.IncidentUpdated && event.data["id"] == currentIncidentId -> loadIncident(currentIncidentId)
                    event is SocketEvent.StatusChange && event.incidentId == currentIncidentId -> loadIncident(currentIncidentId)
                    else -> Unit
                }
            }
        }
    }
}

data class CreateIncidentUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "Medium",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location: String = "",
    val pincode: String = "",
    val imagePaths: List<String> = emptyList(),
    val isGpsLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val createdIncidentId: String? = null
)

@HiltViewModel
class CreateIncidentViewModel @Inject constructor(
    private val createIncidentUseCase: CreateIncidentUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val locationUtils: LocationUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateIncidentUiState())
    val uiState: StateFlow<CreateIncidentUiState> = _uiState.asStateFlow()

    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title, error = null) }
    fun onDescriptionChange(desc: String) = _uiState.update { it.copy(description = desc, error = null) }
    fun onCategoryChange(cat: String) = _uiState.update { it.copy(category = cat) }
    fun onPriorityChange(priority: String) = _uiState.update { it.copy(priority = priority) }
    fun onLocationChange(location: String) = _uiState.update { it.copy(location = location) }
    fun onPincodeChange(pincode: String) = _uiState.update { it.copy(pincode = com.safelive.app.utils.ValidationUtils.digitsOnly(pincode, 6)) }

    fun setLocation(latitude: Double, longitude: Double, location: String) {
        _uiState.update { it.copy(latitude = latitude, longitude = longitude, location = location, isGpsLoading = false) }
    }

    fun setGpsLoading(loading: Boolean) = _uiState.update { it.copy(isGpsLoading = loading) }

    fun addImage(path: String) {
        val current = _uiState.value.imagePaths.toMutableList()
        if (current.size < 5) {
            current.add(path)
            _uiState.update { it.copy(imagePaths = current) }
        }
    }

    fun removeImage(path: String) {
        _uiState.update { it.copy(imagePaths = it.imagePaths.filter { p -> p != path }) }
    }

    fun fetchLiveLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGpsLoading = true) }
            val coords = locationUtils.getCurrentLocation()
            if (coords != null) {
                val address = locationUtils.getAddressFromCoordinates(coords.first, coords.second)
                setLocation(coords.first, coords.second, address)
            } else {
                _uiState.update { it.copy(isGpsLoading = false, error = "Failed to get live location") }
            }
        }
    }

    fun createIncident() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.pincode.isNotBlank() && !com.safelive.app.utils.ValidationUtils.isValidPincode(state.pincode)) {
                _uiState.update { it.copy(isLoading = false, error = "Pincode must be a 6-digit number") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = createIncidentUseCase(
                title = state.title,
                description = state.description,
                category = state.category,
                priority = state.priority,
                latitude = state.latitude,
                longitude = state.longitude,
                location = state.location.takeIf { it.isNotBlank() },
                pincode = state.pincode.takeIf { it.isNotBlank() },
                imagePaths = state.imagePaths
            )
            when (result) {
                is Resource.Success<*> -> {
                    val incident = result.data as? Incident
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, createdIncidentId = incident?.id) }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            val state = _uiState.value
            saveDraftUseCase(
                DraftIncident(
                    title = state.title,
                    description = state.description,
                    category = state.category,
                    priority = state.priority,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    address = state.location.takeIf { it.isNotBlank() },
                    pincode = state.pincode.takeIf { it.isNotBlank() },
                    localImagePaths = state.imagePaths
                )
            )
        }
    }
}
