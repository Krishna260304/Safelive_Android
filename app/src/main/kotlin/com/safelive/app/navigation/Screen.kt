package com.safelive.app.navigation

sealed class Screen(val route: String) {

    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object OtpVerification : Screen("otp_verification/{email}") {
        fun createRoute(email: String) = "otp_verification/$email"
    }
    data object ResetPassword : Screen("reset_password/{email}/{otp}") {
        fun createRoute(email: String, otp: String) = "reset_password/$email/$otp"
    }

    data object CitizenDashboard : Screen("citizen_dashboard")
    data object IncidentList : Screen("incident_list")
    data object IncidentDetail : Screen("incident_detail/{incidentId}") {
        fun createRoute(id: String) = "incident_detail/$id"
    }
    data object CreateIncident : Screen("create_incident")
    data object MapView : Screen("map_view")
    data object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object ChangePassword : Screen("change_password")
    data object NotificationSettings : Screen("notification_settings")

    data object OfficialDashboard : Screen("official_dashboard")
    data object IncidentQueue : Screen("incident_queue")
    data object AssignedIncidents : Screen("assigned_incidents")
    data object TeamManagement : Screen("team_management")
    data object OfficialAlerts : Screen("official_alerts")
}
