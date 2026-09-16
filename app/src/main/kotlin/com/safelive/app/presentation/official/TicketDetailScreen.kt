package com.safelive.app.presentation.official

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.safelive.app.navigation.Screen
import com.safelive.app.data.remote.api.ProfileApi
import com.safelive.app.data.remote.dto.UserDto
import com.safelive.app.domain.model.LogbookEntry
import com.safelive.app.domain.model.Ticket
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.domain.repository.OfficialRepository
import com.safelive.app.ui.theme.DangerRed
import com.safelive.app.ui.theme.SuccessGreen
import com.safelive.app.utils.Constants
import com.safelive.app.utils.ImageUrlUtils
import com.safelive.app.utils.DateUtils
import com.safelive.app.utils.Resource
import com.safelive.app.utils.capitalizeWords
import com.safelive.app.utils.orNA
import com.safelive.app.utils.orZero
import com.safelive.app.utils.toStatusColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Headers
import javax.inject.Inject
import com.safelive.app.data.local.datastore.UserPreferencesDataStore

data class TicketDetailUiState(
    val ticket: Ticket? = null,
    val logbook: List<LogbookEntry> = emptyList(),
    val workers: List<UserDto> = emptyList(),
    val supervisors: List<UserDto> = emptyList(),
    val currentUserId: String = "",
    val selectedWorkerId: String = "",
    val selectedSupervisorId: String = "",
    val progressUpdate: String = "",
    val actionNote: String = "",
    val supervisorNote: String = "",
    val isLoading: Boolean = false,
    val isLoadingLogbook: Boolean = false,
    val isLoadingPeople: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val officialRole: String = "",
    val accessToken: String = ""
)

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    private val officialRepository: OfficialRepository,
    private val authRepository: AuthRepository,
    private val profileApi: ProfileApi,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    private var currentTicketId: String = ""

    init {
        viewModelScope.launch {
            authRepository.getOfficialRole().collectLatest { role ->
                val normalizedRole = role.orEmpty()
                _uiState.update { it.copy(officialRole = normalizedRole) }
                loadPeople(normalizedRole)
            }
        }
        viewModelScope.launch {
            authRepository.getUserId().collectLatest { userId ->
                _uiState.update { it.copy(currentUserId = userId.orEmpty()) }
            }
        }
        viewModelScope.launch {
            userPreferencesDataStore.accessToken.collectLatest { token ->
                _uiState.update { it.copy(accessToken = token.orEmpty()) }
            }
        }
    }

    fun loadTicket(ticketId: String) {
        currentTicketId = ticketId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            when (val result = officialRepository.getTicketById(ticketId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            ticket = result.data,
                            isLoading = false,
                            error = null,
                            selectedWorkerId = result.data.workerId.orEmpty(),
                            selectedSupervisorId = result.data.reopenedSupervisorId.orEmpty()
                        )
                    }
                    loadLogbook(ticketId)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }

                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    fun setSelectedWorker(id: String) = _uiState.update { it.copy(selectedWorkerId = id, error = null) }
    fun setSelectedSupervisor(id: String) = _uiState.update { it.copy(selectedSupervisorId = id, error = null) }
    fun setProgressUpdate(value: String) = _uiState.update { it.copy(progressUpdate = value, error = null) }
    fun setActionNote(value: String) = _uiState.update { it.copy(actionNote = value, error = null) }
    fun setSupervisorNote(value: String) = _uiState.update { it.copy(supervisorNote = value, error = null) }

    fun refresh() {
        if (currentTicketId.isNotBlank()) loadTicket(currentTicketId)
    }

    fun verifyTicket() = submitStatusUpdate("verified", noteOverride = "Case verified")
    fun resolveTicket() = submitStatusUpdate("resolved", noteOverride = "Case resolved")
    fun reopenTicket() = submitStatusUpdate("open", clearSelections = true, noteOverride = "Case reopened")

    fun assignWorker() {
        val state = _uiState.value
        val worker = state.workers.firstOrNull { it.id == state.selectedWorkerId }
        if (worker == null) {
            _uiState.update { it.copy(error = "Select a worker from the dropdown") }
            return
        }

        submitTicketAction {
            officialRepository.assignWorkers(
                ticketId = currentTicketId,
                workerId = worker.id,
                assigneeName = worker.fullName.orEmpty(),
                assigneePhone = worker.phone,
                assigneePhoto = worker.profilePictureUrl,
                notes = state.actionNote.ifBlank { null }
            )
        }
    }

    fun assignSupervisor() {
        val state = _uiState.value
        val supervisor = state.supervisors.firstOrNull { it.id == state.selectedSupervisorId }
        if (supervisor == null) {
            _uiState.update { it.copy(error = "Select a supervisor from the dropdown") }
            return
        }

        submitTicketAction {
            officialRepository.assignSupervisor(
                ticketId = currentTicketId,
                supervisorId = supervisor.id,
                notes = state.supervisorNote.ifBlank { state.actionNote.ifBlank { null } }
            )
        }
    }

    fun updateWork() {
        val updateText = _uiState.value.progressUpdate.trim()
        if (updateText.isBlank()) {
            _uiState.update { it.copy(error = "Enter a work update before submitting") }
            return
        }

        submitTicketAction {
            officialRepository.updateProgress(
                ticketId = currentTicketId,
                updateText = updateText,
                editLastUpdate = false
            )
        }
    }

    private fun submitStatusUpdate(
        status: String,
        clearSelections: Boolean = false,
        noteOverride: String? = null
    ) {
        submitTicketAction {
            officialRepository.updateStatus(
                id = currentTicketId,
                status = status,
                note = noteOverride ?: _uiState.value.actionNote.ifBlank { null }
            )
        }

        if (clearSelections) {
            _uiState.update {
                it.copy(
                    selectedWorkerId = "",
                    selectedSupervisorId = "",
                    progressUpdate = "",
                    actionNote = "",
                    supervisorNote = ""
                )
            }
        }
    }

    private fun submitTicketAction(action: suspend () -> Resource<Ticket>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, message = null) }
            when (val result = action()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            ticket = result.data,
                            isSubmitting = false,
                            message = "Action completed successfully",
                            actionNote = "",
                            supervisorNote = "",
                            progressUpdate = if (result.data.status.equals("resolved", ignoreCase = true)) "" else it.progressUpdate
                        )
                    }
                    loadLogbook(currentTicketId)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.message)
                }

                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    private fun loadPeople(officialRole: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPeople = true) }
            try {
                val normalizedRole = officialRole.normalizedOfficialRole()
                val loadWorkers = normalizedRole == "department" || normalizedRole == "supervisor"
                val loadSupervisors = normalizedRole == "department"
                val workersResponse = if (loadWorkers) profileApi.getWorkers() else null
                val supervisorsResponse = if (loadSupervisors) profileApi.getManagedOfficials() else null
                if ((workersResponse != null && !workersResponse.success) || (supervisorsResponse != null && !supervisorsResponse.success)) {
                    val message = workersResponse?.error ?: supervisorsResponse?.error ?: "Unable to load available users"
                    _uiState.update {
                        it.copy(
                            workers = emptyList(),
                            supervisors = emptyList(),
                            isLoadingPeople = false,
                            error = message
                        )
                    }
                    return@launch
                }
                val workers = workersResponse?.data.orEmpty()
                val supervisors = if (loadSupervisors) {
                    supervisorsResponse?.data.orEmpty().filter { it.officialRole.equals("supervisor", ignoreCase = true) }
                } else {
                    emptyList()
                }
                _uiState.update {
                    it.copy(
                        workers = workers,
                        supervisors = supervisors,
                        isLoadingPeople = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingPeople = false, error = "Unable to load available users") }
            }
        }
    }

    private fun loadLogbook(ticketId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLogbook = true) }
            when (val result = officialRepository.getTicketLogbook(ticketId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoadingLogbook = false, logbook = result.data)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoadingLogbook = false, error = result.message)
                }

                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: String,
    navController: NavController,
    viewModel: TicketDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ticketId) {
        viewModel.loadTicket(ticketId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.ticket?.ticketId ?: "Ticket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.ticket?.title.orNA(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.ticket == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Could not load ticket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.error.orNA(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTicket(ticketId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.ticket != null -> {
                TicketDetailContent(
                    uiState = uiState,
                    modifier = Modifier.padding(padding),
                    navController = navController,
                    onVerify = viewModel::verifyTicket,
                    onAssignWorker = viewModel::assignWorker,
                    onUpdateWork = viewModel::updateWork,
                    onResolve = viewModel::resolveTicket,
                    onReopen = viewModel::reopenTicket,
                    onAssignSupervisor = viewModel::assignSupervisor,
                    onWorkerSelected = viewModel::setSelectedWorker,
                    onSupervisorSelected = viewModel::setSelectedSupervisor,
                    onProgressChange = viewModel::setProgressUpdate,
                    onActionNoteChange = viewModel::setActionNote,
                    onSupervisorNoteChange = viewModel::setSupervisorNote
                )
            }
        }
    }
}

@Composable
private fun TicketDetailContent(
    uiState: TicketDetailUiState,
    modifier: Modifier = Modifier,
    navController: NavController,
    onVerify: () -> Unit,
    onAssignWorker: () -> Unit,
    onUpdateWork: () -> Unit,
    onResolve: () -> Unit,
    onReopen: () -> Unit,
    onAssignSupervisor: () -> Unit,
    onWorkerSelected: (String) -> Unit,
    onSupervisorSelected: (String) -> Unit,
    onProgressChange: (String) -> Unit,
    onActionNoteChange: (String) -> Unit,
    onSupervisorNoteChange: (String) -> Unit
) {
    val ticket = uiState.ticket ?: return
    val context = LocalContext.current
    var showLogbook by remember { mutableStateOf(false) }
    val attachmentImages = remember(ticket.imageUrl, ticket.imageUrls, ticket.images) {
        buildList {
            ticket.imageUrls.orEmpty().forEach { url ->
                ImageUrlUtils.normalize(url)?.let { add(it) }
            }
            ImageUrlUtils.normalize(ticket.imageUrl)?.let { add(it) }
            ticket.images.orEmpty().forEach { url ->
                ImageUrlUtils.normalize(url)?.let { add(it) }
            }
        }.distinct()
    }
    val currentRole = uiState.officialRole.normalizedOfficialRole()
    val isDepartment = currentRole == "department"
    val isSupervisor = currentRole == "supervisor"
    val isInspector = currentRole == "field_inspector"
    val isWorker = currentRole == "worker"
    val status = ticket.status.lowercase()
    val isResolved = status == "resolved" || status == "closed"
    val isVerified = status == "verified"
    val isReopened = ticket.isReopenedCase()
    val hasAssignment = ticket.hasAssignedWorkers()
    val reopenedSupervisorId = ticket.reopenedSupervisorId.orEmpty().trim()
    val hasReopenedSupervisor = reopenedSupervisorId.isNotBlank()
    val supervisorAssignedToCurrentUser =
        isDepartment || !isReopened || !hasReopenedSupervisor || reopenedSupervisorId == uiState.currentUserId
    val canRoleRunWorkflow =
        (isDepartment || isSupervisor) &&
            supervisorAssignedToCurrentUser &&
            !(!hasReopenedSupervisor && isReopened)
    val canVerifyStep = !isResolved && !isVerified
    val canAssignWorkersStep = !isResolved && isVerified && !hasAssignment
    val canResolveStep = !isResolved && isVerified && (hasAssignment || isReopened)
    val canVerify = canRoleRunWorkflow && canVerifyStep
    val canAssignWorker = canRoleRunWorkflow && canAssignWorkersStep
    val canUpdateProgress = isInspector && !isResolved && status in setOf("open", "pending", "verified", "in_progress")
    val canResolve = canRoleRunWorkflow && canResolveStep
    val canReopen = isDepartment && isResolved
    val canAssignSupervisorAfterReopen = isDepartment && isReopened && !hasReopenedSupervisor

    if (showLogbook) {
        TicketLogbookDialog(
            title = ticket.title,
            location = ticket.location,
            entries = uiState.logbook,
            onDismiss = { showLogbook = false },
            onDownload = {
                val report = buildLogbookShareText(ticket.title, ticket.location, uiState.logbook)
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Ticket LogBook - ${ticket.title}")
                            putExtra(Intent.EXTRA_TEXT, report)
                        },
                        "Share ticket logbook"
                    )
                )
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(ticket.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            StatusPill(ticket.status)
                        }
                        Text("${ticket.category.capitalizeWords()} | ${(ticket.priority ?: "Medium").uppercase()} priority", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ticket.location.orNA(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Assigned Workers: ${ticket.assignedDisplayText()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Progress: ${ticket.progressPercent.orZero()}% | Updated: ${ticket.progressUpdatedDisplay()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ticket ID: ${ticket.ticketId.orNA()}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = {
                                navController.navigate(
                                    Screen.Chat.createRoute(ticket.incidentId ?: ticket.id)
                                )
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showLogbook = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("LogBook")
                    }
                }
            }
        }

        if (uiState.message != null) {
            Surface(color = SuccessGreen.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                Text(
                    uiState.message!!,
                    modifier = Modifier.padding(12.dp),
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (attachmentImages.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Incident Images",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(attachmentImages) { imageUrl ->
                            TicketImageThumbnail(
                                imageUrl = imageUrl,
                                accessToken = uiState.accessToken
                            )
                        }
                    }
                }
            }
        }

        if (uiState.error != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                Text(
                    uiState.error!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (isReopened && !hasReopenedSupervisor) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                Text(
                    "Assign a supervisor first for this reopened ticket before workflow actions can continue.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (canVerify) {
            ActionButtonRow(
                label = "Verify case",
                actionText = "Verify",
                icon = Icons.Default.Done,
                buttonColor = MaterialTheme.colorScheme.primary,
                loading = uiState.isSubmitting,
                onAction = onVerify
            )
        }

        if (canAssignWorker) {
            AssignmentSection(
                title = "Assign Workers",
                selectedUserId = uiState.selectedWorkerId,
                users = uiState.workers,
                isLoadingPeople = uiState.isLoadingPeople,
                placeholder = "Select worker",
                buttonLabel = "Assign Workers",
                buttonIcon = Icons.Default.Person,
                onUserSelected = onWorkerSelected,
                onButtonClick = onAssignWorker,
                dropdownLabel = { user ->
                    "${user.fullName.orNA()}${user.workerSpecialization?.let { " ($it)" }.orEmpty()}"
                }
            )
        }

        if (canUpdateProgress) {
            FieldUpdateSection(
                isInspector = isInspector,
                progressUpdate = uiState.progressUpdate,
                lastInspectorUpdate = ticket.lastInspectorUpdateAt,
                loading = uiState.isSubmitting,
                onProgressChange = onProgressChange,
                onSubmit = onUpdateWork
            )
        }

        if (canResolve) {
            ActionButtonRow(
                label = "Resolve case",
                actionText = "Resolved",
                icon = Icons.Default.CheckCircle,
                buttonColor = SuccessGreen,
                loading = uiState.isSubmitting,
                onAction = onResolve
            )
        }

        if (canReopen) {
            ActionButtonRow(
                label = "Reopen case",
                actionText = "Reopen",
                icon = Icons.Default.Refresh,
                buttonColor = DangerRed,
                loading = uiState.isSubmitting,
                onAction = onReopen
            )
        }

        if (canAssignSupervisorAfterReopen) {
            AssignmentSection(
                title = "Assign Supervisor (Reopened Case)",
                selectedUserId = uiState.selectedSupervisorId,
                users = uiState.supervisors,
                isLoadingPeople = uiState.isLoadingPeople,
                placeholder = "Select supervisor",
                buttonLabel = "Assign Supervisor",
                buttonIcon = Icons.Default.Person,
                onUserSelected = onSupervisorSelected,
                onButtonClick = onAssignSupervisor,
                dropdownLabel = { user ->
                    "${user.fullName.orNA()}${user.workerSpecialization?.let { " ($it)" }.orEmpty()}"
                },
                footerText = "Previous resolving supervisor: ${ticket.reopenedSupervisorName.orNA()}",
                footerColor = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun ActionButtonRow(
    label: String,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonColor: Color,
    loading: Boolean,
    onAction: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAction,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(actionText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentSection(
    title: String,
    selectedUserId: String,
    users: List<UserDto>,
    isLoadingPeople: Boolean,
    placeholder: String,
    buttonLabel: String,
    buttonIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onUserSelected: (String) -> Unit,
    onButtonClick: () -> Unit,
    dropdownLabel: (UserDto) -> String,
    footerText: String? = null,
    footerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = users.firstOrNull { it.id == selectedUserId }?.let(dropdownLabel) ?: placeholder

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (isLoadingPeople) {
                        DropdownMenuItem(text = { Text("Loading...") }, onClick = {})
                    } else if (users.isEmpty()) {
                        DropdownMenuItem(text = { Text("No users found") }, onClick = {})
                    } else {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(dropdownLabel(user), fontWeight = FontWeight.Medium)
                                        Text(
                                            user.officialRole.orNA(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onUserSelected(user.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onButtonClick,
                enabled = !isLoadingPeople,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.height(56.dp)
            ) {
                if (isLoadingPeople) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(buttonIcon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Text(buttonLabel, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        footerText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = footerColor)
        }
    }
}

@Composable
private fun FieldUpdateSection(
    isInspector: Boolean,
    progressUpdate: String,
    lastInspectorUpdate: String?,
    loading: Boolean,
    onProgressChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Daily update deadline: 6:00 PM IST | Last inspector update: ${lastInspectorUpdate.orNA().formatMaybeIso()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = progressUpdate,
                onValueChange = onProgressChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text(if (isInspector) "Enter today field inspection update..." else "Enter progress update...")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = onSubmit,
                enabled = !loading,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF154F8E))
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Update", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

private fun buildLogbookShareText(
    title: String,
    location: String,
    entries: List<LogbookEntry>
): String = buildString {
    appendLine("Ticket LogBook")
    appendLine(title)
    appendLine(location)
    appendLine()
    entries.forEach { entry ->
        appendLine("${entry.createdAt.orNA()} | ${entry.action.orNA()}")
        entry.message?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        appendLine()
    }
}

@Composable
private fun TicketLogbookDialog(
    title: String,
    location: String,
    entries: List<LogbookEntry>,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FB))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ticket LogBook", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Official activity LogBook for $title..", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total updates: ${entries.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onDownload, shape = RoundedCornerShape(12.dp)) {
                        Text("Download")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD0D7E2), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF327E8C))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "STATUS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Location", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Details", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Time", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No logbook entries yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
                        ) {
                            itemsIndexed(entries) { index, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = location,
                                        modifier = Modifier.weight(2f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(3f)) {
                                        Text(
                                            text = entry.toLogbookDetails(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = entry.toLogbookMeta(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                    Text(
                                        text = entry.createdAt.formatDatePart(),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = entry.createdAt.formatTimePart(),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (index < entries.size - 1) {
                                    HorizontalDivider(color = Color(0xFFD0D7E2), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogbookEntryCard(entry: LogbookEntry) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        border = BorderStroke(1.dp, Color(0xFFD0D7E2))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = entry.toLogbookTitle(),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = entry.toLogbookDetails(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.createdAt.formatDatePart(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.createdAt.formatTimePart(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val extraInfo = entry.toLogbookMeta()
            if (extraInfo.isNotBlank()) {
                Text(
                    text = extraInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun Ticket.assignedDisplayText(): String {
    val assigneeNames = assignees.orEmpty().mapNotNull { assignee ->
        val name = assignee.name.takeIf { it.isNotBlank() }
        if (name != null && !assignee.workerCode.isNullOrBlank()) {
            "$name (#${assignee.workerCode})"
        } else {
            name
        }
    }
    if (assigneeNames.isNotEmpty()) return assigneeNames.joinToString(", ")

    val fallbackName = assigneeName?.takeIf { it.isNotBlank() }
        ?: assignedTo?.takeIf { it.isNotBlank() }
        ?: fieldInspectorName?.takeIf { it.isNotBlank() }

    return fallbackName ?: "Unassigned"
}

@Composable
private fun TicketImageThumbnail(imageUrl: String, accessToken: String) {
    val context = LocalContext.current
    val request = remember(imageUrl, accessToken) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .apply {
                accessToken.takeIf { it.isNotBlank() }?.let { token ->
                    headers(
                        Headers.Builder()
                            .add("Authorization", "Bearer $token")
                            .build()
                    )
                }
            }
            .crossfade(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = "Incident image",
        modifier = Modifier
            .size(width = 180.dp, height = 120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Image unavailable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

private fun Ticket.progressUpdatedDisplay(): String {
    return DateUtils.formatExact(progressUpdatedAt ?: lastInspectorUpdateAt ?: updatedAt)
}

private fun Ticket.hasAssignedWorkers(): Boolean {
    return assignees.orEmpty().isNotEmpty() ||
        !assignedTo.isNullOrBlank() ||
        !assigneeUserId.isNullOrBlank() ||
        !workerId.isNullOrBlank() ||
        !workerIds.isNullOrEmpty()
}

private fun Ticket.isReopenedCase(): Boolean {
    return status.equals("reopened", ignoreCase = true) ||
        reopenedBy != null ||
        reopenWarning != null ||
        !reopenedSupervisorId.isNullOrBlank()
}

private fun String?.formatMaybeIso(): String {
    if (this.isNullOrBlank()) return "N/A"
    return DateUtils.formatExact(this)
}

private fun String?.formatDatePart(): String {
    if (this.isNullOrBlank()) return "N/A"
    return DateUtils.formatDateOnly(this)
}

private fun String?.formatTimePart(): String {
    if (this.isNullOrBlank()) return "N/A"
    return DateUtils.formatTime(this)
}

private fun String?.normalizedOfficialRole(): String {
    return this?.trim().orEmpty().lowercase().replace("-", "_")
}

private fun LogbookEntry.toLogbookTitle(): String {
    val actionText = action?.takeIf { it.isNotBlank() }?.capitalizeWords()
    val statusText = listOfNotNull(
        statusFrom?.takeIf { it.isNotBlank() }?.let { "From ${it.capitalizeWords()}" },
        statusTo?.takeIf { it.isNotBlank() }?.let { "to ${it.capitalizeWords()}" }
    ).joinToString(" ")
    return when {
        actionText != null && statusText.isNotBlank() -> "$actionText | $statusText"
        actionText != null -> actionText
        statusText.isNotBlank() -> statusText
        else -> "Logbook Update"
    }
}

private fun LogbookEntry.toLogbookMeta(): String {
    val parts = buildList {
        actorName?.takeIf { it.isNotBlank() }?.let {
            add("Performed by $it${actorRole?.takeIf { role -> role.isNotBlank() }?.let { role -> " ($role)" }.orEmpty()}")
        }
        message?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    return parts.joinToString(" • ")
}

private fun LogbookEntry.toLogbookDetails(): String {
    val fromTo = listOfNotNull(
        statusFrom?.takeIf { it.isNotBlank() }?.let { "From status: $it" },
        statusTo?.takeIf { it.isNotBlank() }?.let { "To status: $it" }
    )
    val actionText = action?.takeIf { it.isNotBlank() }?.capitalizeWords()
    val actorText = actorName?.takeIf { it.isNotBlank() }?.let {
        "$it${actorRole?.takeIf { role -> role.isNotBlank() }?.let { role -> " ($role)" }.orEmpty()}"
    }
    return buildString {
        if (fromTo.isNotEmpty()) append(fromTo.joinToString(", "))
        if (actionText != null) {
            if (isNotEmpty()) append(". ")
            append(actionText)
        }
        if (message != null && message.isNotBlank()) {
            if (isNotEmpty()) append(". ")
            append(message)
        }
        if (actorText != null) {
            if (isNotEmpty()) append(". ")
            append("Performed by $actorText")
        }
    }.ifBlank { message.orNA() }
}

@Composable
private fun WorkflowActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    buttonColor: Color,
    loading: Boolean,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = buttonColor)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onClick,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(buttonText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignUserSection(
    label: String,
    helpText: String,
    users: List<UserDto>,
    selectedId: String,
    loading: Boolean,
    onSelected: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    placeholder: String,
    buttonText: String,
    buttonLoading: Boolean,
    onButtonClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = users.firstOrNull { it.id == selectedId }?.fullName?.takeIf { it.isNotBlank() } ?: "Select user"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(helpText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Select user") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (loading) {
                    DropdownMenuItem(text = { Text("Loading...") }, onClick = {})
                } else if (users.isEmpty()) {
                    DropdownMenuItem(text = { Text("No users found") }, onClick = {})
                } else {
                    users.forEach { user ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(user.fullName.orNA(), fontWeight = FontWeight.Medium)
                                    Text(
                                        "${user.officialRole.orNA()}${user.workerSpecialization?.let { " • $it" }.orEmpty()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onSelected(user.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            label = { Text("Note") }
        )

        Button(
            onClick = onButtonClick,
            enabled = !buttonLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (buttonLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun ProgressUpdateSection(
    progressUpdate: String,
    onProgressChange: (String) -> Unit,
    onUpdateWork: () -> Unit,
    loading: Boolean,
    roleHint: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(roleHint, fontWeight = FontWeight.SemiBold)
                Text("Keep the case log current with field updates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = progressUpdate,
            onValueChange = onProgressChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            placeholder = { Text("Describe the current work status") },
            minLines = 4
        )

        Button(
            onClick = onUpdateWork,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Submit work update")
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(100.dp)
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = status.toStatusColor()
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}
