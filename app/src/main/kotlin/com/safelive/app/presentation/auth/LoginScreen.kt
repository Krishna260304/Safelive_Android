@file:Suppress("DEPRECATION")
package com.safelive.app.presentation.auth
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.navigation.Screen

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val route = if (uiState.userType?.lowercase() == "official") {
                Screen.OfficialDashboard.route
            } else {
                Screen.CitizenDashboard.route
            }
            navController.navigate(route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFF5F5F5)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).align(Alignment.Start)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.safelive.app.R.drawable.safelive_logo),
                    contentDescription = "SafeLive Logo",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Text("SafeLive", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    if (uiState.otpChallengeId != null) {
                        LoginOtpContent(uiState, viewModel)
                    } else if (uiState.loginMode == "local") {
                        LocalLoginContent(uiState, viewModel, focusManager, navController)
                    } else {
                        OfficialLoginContent(uiState, viewModel, focusManager, navController)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoginOtpContent(uiState: LoginUiState, viewModel: LoginViewModel) {
    Text("Verification Code", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Enter the one-time code sent to ${uiState.otpChannels.joinToString(" and ").ifBlank { "your registered contact" }}.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))
    OutlinedTextField(
        value = uiState.otp,
        onValueChange = viewModel::onOtpChange,
        label = { Text("6-digit OTP") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    ErrorDisplay(uiState.error)
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = viewModel::verifyLoginOtp,
        enabled = !uiState.isLoading && uiState.otp.length == 6,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
        else Text("Verify & Continue", color = Color.White)
    }
    TextButton(onClick = viewModel::cancelOtp) { Text("Back to Login") }
}

@Composable
private fun LocalLoginContent(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    navController: NavController
) {
    Text("Local Login", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Report and track incidents in SafeLive", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

    Spacer(modifier = Modifier.height(24.dp))

    // Login With Toggle
    Text("Login With", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoleButton(
            title = "Email",
            isSelected = uiState.loginWith == "email",
            onClick = { viewModel.onLoginWithChange("email") },
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Email
        )
        RoleButton(
            title = "Phone",
            isSelected = uiState.loginWith == "phone",
            onClick = { viewModel.onLoginWithChange("phone") },
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Phone
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (uiState.loginWith == "email") {
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
    } else {
        OutlinedTextField(
            value = uiState.phone,
            onValueChange = viewModel::onPhoneChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = uiState.password,
        onValueChange = viewModel::onPasswordChange,
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = viewModel::togglePasswordVisibility) {
                Icon(if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
    
    TextButton(
        onClick = { navController.navigate(Screen.ForgotPassword.route) },
        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)
    ) {
        Text("Forgot Password?", color = MaterialTheme.colorScheme.primary)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text("Security Check", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.background(Color(0xFFF0F2F5), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(uiState.securityQuestion, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        IconButton(onClick = viewModel::refreshSecurityQuestion) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = uiState.userSecurityAnswer,
        onValueChange = viewModel::onSecurityAnswerChange,
        placeholder = { Text("Enter answer") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.login() })
    )

    Spacer(modifier = Modifier.height(24.dp))
    
    ErrorDisplay(uiState.error)

    Button(
        onClick = viewModel::login,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Login, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Don't have an account? ", style = MaterialTheme.typography.bodySmall)
        Text("Register Here", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary), modifier = Modifier.clickable { navController.navigate(Screen.Register.route) })
    }

    Spacer(modifier = Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        Text(" OR ", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
    }
    Spacer(modifier = Modifier.height(16.dp))

    Text("Are you an official?", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = { viewModel.onLoginModeChange("official") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
    ) {
        Text("Official Admin Login")
    }
}

@Composable
private fun OfficialLoginContent(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    navController: NavController
) {
    Text("Official Login", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Department, supervisor, field inspector, and worker access", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)

    Spacer(modifier = Modifier.height(24.dp))

    Text("Official Role", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    Spacer(modifier = Modifier.height(8.dp))
    
    // Role Grid
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoleButton(
                title = "Department Login",
                isSelected = uiState.officialRole == "Department Login",
                onClick = { viewModel.onOfficialRoleChange("Department Login") },
                modifier = Modifier.weight(1f)
            )
            RoleButton(
                title = "Supervisor Login",
                isSelected = uiState.officialRole == "Supervisor Login",
                onClick = { viewModel.onOfficialRoleChange("Supervisor Login") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoleButton(
                title = "Field Inspector Login",
                isSelected = uiState.officialRole == "Field Inspector Login",
                onClick = { viewModel.onOfficialRoleChange("Field Inspector Login") },
                modifier = Modifier.weight(1f)
            )
            RoleButton(
                title = "Worker Login",
                isSelected = uiState.officialRole == "Worker Login",
                onClick = { viewModel.onOfficialRoleChange("Worker Login") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChange,
        label = { Text("Official Email") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = uiState.password,
        onValueChange = viewModel::onPasswordChange,
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = viewModel::togglePasswordVisibility) {
                Icon(if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
    )
    
    TextButton(
        onClick = { navController.navigate(Screen.ForgotPassword.route) },
        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)
    ) {
        Text("Forgot Password?", color = MaterialTheme.colorScheme.primary)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text("Security Check", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.background(Color(0xFFF0F2F5), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(uiState.securityQuestion, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        IconButton(onClick = viewModel::refreshSecurityQuestion) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = uiState.userSecurityAnswer,
        onValueChange = viewModel::onSecurityAnswerChange,
        placeholder = { Text("Enter answer") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.login() })
    )

    Spacer(modifier = Modifier.height(24.dp))
    
    ErrorDisplay(uiState.error)

    Button(
        onClick = viewModel::login,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Login, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In as ${uiState.officialRole}")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Need a new account? ", style = MaterialTheme.typography.bodySmall)
        Text("Register", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary), modifier = Modifier.clickable { navController.navigate(Screen.Register.route) })
    }
    Text("Supervisor and field inspector accounts are created by department users.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = Color.Gray, textAlign = TextAlign.Center)

    Spacer(modifier = Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        Text(" OR ", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
    }
    Spacer(modifier = Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Local user? ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text("Login here", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary), modifier = Modifier.clickable { viewModel.onLoginModeChange("local") })
    }
}

@Composable
private fun RoleButton(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorDisplay(error: String?) {
    AnimatedVisibility(visible = error != null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(error ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
