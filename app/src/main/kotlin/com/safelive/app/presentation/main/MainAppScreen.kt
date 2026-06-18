package com.safelive.app.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import com.safelive.app.navigation.SafeLiveNavGraph
import com.safelive.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safelive.app.data.websocket.WebSocketManager

@HiltViewModel
class MainAppViewModel @Inject constructor(
    userPreferencesDataStore: UserPreferencesDataStore,
    private val webSocketManager: WebSocketManager
) : ViewModel() {
    val isOfficial: StateFlow<Boolean> = userPreferencesDataStore.userType
        .map { it?.lowercase() == "official" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val socketEvents = webSocketManager.socketEvents
}

sealed class BottomNavItem(val title: String, val icon: ImageVector, val route: String) {
    object Home : BottomNavItem("Home", Icons.Default.Home, Screen.CitizenDashboard.route)
    object Reports : BottomNavItem("Reports", Icons.Default.ListAlt, Screen.IncidentList.route)
    object Map : BottomNavItem("Map", Icons.Default.Map, Screen.MapView.route)
    object Profile : BottomNavItem("Profile", Icons.Default.Person, Screen.Profile.route)
}

@Composable
fun MainAppScreen(
    viewModel: MainAppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isOfficial by viewModel.isOfficial.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarRoutes = listOf(
        Screen.CitizenDashboard.route,
        Screen.IncidentList.route,
        Screen.MapView.route,
        Screen.Profile.route
    )

    // Only show global bottom bar for citizens on primary routes
    val showBottomBar = !isOfficial && bottomBarRoutes.any { currentDestination?.route?.startsWith(it) == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        BottomNavItem.Home,
                        BottomNavItem.Reports,
                        BottomNavItem.Map,
                        BottomNavItem.Profile
                    )

                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else innerPadding.calculateBottomPadding())
        ) {
            SafeLiveNavGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )

            WarningPopupHost(
                socketEvents = viewModel.socketEvents,
                onNavigateTo = { route -> navController.navigate(route) }
            )
        }
    }
}
