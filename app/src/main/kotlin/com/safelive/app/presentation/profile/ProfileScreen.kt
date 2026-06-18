package com.safelive.app.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.safelive.app.navigation.Screen
import com.safelive.app.presentation.auth.SafeLiveTextField
import com.safelive.app.ui.theme.PrimaryBlue
import com.safelive.app.ui.theme.SecondaryTeal
import com.safelive.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.uploadPhoto(uri, context)
            }
        }
    )

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.EditProfile.route) }) {
                        Icon(Icons.Default.Edit, null, tint = Color.White)
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
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PrimaryBlue, SecondaryTeal)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!uiState.user?.profilePictureUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = uiState.user?.profilePictureUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = uiState.user?.fullName?.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 36.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Camera icon overlay
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .background(PrimaryBlue, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Upload Photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.user?.fullName ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "🛡️ ${uiState.user?.userType?.replaceFirstChar { it.uppercase() } ?: ""}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileInfoItem(
                        icon = Icons.Default.Person,
                        label = "Full Name",
                        value = uiState.user?.fullName ?: "—"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = uiState.user?.email ?: "—"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(
                        icon = Icons.Default.Phone,
                        label = "Mobile",
                        value = uiState.user?.phone ?: "—"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoItem(
                        icon = Icons.Default.Badge,
                        label = "Account Type",
                        value = uiState.user?.userType?.replaceFirstChar { it.uppercase() } ?: "—"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ProfileActionItem(
                        icon = Icons.Default.Edit,
                        label = "Edit Profile",
                        onClick = { navController.navigate(Screen.EditProfile.route) }
                    )
                    HorizontalDivider()
                    ProfileActionItem(
                        icon = Icons.Default.Lock,
                        label = "Change Password",
                        onClick = { navController.navigate(Screen.ChangePassword.route) }
                    )
                    HorizontalDivider()
                    ProfileActionItem(
                        icon = Icons.Default.Notifications,
                        label = "Notification Settings",
                        onClick = { navController.navigate(Screen.NotificationSettings.route) }
                    )
                    HorizontalDivider()
                    ProfileActionItem(
                        icon = Icons.Default.PrivacyTip,
                        label = "Privacy Policy",
                        onClick = {  }
                    )
                    HorizontalDivider()
                    ProfileActionItem(
                        icon = Icons.Default.Logout,
                        label = "Sign Out",
                        color = MaterialTheme.colorScheme.error,
                        onClick = { showLogoutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SafeLive v1.0.0 • Government of India",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProfileActionItem(icon: ImageVector, label: String, color: Color? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color ?: PrimaryBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) navController.navigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SafeLiveTextField(
                value = uiState.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = "Full Name",
                keyboardType = KeyboardType.Text
            )

            SafeLiveTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = "Mobile Number",
                keyboardType = KeyboardType.Phone
            )

            SafeLiveTextField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                label = "Address",
                keyboardType = KeyboardType.Text
            )

            SafeLiveTextField(
                value = uiState.pincode,
                onValueChange = viewModel::onPincodeChange,
                label = "Pincode",
                keyboardType = KeyboardType.Number
            )
            if (uiState.isCheckingPincode) {
                Text(
                    text = "Checking pincode...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            } else if (!uiState.pincodeLookupMessage.isNullOrBlank()) {
                Text(
                    text = uiState.pincodeLookupMessage!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isPincodeValid) SuccessGreen else MaterialTheme.colorScheme.error
                )
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::updateProfile,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save Changes", color = Color.White)
                }
            }
        }
    }
}
