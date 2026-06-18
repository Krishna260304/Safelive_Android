package com.safelive.app.presentation.auth
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.navigation.Screen
import com.safelive.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate(Screen.CitizenDashboard.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Create Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Join SafeLive to report and track incidents", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Select Account Type", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        UserTypeCard(
                            type = "local",
                            selectedType = uiState.userType,
                            icon = Icons.Default.Person,
                            label = "Local User",
                            description = "Report incidents and track status",
                            onClick = { viewModel.onUserTypeChange("local") },
                            modifier = Modifier.weight(1f)
                        )
                        UserTypeCard(
                            type = "official",
                            selectedType = uiState.userType,
                            icon = Icons.Default.BusinessCenter,
                            label = "Official User",
                            description = "Department and worker registration",
                            onClick = { viewModel.onUserTypeChange("official") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState.userType == "local") {
                        // Local User Form
                        SafeLiveTextField(
                            value = uiState.fullName,
                            onValueChange = viewModel::onFullNameChange,
                            label = "Full Name",
                            placeholder = "Enter your name",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SafeLiveTextField(
                                value = uiState.email,
                                onValueChange = viewModel::onEmailChange,
                                label = "Email Address",
                                placeholder = "abc@gmail.com",
                                keyboardType = KeyboardType.Email,
                                modifier = Modifier.weight(1f)
                            )
                            SafeLiveTextField(
                                value = uiState.phone,
                                onValueChange = viewModel::onPhoneChange,
                                label = "Phone Number",
                                placeholder = "9876543210",
                                prefix = "+91",
                                keyboardType = KeyboardType.Phone,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SafeLiveTextField(
                            value = uiState.address,
                            onValueChange = viewModel::onAddressChange,
                            label = "Address",
                            placeholder = "Enter your complete address",
                            keyboardType = KeyboardType.Text,
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SafeLiveTextField(
                            value = uiState.pincode,
                            onValueChange = viewModel::onPincodeChange,
                            label = "Pincode",
                            placeholder = "123456",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth(0.5f).align(Alignment.Start)
                        )
                        if (uiState.isCheckingPincode) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Checking pincode...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        } else if (!uiState.pincodeLookupMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.pincodeLookupMessage!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isPincodeValid) SuccessGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { Text("Password", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("Create a strong password", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = { Text("Confirm Password", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("Confirm your password", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                    } else {
                        // Official User Form
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Official Role", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isDepartment = uiState.officialRole == "department"
                            Button(
                                onClick = { viewModel.onOfficialRoleChange("department") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDepartment) MaterialTheme.colorScheme.primary else Color.White,
                                    contentColor = if (isDepartment) Color.White else Color.Black
                                ),
                                border = BorderStroke(1.dp, if (isDepartment) MaterialTheme.colorScheme.primary else Color.LightGray)
                            ) {
                                Text("Department", style = MaterialTheme.typography.labelMedium)
                            }
                            
                            val isWorker = uiState.officialRole == "worker"
                            Button(
                                onClick = { viewModel.onOfficialRoleChange("worker") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isWorker) MaterialTheme.colorScheme.primary else Color.White,
                                    contentColor = if (isWorker) Color.White else Color.Black
                                ),
                                border = BorderStroke(1.dp, if (isWorker) MaterialTheme.colorScheme.primary else Color.LightGray)
                            ) {
                                Text("Worker", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        if (uiState.officialRole == "worker") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Worker Category", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val workerCategories = listOf(
                                "Road Maintenance Worker",
                                "Electrician",
                                "Plumber",
                                "Drainage Worker",
                                "Sanitation Worker",
                                "Water Supply Technician",
                                "Technician",
                                "Emergency Response Worker",
                                "Security Officer",
                                "Complaint Manager",
                                "Operations Manager",
                                "General Worker",
                                "Other"
                            )
                            
                            ExposedDropdownMenuBox(
                                expanded = uiState.workerSpecializationExpanded,
                                onExpandedChange = viewModel::onWorkerSpecializationExpandedChange
                            ) {
                                OutlinedTextField(
                                    value = uiState.workerSpecialization.ifEmpty { "Select worker category" },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.workerSpecializationExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (uiState.workerSpecialization.isEmpty()) Color.Gray else Color.Black
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = uiState.workerSpecializationExpanded,
                                    onDismissRequest = { viewModel.onWorkerSpecializationExpandedChange(false) }
                                ) {
                                    workerCategories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category) },
                                            onClick = { viewModel.onWorkerSpecializationChange(category) }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        SafeLiveTextField(
                            value = uiState.fullName,
                            onValueChange = viewModel::onFullNameChange,
                            label = "Full Name",
                            placeholder = "Enter your name",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SafeLiveTextField(
                                value = uiState.email,
                                onValueChange = viewModel::onEmailChange,
                                label = if (uiState.officialRole == "worker") "Email Address (Optional)" else "Email Address",
                                placeholder = "abc@gmail.com",
                                keyboardType = KeyboardType.Email,
                                modifier = Modifier.weight(1f)
                            )
                            SafeLiveTextField(
                                value = uiState.phone,
                                onValueChange = viewModel::onPhoneChange,
                                label = "Phone Number",
                                placeholder = "9876543210",
                                prefix = "+91",
                                keyboardType = KeyboardType.Phone,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        SafeLiveTextField(
                            value = uiState.address,
                            onValueChange = viewModel::onAddressChange,
                            label = "Address",
                            placeholder = "Enter your complete address",
                            keyboardType = KeyboardType.Text,
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SafeLiveTextField(
                            value = uiState.pincode,
                            onValueChange = viewModel::onPincodeChange,
                            label = "Pincode",
                            placeholder = "123456",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth(0.5f).align(Alignment.Start)
                        )
                        if (uiState.isCheckingPincode) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Checking pincode...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        } else if (!uiState.pincodeLookupMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.pincodeLookupMessage!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isPincodeValid) SuccessGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { Text("Password", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("Create a strong password", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = { Text("Confirm Password", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("Confirm your password", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedVisibility(visible = uiState.error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::register,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.PersonAdd, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Account")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.Center) {
                        Text("Already have an account? ", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Sign In",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.clickable { navController.navigate(Screen.Login.route) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("© 2026 SafeLive. Smart City Incident Resolver.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UserTypeCard(
    type: String,
    selectedType: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = type == selectedType
    Card(
        modifier = modifier.clickable { onClick() }.height(120.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF0F2F5), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black)
            Text(description, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SafeLiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
            leadingIcon = prefix?.let { { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp)) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = minLines == 1,
            minLines = minLines,
            maxLines = if (minLines == 1) 1 else 5,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
