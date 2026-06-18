package com.safelive.app.presentation.official
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.data.remote.api.IncidentApi
import com.safelive.app.data.remote.dto.DashboardDataDto
import com.safelive.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- ViewModel ----

data class AnalyticsUiState(
    val data: DashboardDataDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OfficialAnalyticsViewModel @Inject constructor(
    private val incidentApi: IncidentApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init { loadAnalytics() }

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = incidentApi.getDashboardData()
                if (response.success && response.data != null) {
                    _uiState.update { it.copy(data = response.data, isLoading = false) }
                } else {
                    _uiState.update { it.copy(error = response.error ?: "Failed to load analytics", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Unknown error", isLoading = false) }
            }
        }
    }
}

// ---- Screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialAnalyticsScreen(
    navController: NavController,
    viewModel: OfficialAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalytics() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
            return@Scaffold
        }

        if (uiState.error != null && uiState.data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.error ?: "Error", color = DangerRed)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAnalytics() }) { Text("Retry") }
                }
            }
            return@Scaffold
        }

        val data = uiState.data ?: DashboardDataDto()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Overview Card ----
            item {
                AnalyticsOverviewCard(data = data)
            }

            // ---- Resolution Rate ----
            item {
                AnalyticsMetricCard(
                    title = "Resolution Rate",
                    value = "${(data.resolutionRate * 100).toInt()}%",
                    subtitle = "of all incidents resolved",
                    color = SuccessGreen,
                    icon = Icons.Default.CheckCircle
                )
            }

            // ---- Avg Resolution Time ----
            item {
                AnalyticsMetricCard(
                    title = "Avg Resolution Time",
                    value = "${data.avgResolutionHours.toInt()} hrs",
                    subtitle = "from report to resolution",
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.AccessTime
                )
            }

            // ---- Category Breakdown ----
            if (data.categoryBreakdown.isNotEmpty()) {
                item {
                    BreakdownCard(
                        title = "Category Breakdown",
                        items = data.categoryBreakdown,
                        totalValue = data.totalIssues.coerceAtLeast(1)
                    )
                }
            }

            // ---- Priority Breakdown ----
            if (data.priorityBreakdown.isNotEmpty()) {
                item {
                    val priorityColors = mapOf(
                        "critical" to PriorityCritical,
                        "high" to PriorityHigh,
                        "medium" to PriorityMedium,
                        "low" to PriorityLow
                    )
                    BreakdownCard(
                        title = "Priority Distribution",
                        items = data.priorityBreakdown,
                        totalValue = data.totalIssues.coerceAtLeast(1),
                        colorMap = priorityColors
                    )
                }
            }

            // ---- Status Breakdown ----
            if (data.statusBreakdown.isNotEmpty()) {
                item {
                    val statusColors = mapOf(
                        "open" to WarningOrange,
                        "in_progress" to MaterialTheme.colorScheme.primary,
                        "resolved" to SuccessGreen,
                        "pending" to PriorityMedium,
                        "rejected" to DangerRed
                    )
                    BreakdownCard(
                        title = "Status Breakdown",
                        items = data.statusBreakdown,
                        totalValue = data.totalIssues.coerceAtLeast(1),
                        colorMap = statusColors
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsOverviewCard(data: DashboardDataDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Overview", color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OverviewStat("Total", data.totalIssues, Color.White)
                    OverviewStat("Open", data.openIssues, Color(0xFFFDE047))
                    OverviewStat("Resolved", data.resolvedIssues, Color(0xFF86EFAC))
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    OverviewStat("Pending", data.pendingIssues, Color(0xFFFDBA74))
                    OverviewStat("In Progress", data.inProgressIssues, Color(0xFF7DD3FC))
                }
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun AnalyticsMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = color)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    items: Map<String, Int>,
    totalValue: Int,
    colorMap: Map<String, Color> = emptyMap()
) {
    val defaultColors = listOf(MaterialTheme.colorScheme.primary, SuccessGreen, WarningOrange, MaterialTheme.colorScheme.secondary,
        PriorityCritical, PriorityHigh, PriorityMedium, PriorityLow)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            items.entries.sortedByDescending { it.value }.forEachIndexed { idx, (key, count) ->
                val color = colorMap[key.lowercase()] ?: defaultColors[idx % defaultColors.size]
                val percent = (count.toFloat() / totalValue * 100).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        key.replace("_", " ").replaceFirstChar { it.uppercaseChar() },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("$count", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = color)
                    Spacer(Modifier.width(4.dp))
                    Text("($percent%)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                LinearProgressIndicator(
                    progress = { count.toFloat() / totalValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.12f)
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
