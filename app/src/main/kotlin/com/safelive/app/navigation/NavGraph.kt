package com.safelive.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.safelive.app.presentation.auth.*
import com.safelive.app.presentation.chat.ChatScreen
import com.safelive.app.presentation.dashboard.CitizenDashboardScreen
import com.safelive.app.presentation.incidents.*
import com.safelive.app.presentation.maps.MapScreen
import com.safelive.app.presentation.notifications.NotificationScreen
import com.safelive.app.presentation.official.OfficialDashboardScreen
import com.safelive.app.presentation.official.IncidentQueueScreen
import com.safelive.app.presentation.official.TicketDetailScreen
import com.safelive.app.presentation.official.TeamManagementScreen
import com.safelive.app.presentation.official.OfficialAlertsScreen
import com.safelive.app.presentation.official.AssignedIncidentsScreen
import com.safelive.app.presentation.official.OfficialAnalyticsScreen
import com.safelive.app.presentation.profile.ProfileScreen
import com.safelive.app.presentation.profile.EditProfileScreen
import com.safelive.app.presentation.profile.ChangePasswordScreen
import com.safelive.app.presentation.profile.NotificationSettingsScreen
import com.safelive.app.presentation.splash.SplashScreen

@Composable
fun SafeLiveNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(
            route = Screen.Splash.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }

        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OtpVerificationScreen(navController = navController, email = email)
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            ResetPasswordScreen(navController = navController, token = token)
        }

        composable(Screen.CitizenDashboard.route) {
            CitizenDashboardScreen(navController = navController)
        }

        composable(Screen.IncidentList.route) {
            IncidentListScreen(navController = navController)
        }

        composable(
            route = Screen.IncidentDetail.route,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("incidentId") ?: ""
            IncidentDetailScreen(navController = navController, incidentId = id)
        }

        composable(Screen.CreateIncident.route) {
            CreateIncidentScreen(navController = navController)
        }

        composable(Screen.MapView.route) {
            MapScreen(navController = navController)
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(navController = navController, chatId = chatId)
        }

        composable(Screen.Notifications.route) {
            NotificationScreen(navController = navController)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(navController = navController)
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(navController = navController)
        }

        composable(Screen.OfficialDashboard.route) {
            OfficialDashboardScreen(navController = navController)
        }

        composable(Screen.IncidentQueue.route) {
            IncidentQueueScreen(navController = navController)
        }

        composable(
            route = Screen.TicketDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("ticketId") ?: ""
            TicketDetailScreen(navController = navController, ticketId = id)
        }

        composable(Screen.TeamManagement.route) {
            TeamManagementScreen(navController = navController)
        }

        composable(Screen.OfficialAlerts.route) {
            OfficialAlertsScreen(navController = navController)
        }

        composable(Screen.AssignedIncidents.route) {
            AssignedIncidentsScreen(navController = navController)
        }

        composable(Screen.OfficialAnalytics.route) {
            OfficialAnalyticsScreen(navController = navController)
        }

        composable(Screen.OfficialReports.route) {
            IncidentListScreen(navController = navController, officialMode = true)
        }
    }
}
