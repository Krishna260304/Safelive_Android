package com.safelive.app.domain.model

data class User(
    val id: String,
    val email: String,
    val phone: String?,
    val fullName: String,
    val userType: String,
    val officialRole: String?,
    val workerSpecialization: String?,
    val address: String?,
    val pincode: String?,
    val createdAt: String?,
    val isVerified: Boolean,
    val profilePictureUrl: String? = null
) {
    val isCitizen: Boolean get() = userType.lowercase() == "local"
    val isOfficial: Boolean get() = userType.lowercase() == "official"
    val displayRole: String
        get() = if (userType.equals("official", ignoreCase = true) && !officialRole.isNullOrBlank()) {
            officialRole.split("_", " ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else {
            userType.replaceFirstChar { it.uppercase() }
        }
}

data class Incident(
    val id: String,
    val incidentId: String?,
    val title: String,
    val description: String?,
    val category: String,
    val status: String,
    val priority: String?,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val images: List<String>?,
    val imageUrls: List<String>?,
    val imageUrl: String?,
    val reportedBy: String?,
    val reporterId: String?,
    val reporterEmail: String?,
    val reporterPhone: String?,
    val commonIncident: Boolean?,
    val duplicateReportCount: Int?,
    val duplicateMatch: Boolean?,
    val assignedTo: String?,
    val ticketId: String?,
    val severity: String?,
    val scope: String?,
    val source: String?,
    val deviceId: String?,
    val officialActionTaken: Boolean?,
    val reporterDeleteLocked: Boolean?,
    val createdAt: String,
    val updatedAt: String?,
    val hasMessages: Boolean?
)

data class Ticket(
    val id: String,
    val ticketId: String?,
    val incidentId: String?,
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val status: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val imageUrls: List<String>?,
    val reportedBy: String,
    val assignedTo: String?,
    val assigneeName: String?,
    val assigneePhone: String?,
    val assigneePhotoUrl: String?,
    val assigneeEmail: String?,
    val assigneeUserId: String?,
    val workerId: String?,
    val workerCode: String?,
    val workerIds: List<String>?,
    val workerCodes: List<String>?,
    val assignees: List<TicketAssignee>?,
    val workerSpecialization: String?,
    val workerSpecializations: List<String>?,
    val fieldInspectorId: String?,
    val fieldInspectorName: String?,
    val progressPercent: Int?,
    val progressSummary: String?,
    val progressSource: String?,
    val progressConfidence: Double?,
    val progressUpdatedAt: String?,
    val lastInspectorUpdateAt: String?,
    val lastWorkerUpdateAt: String?,
    val reopenedSupervisorId: String?,
    val reopenedSupervisorName: String?,
    val resolvedById: String?,
    val resolvedByName: String?,
    val resolvedAt: String?,
    val verifiedAt: String?,
    val createdAt: String,
    val updatedAt: String?
)

data class TicketAssignee(
    val workerId: String,
    val workerCode: String?,
    val name: String,
    val phone: String?,
    val email: String?,
    val workerSpecialization: String?,
    val assignedAt: String?
)

data class IncidentStats(
    val total: Int,
    val open: Int,
    val inProgress: Int,
    val resolved: Int,
    val pending: Int
)

data class TicketStats(
    val totalTickets: Int,
    val openTickets: Int,
    val pendingTickets: Int?,
    val inProgress: Int,
    val resolvedToday: Int,
    val avgResponseTime: String,
    val resolutionRate: Double
)

data class Message(
    val id: String,
    val incidentId: String,
    val senderId: String,
    val senderName: String,
    val senderType: String,
    val content: String,
    val timestamp: String,
    val isRead: Boolean
)

data class Notification(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val referenceId: String?,
    val isRead: Boolean,
    val createdAt: String?
)

data class DashboardStats(
    val stats: IncidentStats,
    val recentIncidents: List<Incident>
)

data class DraftIncident(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "Medium",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val pincode: String? = null,
    val localImagePaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
