package com.safelive.app.presentation.profile
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.domain.repository.ProfileRepository
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val otp: String = "",
    val challengeId: String? = null,
    val isOtpSent: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val twoFactorChallengeId: String? = null,
    val twoFactorOtp: String = "",
    val pendingTwoFactorEnable: Boolean? = null,
    val isTwoFactorOtpSent: Boolean = false,
    val isTwoFactorLoading: Boolean = false,
    val isLoading: Boolean = false,
    val showCurrentPassword: Boolean = false,
    val showNewPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    init {
        loadProfileState()
    }

    fun onCurrentPasswordChange(password: String) = _uiState.update { it.copy(currentPassword = password) }
    fun onNewPasswordChange(password: String) = _uiState.update { it.copy(newPassword = password) }
    fun onConfirmPasswordChange(password: String) = _uiState.update { it.copy(confirmPassword = password) }
    fun toggleShowCurrentPassword() = _uiState.update { it.copy(showCurrentPassword = !it.showCurrentPassword) }
    fun toggleShowNewPassword() = _uiState.update { it.copy(showNewPassword = !it.showNewPassword) }
    fun toggleShowConfirmPassword() = _uiState.update { it.copy(showConfirmPassword = !it.showConfirmPassword) }
    fun onOtpChange(otp: String) = _uiState.update { it.copy(otp = otp) }
    fun onTwoFactorOtpChange(otp: String) = _uiState.update { it.copy(twoFactorOtp = otp, error = null) }

    private fun loadProfileState() {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is Resource.Success -> _uiState.update { it.copy(twoFactorEnabled = result.data.twoFactorEnabled) }
                is Resource.Error -> Unit
                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    fun requestOtp() {
        if (_uiState.value.newPassword != _uiState.value.confirmPassword) {
            _uiState.update { it.copy(error = "New passwords do not match") }
            return
        }
        
        if (_uiState.value.newPassword.isBlank() || _uiState.value.currentPassword.isBlank()) {
            _uiState.update { it.copy(error = "Please fill all fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = authRepository.requestChangePasswordOtp(_uiState.value.currentPassword)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isOtpSent = true, challengeId = result.data, successMessage = "OTP sent to your registered email/phone") }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    fun changePassword() {
        val challengeId = _uiState.value.challengeId
        if (challengeId == null) {
            _uiState.update { it.copy(error = "Invalid session, request OTP again") }
            return
        }
        if (_uiState.value.otp.isBlank()) {
            _uiState.update { it.copy(error = "Please enter OTP") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = authRepository.confirmChangePassword(
                challengeId = challengeId,
                otp = _uiState.value.otp,
                newPassword = _uiState.value.newPassword
            )) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, successMessage = result.data, currentPassword = "", newPassword = "", confirmPassword = "", otp = "", challengeId = null, isOtpSent = false) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    fun requestTwoFactorChange(enable: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTwoFactorLoading = true, error = null, successMessage = null) }
            val result = if (enable) {
                authRepository.requestEnable2FAOtp()
            } else {
                authRepository.requestDisable2FAOtp()
            }
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isTwoFactorLoading = false,
                        twoFactorChallengeId = result.data,
                        twoFactorOtp = "",
                        pendingTwoFactorEnable = enable,
                        isTwoFactorOtpSent = true,
                        successMessage = "OTP sent to your registered email/phone"
                    )
                }
                is Resource.Error -> _uiState.update { it.copy(isTwoFactorLoading = false, error = result.message) }
                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    fun confirmTwoFactorChange() {
        val challengeId = _uiState.value.twoFactorChallengeId ?: run {
            _uiState.update { it.copy(error = "Please request OTP again") }
            return
        }
        val otp = _uiState.value.twoFactorOtp.trim()
        val enable = _uiState.value.pendingTwoFactorEnable ?: run {
            _uiState.update { it.copy(error = "Please request OTP again") }
            return
        }
        if (otp.isBlank()) {
            _uiState.update { it.copy(error = "Please enter OTP") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTwoFactorLoading = true, error = null, successMessage = null) }
            val result = if (enable) {
                authRepository.confirmEnable2FA(challengeId, otp)
            } else {
                authRepository.confirmDisable2FA(challengeId, otp)
            }
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isTwoFactorLoading = false,
                        twoFactorEnabled = result.data.twoFactorEnabled,
                        twoFactorChallengeId = null,
                        twoFactorOtp = "",
                        pendingTwoFactorEnable = null,
                        isTwoFactorOtpSent = false,
                        successMessage = "Two-factor authentication ${if (result.data.twoFactorEnabled) "enabled" else "disabled"}."
                    )
                }
                is Resource.Error -> _uiState.update { it.copy(isTwoFactorLoading = false, error = result.message) }
                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    fun clearTwoFactorChallenge() {
        _uiState.update {
            it.copy(
                twoFactorChallengeId = null,
                twoFactorOtp = "",
                pendingTwoFactorEnable = null,
                isTwoFactorOtpSent = false,
                isTwoFactorLoading = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Change Password (OTP)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Current Password
                Column {
                    Text("Current Password", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.currentPassword,
                        onValueChange = viewModel::onCurrentPasswordChange,
                        placeholder = { Text("Enter current password", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleShowCurrentPassword) {
                                Icon(
                                    if (uiState.showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (uiState.showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // New Password
                Column {
                    Text("New Password", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        placeholder = { Text("Enter new password", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleShowNewPassword) {
                                Icon(
                                    if (uiState.showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (uiState.showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Confirm Password
                Column {
                    Text("Confirm Password", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        placeholder = { Text("Confirm new password", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleShowConfirmPassword) {
                                Icon(
                                    if (uiState.showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (uiState.showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                AnimatedVisibility(visible = uiState.isOtpSent) {
                    Column {
                        Text("Enter OTP", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.otp,
                            onValueChange = viewModel::onOtpChange,
                            placeholder = { Text("Enter OTP received", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (uiState.error != null) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }

                if (uiState.successMessage != null) {
                    Text(uiState.successMessage!!, color = Color(0xFF2E7D32)) // Green
                }

                Button(
                    onClick = {
                        if (uiState.isOtpSent) {
                            viewModel.changePassword()
                        } else {
                            viewModel.requestOtp()
                        }
                    },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (uiState.isOtpSent) "Confirm OTP & Change Password" else "Send OTP", color = Color.White)
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Two-Factor Authentication (OTP)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (uiState.twoFactorEnabled) "Two-factor authentication is on" else "Two-factor authentication is off",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Require OTP after password on login",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.twoFactorChallengeId == null) {
                            Button(
                                onClick = { viewModel.requestTwoFactorChange(!uiState.twoFactorEnabled) },
                                enabled = !uiState.isTwoFactorLoading,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (uiState.isTwoFactorLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = if (uiState.twoFactorEnabled) "Disable 2FA" else "Enable 2FA",
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "OTP sent to your registered email/phone.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = uiState.twoFactorOtp,
                                    onValueChange = viewModel::onTwoFactorOtpChange,
                                    placeholder = { Text("Enter OTP", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = viewModel::clearTwoFactorChallenge,
                                        enabled = !uiState.isTwoFactorLoading,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = viewModel::confirmTwoFactorChange,
                                        enabled = !uiState.isTwoFactorLoading,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        if (uiState.isTwoFactorLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text("Confirm OTP", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
