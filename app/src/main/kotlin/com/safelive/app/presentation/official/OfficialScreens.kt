package com.safelive.app.presentation.official
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.domain.model.Incident
import com.safelive.app.domain.model.Ticket
import com.safelive.app.navigation.Screen
import com.safelive.app.presentation.dashboard.EmptyStateCard
import com.safelive.app.presentation.dashboard.StatusChip
import com.safelive.app.ui.theme.*
import com.safelive.app.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialDashboardScreen(
    navController: NavController,
    viewModel: OfficialDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text("${uiState.displayRole} Portal", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                HorizontalDivider()
                
                val navItems = if (uiState.officialRole.equals("supervisor", ignoreCase = true)) {
                    listOf(
                        Triple("Dashboard", Icons.Default.Home, null as String?),
                        Triple("Tickets", Icons.AutoMirrored.Filled.Assignment, Screen.IncidentQueue.route),
                        Triple("Reports", Icons.Default.Assessment, Screen.OfficialReports.route),
                        Triple("Live Map", Icons.Default.LocationOn, Screen.MapView.route),
                        Triple("Alerts", Icons.Default.Notifications, Screen.OfficialAlerts.route),
                        Triple("Profile", Icons.Default.Person, Screen.Profile.route)
                    )
                } else if (uiState.officialRole.equals("field_inspector", ignoreCase = true)) {
                    listOf(
                        Triple("Dashboard", Icons.Default.Home, null as String?),
                        Triple("Tickets", Icons.AutoMirrored.Filled.Assignment, Screen.IncidentQueue.route),
                        Triple("Reports", Icons.Default.Assessment, Screen.OfficialReports.route),
                        Triple("Profile", Icons.Default.Person, Screen.Profile.route)
                    )
                } else if (uiState.officialRole.equals("worker", ignoreCase = true)) {
                    listOf(
                        Triple("Dashboard", Icons.Default.Home, null as String?),
                        Triple("Tickets", Icons.AutoMirrored.Filled.Assignment, Screen.IncidentQueue.route),
                        Triple("Reports", Icons.Default.Assessment, Screen.OfficialReports.route),
                        Triple("Profile", Icons.Default.Person, Screen.Profile.route)
                    )
                } else {
                    listOf(
                        Triple("Home", Icons.Default.Home, null as String?),
                        Triple("Tickets", Icons.AutoMirrored.Filled.Assignment, Screen.IncidentQueue.route),
                        Triple("Reports", Icons.Default.Assessment, Screen.OfficialReports.route),
                        Triple("Team", Icons.Default.Group, Screen.TeamManagement.route),
                        Triple("Live Map", Icons.Default.Map, Screen.MapView.route),
                        Triple("Analytics", Icons.Default.BarChart, Screen.OfficialAnalytics.route),
                        Triple("Alerts", Icons.Default.Notifications, Screen.OfficialAlerts.route),
                        Triple("Profile", Icons.Default.Person, Screen.Profile.route)
                    )
                }
                
                navItems.forEach { (label, icon, route) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
                        selected = route == null,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (route != null) navController.navigate(route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.safelive.app.R.drawable.safelive_logo),
                            contentDescription = "SafeLive Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "${uiState.displayRole} Portal", 
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Welcome, ${uiState.officialName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (uiState.unreadCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) { Text(uiState.unreadCount.toString(), color = Color.White) }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OfficialStatsSection(stats = uiState.stats, isLoading = uiState.isLoading)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tickets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { navController.navigate(Screen.AssignedIncidents.route) }) {
                        Text("View All")
                    }
                }
            }

            val tickets = uiState.recentTickets
            if (tickets.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = "📋",
                        title = "No assignments yet",
                        message = "Check the incident queue for new reports"
                    )
                }
            } else {
                items(tickets, key = { it.id }) { ticket ->
                    OfficialTicketCard(
                        ticket = ticket,
                        onClick = { navController.navigate(Screen.TicketDetail.createRoute(ticket.ticketId ?: ticket.id)) }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun OfficialStatsSection(stats: com.safelive.app.domain.model.TicketStats?, isLoading: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatKpiCard("Total", stats?.totalTickets ?: 0, com.safelive.app.ui.theme.PrimaryTeal, isLoading, Icons.Default.Dashboard, Modifier.weight(1f))
            StatKpiCard("Open", stats?.openTickets ?: 0, com.safelive.app.ui.theme.StatusOpen, isLoading, Icons.Default.Inbox, Modifier.weight(1f))
            StatKpiCard("Resolved", stats?.resolvedToday ?: 0, com.safelive.app.ui.theme.StatusResolved, isLoading, Icons.Default.CheckCircle, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatKpiCard("Pending", stats?.pendingTickets ?: 0, com.safelive.app.ui.theme.StatusPending, isLoading, Icons.Default.PendingActions, Modifier.weight(1f))
            StatKpiCard("In Progress", stats?.inProgress ?: 0, com.safelive.app.ui.theme.StatusInProgress, isLoading, Icons.Default.PlayCircle, Modifier.weight(1f))
        }
    }
}


@Composable
private fun StatKpiCard(label: String, count: Int, color: androidx.compose.ui.graphics.Color, isLoading: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = color)
            } else {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OfficialIncidentCard(incident: Incident, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = incident.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                com.safelive.app.presentation.dashboard.StatusChip(incident.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(incident.priority.toPriorityBorderColor(), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        incident.priority ?: "Medium",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        incident.category ?: "Unassigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    DateUtils.getTimeAgo(incident.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            incident.reportedBy?.let { reporterName ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reporterName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun String?.toPriorityBorderColor() = when (this?.lowercase()) {
    "critical" -> PriorityCritical
    "high" -> PriorityHigh
    "medium" -> PriorityMedium
    else -> PriorityLow
}

@Composable
fun OfficialTicketCard(ticket: Ticket, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = ticket.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                com.safelive.app.presentation.dashboard.StatusChip(ticket.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ticket.priority.toPriorityBorderColor(), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        ticket.priority,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    DateUtils.getTimeAgo(ticket.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentQueueScreen(
    navController: NavController,
    viewModel: IncidentQueueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.incidents.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateCard(
                        icon = "🎉",
                        title = "No tickets found",
                        message = "There are no tickets available for your account right now"
                    )
                }
            } else {
                items(uiState.incidents, key = { it.id }) { incident ->
                    OfficialTicketCard(
                        ticket = incident,
                        onClick = { navController.navigate(Screen.TicketDetail.createRoute(incident.id)) }
                    )
                }
            }
        }
    }
}
