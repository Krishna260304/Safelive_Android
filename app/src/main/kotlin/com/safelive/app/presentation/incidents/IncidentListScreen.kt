package com.safelive.app.presentation.incidents
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.navigation.Screen
import com.safelive.app.presentation.dashboard.EmptyStateCard
import com.safelive.app.presentation.dashboard.IncidentSummaryCard
import com.safelive.app.presentation.dashboard.LoadingIncidentCard
import com.safelive.app.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    navController: NavController,
    viewModel: IncidentListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = { Text("Incidents", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                null, tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = { Text("Search incidents...", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchChange("") }) {
                                Icon(Icons.Default.Clear, null, tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.CreateIncident.route) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Report Issue") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                AnimatedVisibility(visible = showFilters) {
                    FilterChipsSection(
                        selectedStatus = uiState.selectedStatus,
                        selectedCategory = uiState.selectedCategory,
                        selectedPriority = uiState.selectedPriority,
                        onStatusSelect = viewModel::setStatusFilter,
                        onCategorySelect = viewModel::setCategoryFilter,
                        onPrioritySelect = viewModel::setPriorityFilter,
                        onClearAll = viewModel::clearFilters
                    )
                }

                if (uiState.selectedStatus != null || uiState.selectedCategory != null || uiState.selectedPriority != null) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filters active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = viewModel::clearFilters, contentPadding = PaddingValues(0.dp)) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isLoading) {
                        items(5) { LoadingIncidentCard() }
                    } else if (uiState.incidents.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = "📋",
                                title = "No incidents found",
                                message = if (uiState.searchQuery.isNotBlank() || uiState.selectedStatus != null)
                                    "Try adjusting your search or filters"
                                else "No incidents have been reported yet"
                            )
                        }
                    } else {
                        items(uiState.incidents, key = { it.id }) { incident ->
                            IncidentSummaryCard(
                                incident = incident,
                                onClick = { navController.navigate(Screen.IncidentDetail.createRoute(incident.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsSection(
    selectedStatus: String?,
    selectedCategory: String?,
    selectedPriority: String?,
    onStatusSelect: (String?) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onPrioritySelect: (String?) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text("Filter by Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Constants.INCIDENT_STATUSES) { status ->
                FilterChip(
                    selected = status == selectedStatus,
                    onClick = { onStatusSelect(if (status == selectedStatus) null else status) },
                    label = { Text(status) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Filter by Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Constants.PRIORITY_LEVELS) { priority ->
                FilterChip(
                    selected = priority == selectedPriority,
                    onClick = { onPrioritySelect(if (priority == selectedPriority) null else priority) },
                    label = { Text(priority) }
                )
            }
        }
    }
}
