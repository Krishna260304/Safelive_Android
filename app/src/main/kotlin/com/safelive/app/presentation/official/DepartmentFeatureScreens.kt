package com.safelive.app.presentation.official
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions as KO
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import com.safelive.app.data.remote.api.ProfileApi
import com.safelive.app.data.remote.dto.UserDto
import com.safelive.app.data.websocket.SocketEvent
import com.safelive.app.data.websocket.WebSocketManager
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.repository.PincodeRepository
import com.safelive.app.domain.usecase.incident.GetIncidentsUseCase
import com.safelive.app.ui.theme.SuccessGreen
import com.safelive.app.ui.theme.PriorityCritical
import com.safelive.app.utils.ValidationUtils
import com.safelive.app.utils.toApiErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import retrofit2.HttpException
import javax.inject.Inject

// ─────────────────────────────────────────────
// TEAM MANAGEMENT
// ─────────────────────────────────────────────

data class TeamManagementUiState(
    val selectedRole: String = "supervisor",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val pincode: String = "",
    val pincodeLookupMessage: String? = null,
    val isCheckingPincode: Boolean = false,
    val isPincodeValid: Boolean = false,
    val address: String = "",
    val tempPassword: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val teamMembers: List<UserDto> = emptyList(),
    val isLoadingTeam: Boolean = false,
    val detectedOfficialRole: String? = null,
    val isDepartmentAccount: Boolean = false,
    val isCheckingAccess: Boolean = true
)

@HiltViewModel
class TeamManagementViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val pincodeRepository: PincodeRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamManagementUiState())
    val uiState: StateFlow<TeamManagementUiState> = _uiState.asStateFlow()

    private var pincodeLookupJob: Job? = null

    init { resolveAccess() }

    fun onRoleChange(role: String) = _uiState.update { it.copy(selectedRole = role, error = null, successMessage = null) }
    fun onFullNameChange(v: String) = _uiState.update { it.copy(fullName = v, error = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = ValidationUtils.digitsOnly(v, 10)) }
    fun onPincodeChange(v: String) {
        val trimmed = ValidationUtils.digitsOnly(v, 6)
        _uiState.update {
            it.copy(
                pincode = trimmed,
                pincodeLookupMessage = null,
                isCheckingPincode = false,
                isPincodeValid = false
            )
        }

        if (trimmed.length == 6 && ValidationUtils.isValidPincode(trimmed)) {
            pincodeLookupJob?.cancel()
            pincodeLookupJob = viewModelScope.launch {
                _uiState.update { it.copy(isCheckingPincode = true) }
                when (val result = pincodeRepository.lookupPincode(trimmed)) {
                    is com.safelive.app.utils.Resource.Success -> _uiState.update {
                        it.copy(
                            isCheckingPincode = false,
                            pincodeLookupMessage = result.data,
                            isPincodeValid = true,
                            error = null
                        )
                    }

                    is com.safelive.app.utils.Resource.Error -> _uiState.update {
                        it.copy(
                            isCheckingPincode = false,
                            pincodeLookupMessage = result.message,
                            isPincodeValid = false
                        )
                    }

                    is com.safelive.app.utils.Resource.OtpRequired -> Unit
                    com.safelive.app.utils.Resource.Loading -> Unit
                }
            }
        }
    }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }
    fun onTempPasswordChange(v: String) = _uiState.update { it.copy(tempPassword = v, error = null) }
    fun toggleShowPassword() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun resetForm() = _uiState.update {
        it.copy(fullName = "", email = "", phone = "", pincode = "", pincodeLookupMessage = null, isCheckingPincode = false, isPincodeValid = false, address = "", tempPassword = "", error = null, successMessage = null)
    }

    private fun isDepartmentRole(role: String?): Boolean {
        val normalized = role?.trim()?.lowercase().orEmpty()
        return normalized.contains("department")
    }

    private fun resolveAccess() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingAccess = true, error = null) }
            try {
                val savedRole = userPreferencesDataStore.officialRole.firstOrNull()
                val resolvedRole = if (savedRole.isNullOrBlank()) {
                    val profileResult = profileApi.getProfile()
                    val profileRole = profileResult.data?.officialRole ?: profileResult.data?.userType
                    profileRole?.let { userPreferencesDataStore.saveOfficialRole(it) }
                    profileRole
                } else {
                    savedRole
                }

                val isDepartment = isDepartmentRole(resolvedRole)
                _uiState.update {
                    it.copy(
                        detectedOfficialRole = resolvedRole,
                        isDepartmentAccount = isDepartment,
                        isCheckingAccess = false,
                        teamMembers = if (isDepartment) it.teamMembers else emptyList(),
                        isLoadingTeam = false,
                        error = if (isDepartment) null else "This feature is only available to department accounts"
                    )
                }

                if (isDepartment) {
                    loadTeamMembers()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingAccess = false,
                        error = e.localizedMessage ?: "Unable to verify department access"
                    )
                }
            }
        }
    }

    private fun loadTeamMembers() {
        viewModelScope.launch {
            if (!_uiState.value.isDepartmentAccount) {
                _uiState.update { it.copy(isLoadingTeam = false) }
                return@launch
            }
            _uiState.update { it.copy(isLoadingTeam = true) }
            try {
                val result = profileApi.getManagedOfficials()
                if (result.success && result.data != null) {
                    _uiState.update { it.copy(teamMembers = result.data, isLoadingTeam = false) }
                } else {
                    _uiState.update { it.copy(isLoadingTeam = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingTeam = false) }
            }
        }
    }

    fun createOfficial() {
        val state = _uiState.value
        when {
            !ValidationUtils.isValidName(state.fullName) -> {
                _uiState.update { it.copy(error = "Enter a valid full name") }
                return
            }
            !ValidationUtils.isValidEmail(state.email) -> {
                _uiState.update { it.copy(error = "Enter a valid email address") }
                return
            }
            state.phone.isNotBlank() && !ValidationUtils.isValidPhone(state.phone) -> {
                _uiState.update { it.copy(error = "Enter a valid 10-digit mobile number") }
                return
            }
            !ValidationUtils.isValidPassword(state.tempPassword) -> {
                _uiState.update { it.copy(error = "Password must be at least 8 characters and include letters and numbers") }
                return
            }
            state.pincode.isNotBlank() && !ValidationUtils.isValidPincode(state.pincode) -> {
                _uiState.update { it.copy(error = "Pincode must be a 6-digit number") }
                return
            }
            state.pincode.trim().length == 6 && !state.isPincodeValid -> {
                _uiState.update { it.copy(error = state.pincodeLookupMessage ?: "Please validate the pincode") }
                return
            }
        }

        if (state.selectedRole != "supervisor" && state.selectedRole != "field_inspector") {
            _uiState.update { it.copy(error = "Select a valid official role") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val body = mutableMapOf(
                    "name" to state.fullName,
                    "email" to state.email,
                    "password" to state.tempPassword,
                    "officialRole" to state.selectedRole
                )
                if (state.phone.isNotBlank()) body["phone"] = state.phone
                if (state.pincode.isNotBlank()) body["pincode"] = state.pincode
                if (state.address.isNotBlank()) body["address"] = state.address

                val result = profileApi.createManagedOfficial(body)
                if (result.success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "${if (state.selectedRole == "supervisor") "Supervisor" else "Field Inspector"} account created successfully",
                            fullName = "", email = "", phone = "", pincode = "", address = "", tempPassword = ""
                        )
                    }
                    loadTeamMembers()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.error ?: "Failed to create account") }
                }
            } catch (e: Exception) {
                val serverMessage = e.toApiErrorMessage()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = serverMessage ?: e.localizedMessage ?: "Unknown error"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(
    navController: NavController,
    viewModel: TeamManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("Team Management", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Create and manage team accounts", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isCheckingAccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else if (!uiState.isDepartmentAccount) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Access restricted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            uiState.detectedOfficialRole?.let { "Detected role: $it" } ?: "We couldn't verify a department role for this session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            "Please sign in with a department account to create supervisor and field inspector accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (!uiState.isCheckingAccess && uiState.isDepartmentAccount) {
            // ── Create Official Account Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f))
            ) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f),
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Create Official Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Role selector
                    Text("Official Role", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("supervisor" to "Supervisor", "field_inspector" to "Field Inspector").forEach { (key, label) ->
                            Button(
                                onClick = { viewModel.onRoleChange(key) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.selectedRole == key) MaterialTheme.colorScheme.secondary else Color(0xFFE8F4F8),
                                    contentColor = if (uiState.selectedRole == key) Color.White else MaterialTheme.colorScheme.secondary
                                )
                            ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                        }
                    }

                    // Full Name & Email in a row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Full Name", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.fullName,
                                onValueChange = viewModel::onFullNameChange,
                                placeholder = { Text("Enter full name", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = fieldColors
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = viewModel::onEmailChange,
                                placeholder = { Text("official@safelive.in", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KO(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = fieldColors
                            )
                        }
                    }

                    // Phone & Temp Password
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phone (Optional)", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.phone,
                                onValueChange = viewModel::onPhoneChange,
                                placeholder = { Text("9876543210", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KO(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = fieldColors
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Temporary Password", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.tempPassword,
                                onValueChange = viewModel::onTempPasswordChange,
                                placeholder = { Text("At least 8 chars", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                                singleLine = true,
                                visualTransformation = if (uiState.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = viewModel::toggleShowPassword) {
                                        Icon(if (uiState.showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = fieldColors
                            )
                        }
                    }

                    // Pincode
                    Column {
                        Text("Pincode (Optional)", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.pincode,
                            onValueChange = viewModel::onPincodeChange,
                            placeholder = { Text("751024", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                            singleLine = true,
                            keyboardOptions = KO(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(0.5f),
                            shape = RoundedCornerShape(8.dp),
                            colors = fieldColors
                        )
                        if (uiState.isCheckingPincode) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Checking pincode...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        } else if (!uiState.pincodeLookupMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.pincodeLookupMessage!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isPincodeValid) SuccessGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Address
                    Column {
                        Text("Address (Optional)", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.address,
                            onValueChange = viewModel::onAddressChange,
                            placeholder = { Text("Office or area address", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = fieldColors
                        )
                    }

                    // Feedback
                    if (uiState.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(uiState.error!!, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (uiState.successMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(uiState.successMessage!!, color = SuccessGreen, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = viewModel::resetForm, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                        Button(
                            onClick = viewModel::createOfficial,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Account")
                            }
                        }
                    }
                }
            }

            // ── Added Supervisors / Field Inspectors ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Added Supervisors / Field Inspectors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (uiState.isLoadingTeam) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    } else if (uiState.teamMembers.isEmpty()) {
                        Text(
                            "No supervisor or field inspector accounts created yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        uiState.teamMembers.forEach { member ->
                            TeamMemberRow(member)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun TeamMemberRow(user: UserDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user.fullName ?: "?").take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName ?: "Unknown", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val role = user.officialRole ?: user.userType ?: "official"
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = role.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// OFFICIAL ALERTS
// ─────────────────────────────────────────────

data class AlertItem(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "critical" or "standard"
    val time: String
)

data class OfficialAlertsUiState(
    val alerts: List<AlertItem> = emptyList(),
    val filter: String = "all", // "all" or "critical"
    val isLoading: Boolean = false
)

@HiltViewModel
class OfficialAlertsViewModel @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val getIncidentsUseCase: GetIncidentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfficialAlertsUiState(isLoading = true))
    val uiState: StateFlow<OfficialAlertsUiState> = _uiState.asStateFlow()
    private var incidentsJob: Job? = null

    init {
        observeWebSocketAlerts()
        // Start with empty live feed — populated by WebSocket events
        refreshIncidents()
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                refreshIncidents()
            }
        }
    }

    fun setFilter(filter: String) = _uiState.update { it.copy(filter = filter) }

    fun refreshIncidents() {
        incidentsJob?.cancel()
        incidentsJob = viewModelScope.launch {
            getIncidentsUseCase(page = 1).collect { result ->
                when (result) {
                    is com.safelive.app.utils.Resource.Success -> {
                        val alerts = result.data
                            .sortedByDescending { it.createdAt }
                            .map { incident ->
                                AlertItem(
                                    id = incident.id,
                                    title = incident.title,
                                    description = incident.description.orEmpty(),
                                    type = if (incident.priority.equals("high", true) || incident.priority.equals("critical", true)) "critical" else "standard",
                                    time = incident.createdAt
                                )
                            }
                        _uiState.update { it.copy(alerts = alerts, isLoading = false) }
                    }
                    is com.safelive.app.utils.Resource.Error -> _uiState.update { it.copy(isLoading = false) }
                    is com.safelive.app.utils.Resource.OtpRequired -> Unit
                    com.safelive.app.utils.Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun observeWebSocketAlerts() {
        viewModelScope.launch {
            webSocketManager.socketEvents.collect { event ->
                when (event) {
                    is SocketEvent.EmergencyAlert -> {
                        val newAlert = AlertItem(
                            id = System.currentTimeMillis().toString(),
                            title = "Emergency Alert",
                            description = event.message,
                            type = if (event.severity == "high" || event.severity == "critical") "critical" else "standard",
                            time = "Just now"
                        )
                        _uiState.update { it.copy(alerts = listOf(newAlert) + it.alerts) }
                    }
                    is SocketEvent.IncidentCreated -> {
                        val newAlert = AlertItem(
                            id = System.currentTimeMillis().toString(),
                            title = "New Incident Reported",
                            description = "A new incident has been reported in your zone",
                            type = "standard",
                            time = "Just now"
                        )
                        _uiState.update { it.copy(alerts = listOf(newAlert) + it.alerts) }
                    }
                    else -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialAlertsScreen(
    navController: NavController,
    viewModel: OfficialAlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredAlerts = if (uiState.filter == "critical") {
        uiState.alerts.filter { it.type == "critical" }
    } else {
        uiState.alerts
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("Real-Time Alert Center", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Live monitoring of city incidents", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    }
                    // Filter toggles
                    Row(modifier = Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("all" to "All Alerts", "critical" to "Critical (${uiState.alerts.count { it.type == "critical" }})").forEach { (key, label) ->
                            FilterChip(
                                selected = uiState.filter == key,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = MaterialTheme.colorScheme.secondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = uiState.filter == key,
                                    borderColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Feed Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(color = if (filteredAlerts.isEmpty()) Color(0xFFF0F2F5) else PriorityCritical.copy(alpha = 0.15f), shape = RoundedCornerShape(100.dp)) {
                    Text(
                        "${filteredAlerts.size} Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (filteredAlerts.isEmpty()) Color.Gray else PriorityCritical,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            } else if (filteredAlerts.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("All clear. No active alerts.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                filteredAlerts.forEach { alert ->
                    AlertCard(alert)
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(PriorityCritical, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Critical", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Standard", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertItem) {
    val isCritical = alert.type == "critical"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) PriorityCritical.copy(alpha = 0.06f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCritical) PriorityCritical.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    if (isCritical) PriorityCritical.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCritical) Icons.Default.Warning else Icons.Default.Notifications,
                    null,
                    tint = if (isCritical) PriorityCritical else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(alert.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(alert.time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
