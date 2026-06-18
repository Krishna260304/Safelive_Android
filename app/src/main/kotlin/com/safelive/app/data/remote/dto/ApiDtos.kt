package com.safelive.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.safelive.app.domain.model.*

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val error: String?
)

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class RegisterResponse(
    val token: String,
    val user: UserDto
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class UserDto(
    val id: String,
    @SerializedName(value = "email", alternate = ["emailAddress", "email_address", "userEmail"])
    val email: String,
    @SerializedName(value = "phone", alternate = ["mobile", "mobileNumber", "phoneNumber", "contactNumber"])
    val phone: String?,
    @SerializedName(value = "fullName", alternate = ["name", "fullname"])
    val fullName: String?,
    val userType: String,
    val officialRole: String?,
    val workerSpecialization: String?,
    val address: String?,
    val pincode: String?,
    val createdAt: String?,
    val isVerified: Boolean,
    val profilePictureUrl: String? = null
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        phone = phone,
        fullName = fullName.orEmpty(),
        userType = userType,
        officialRole = officialRole,
        workerSpecialization = workerSpecialization,
        address = address,
        pincode = pincode,
        createdAt = createdAt,
        isVerified = isVerified,
        profilePictureUrl = profilePictureUrl
    )
}

data class IncidentDto(
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
) {
    fun toDomain(): Incident = Incident(
        id = id,
        incidentId = incidentId,
        title = title,
        description = description,
        category = category,
        status = status,
        priority = priority,
        location = location,
        latitude = latitude,
        longitude = longitude,
        images = images,
        imageUrls = imageUrls,
        imageUrl = imageUrl,
        reportedBy = reportedBy,
        reporterId = reporterId,
        reporterEmail = reporterEmail,
        reporterPhone = reporterPhone,
        commonIncident = commonIncident,
        duplicateReportCount = duplicateReportCount,
        duplicateMatch = duplicateMatch,
        assignedTo = assignedTo,
        ticketId = ticketId,
        severity = severity,
        scope = scope,
        source = source,
        deviceId = deviceId,
        officialActionTaken = officialActionTaken,
        reporterDeleteLocked = reporterDeleteLocked,
        createdAt = createdAt,
        updatedAt = updatedAt,
        hasMessages = hasMessages
    )
}

data class TicketDto(
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
    val assignees: List<TicketAssigneeDto>?,
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
) {
    fun toDomain(): Ticket = Ticket(
        id = id,
        ticketId = ticketId,
        incidentId = incidentId,
        title = title,
        description = description,
        category = category,
        priority = priority,
        status = status,
        location = location,
        latitude = latitude,
        longitude = longitude,
        imageUrl = imageUrl,
        imageUrls = imageUrls,
        reportedBy = reportedBy,
        assignedTo = assignedTo,
        assigneeName = assigneeName,
        assigneePhone = assigneePhone,
        assigneePhotoUrl = assigneePhotoUrl,
        assigneeEmail = assigneeEmail,
        assigneeUserId = assigneeUserId,
        workerId = workerId,
        workerCode = workerCode,
        workerIds = workerIds,
        workerCodes = workerCodes,
        assignees = assignees?.map { it.toDomain() },
        workerSpecialization = workerSpecialization,
        workerSpecializations = workerSpecializations,
        fieldInspectorId = fieldInspectorId,
        fieldInspectorName = fieldInspectorName,
        progressPercent = progressPercent,
        progressSummary = progressSummary,
        progressSource = progressSource,
        progressConfidence = progressConfidence,
        progressUpdatedAt = progressUpdatedAt,
        lastInspectorUpdateAt = lastInspectorUpdateAt,
        lastWorkerUpdateAt = lastWorkerUpdateAt,
        reopenedSupervisorId = reopenedSupervisorId,
        reopenedSupervisorName = reopenedSupervisorName,
        resolvedById = resolvedById,
        resolvedByName = resolvedByName,
        resolvedAt = resolvedAt,
        verifiedAt = verifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

data class TicketAssigneeDto(
    val workerId: String,
    val workerCode: String?,
    val name: String,
    val phone: String?,
    val email: String?,
    val workerSpecialization: String?,
    val assignedAt: String?
) {
    fun toDomain(): TicketAssignee = TicketAssignee(
        workerId = workerId,
        workerCode = workerCode,
        name = name,
        phone = phone,
        email = email,
        workerSpecialization = workerSpecialization,
        assignedAt = assignedAt
    )
}

data class IncidentStatsDto(
    val total: Int,
    val open: Int,
    val inProgress: Int,
    val resolved: Int,
    val pending: Int
) {
    fun toDomain(): IncidentStats = IncidentStats(
        total = total,
        open = open,
        inProgress = inProgress,
        resolved = resolved,
        pending = pending
    )
}

data class TicketStatsDto(
    val totalTickets: Int,
    val openTickets: Int,
    val pendingTickets: Int?,
    val inProgress: Int,
    val resolvedToday: Int,
    val avgResponseTime: String,
    val resolutionRate: Double
) {
    fun toDomain(): TicketStats = TicketStats(
        totalTickets = totalTickets,
        openTickets = openTickets,
        pendingTickets = pendingTickets,
        inProgress = inProgress,
        resolvedToday = resolvedToday,
        avgResponseTime = avgResponseTime,
        resolutionRate = resolutionRate
    )
}

data class MessageDto(
    val id: String,
    val incidentId: String,
    val senderId: String,
    val senderName: String,
    val senderType: String,
    val content: String,
    val timestamp: String,
    val isRead: Boolean
) {
    fun toDomain(): Message = Message(
        id = id,
        incidentId = incidentId,
        senderId = senderId,
        senderName = senderName,
        senderType = senderType,
        content = content,
        timestamp = timestamp,
        isRead = isRead
    )
}

data class IncidentCreateRequest(
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val pincode: String? = null,
    val images: List<String>?
)

data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val referenceId: String?,
    val isRead: Boolean,
    val createdAt: String?
) {
    fun toDomain(): com.safelive.app.domain.model.Notification = com.safelive.app.domain.model.Notification(
        id = id,
        title = title,
        body = body,
        type = type,
        referenceId = referenceId,
        isRead = isRead,
        createdAt = createdAt
    )
}

/** Typed analytics data from GET analytics/dashboard */
data class DashboardDataDto(
    val totalIssues: Int = 0,
    val resolvedIssues: Int = 0,
    val pendingIssues: Int = 0,
    val inProgressIssues: Int = 0,
    val openIssues: Int = 0,
    val resolutionRate: Double = 0.0,
    val avgResolutionHours: Double = 0.0,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val priorityBreakdown: Map<String, Int> = emptyMap(),
    val statusBreakdown: Map<String, Int> = emptyMap(),
    val recentTrend: List<DailyCountDto> = emptyList()
)

data class DailyCountDto(
    val date: String = "",
    val count: Int = 0
)

/** Request body for FCM token registration */
data class FcmTokenRequest(
    val fcmToken: String,
    val platform: String = "android"
)
