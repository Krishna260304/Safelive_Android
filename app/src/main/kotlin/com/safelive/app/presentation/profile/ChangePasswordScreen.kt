package com.safelive.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.safelive.app.ui.theme.PrimaryBlue
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
    val twoFactorEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val showCurrentPassword: Boolean = false,
    val showNewPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(password: String) = _uiState.update { it.copy(currentPassword = password) }
    fun onNewPasswordChange(password: String) = _uiState.update { it.copy(newPassword = password) }
    fun onConfirmPasswordChange(password: String) = _uiState.update { it.copy(confirmPassword = password) }
    fun toggleShowCurrentPassword() = _uiState.update { it.copy(showCurrentPassword = !it.showCurrentPassword) }
    fun toggleShowNewPassword() = _uiState.update { it.copy(showNewPassword = !it.showNewPassword) }
    fun toggleShowConfirmPassword() = _uiState.update { it.copy(showConfirmPassword = !it.showConfirmPassword) }

    fun changePassword() {
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
            when (val result = authRepository.changePassword(
                currentPassword = _uiState.value.currentPassword,
                newPassword = _uiState.value.newPassword
            )) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, successMessage = result.data, currentPassword = "", newPassword = "", confirmPassword = "") }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun toggle2FA(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(twoFactorEnabled = enabled) }
            val result = authRepository.toggle2FA(enabled)
            if (result is Resource.Error) {
                // Revert if failed
                _uiState.update { it.copy(twoFactorEnabled = !enabled, error = result.message) }
            }
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
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
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

                if (uiState.error != null) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }

                if (uiState.successMessage != null) {
                    Text(uiState.successMessage!!, color = Color(0xFF2E7D32)) // Green
                }

                Button(
                    onClick = viewModel::changePassword,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Send OTP", color = Color.White)
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable 2FA",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Require OTP after password on login",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.twoFactorEnabled,
                            onCheckedChange = viewModel::toggle2FA,
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }
        }
    }
}
