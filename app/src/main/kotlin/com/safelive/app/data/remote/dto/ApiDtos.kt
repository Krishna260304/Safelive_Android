package com.safelive.app.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.safelive.app.domain.model.*

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val error: String?
)

data class LoginResponse(
    val token: String? = null,
    val user: UserDto? = null,
    val requiresOtp: Boolean = false,
    val challengeId: String? = null,
    val channels: List<String>? = null,
    val maskedEmail: String? = null,
    val maskedPhone: String? = null
)

data class RegisterResponse(
    val token: String,
    val user: UserDto
)

data class OtpChallengeDto(
    val challengeId: String,
    val channels: List<String>? = null
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
    val profilePictureUrl: String? = null,
    val twoFactorEnabled: Boolean = false
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
        profilePictureUrl = profilePictureUrl,
        twoFactorEnabled = twoFactorEnabled
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
    val hasMessages: Boolean?,
    val progressPercent: Int?,
    val workerIds: List<String>?
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
        hasMessages = hasMessages,
        progressPercent = progressPercent,
        workerIds = workerIds
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
    val images: List<String>?,
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
    val reopenedBy: ReopenedByDto?,
    val reopenWarning: ReopenWarningDto?,
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
        images = images,
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
        reopenedBy = reopenedBy?.toDomain(),
        reopenWarning = reopenWarning?.toDomain(),
        resolvedById = resolvedById,
        resolvedByName = resolvedByName,
        resolvedAt = resolvedAt,
        verifiedAt = verifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

data class ReopenedByDto(
    val id: String? = null,
    val name: String? = null,
    val timestamp: String? = null
) {
    fun toDomain(): TicketReopenedBy = TicketReopenedBy(
        id = id,
        name = name,
        timestamp = timestamp
    )
}

data class ReopenWarningDto(
    val message: String,
    val issuedAt: String,
    val supervisorName: String? = null,
    val departmentName: String? = null
) {
    fun toDomain(): TicketReopenWarning = TicketReopenWarning(
        message = message,
        issuedAt = issuedAt,
        supervisorName = supervisorName,
        departmentName = departmentName
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
    val incidents: AnalyticsStatusDto = AnalyticsStatusDto(),
    val tickets: AnalyticsStatusDto = AnalyticsStatusDto(),
    val cityCleanlinessScore: Double = 0.0,
    val safetyIndex: Double = 0.0,
    val byCategory: List<AnalyticsCategoryDto> = emptyList(),
    val workerProductivity: List<WorkerProductivityDto> = emptyList(),
    val avgResolutionHours: Double = 0.0,
    val recentTrend: List<DailyCountDto> = emptyList()
) {
    // Compatibility accessors keep the existing summary components small while
    // decoding the nested shape returned by the webapp API.
    val totalIssues: Int get() = incidents.total
    val resolvedIssues: Int get() = incidents.resolved
    val pendingIssues: Int get() = incidents.pending
    val inProgressIssues: Int get() = incidents.inProgress
    val openIssues: Int get() = incidents.open
    val resolutionRate: Double get() = incidents.resolutionRate
    val resolutionRatePercent: Double get() = if (resolutionRate <= 1.0) resolutionRate * 100 else resolutionRate
    val categoryBreakdown: Map<String, Int> get() = byCategory.associate { it.category to it.count }
    val priorityBreakdown: Map<String, Int> get() = emptyMap()
    val statusBreakdown: Map<String, Int>
        get() = mapOf(
            "open" to incidents.open,
            "pending" to incidents.pending,
            "in_progress" to incidents.inProgress,
            "resolved" to incidents.resolved
        ).filterValues { it > 0 }
}

data class AnalyticsStatusDto(
    val total: Int = 0,
    val open: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val resolved: Int = 0,
    val resolutionRate: Double = 0.0
)

data class AnalyticsCategoryDto(
    val category: String = "",
    val count: Int = 0
)

data class WorkerProductivityDto(
    val worker: String = "",
    val total: Int = 0,
    val resolved: Int = 0,
    val open: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val resolutionRate: Double = 0.0
)

data class DailyCountDto(
    val date: String = "",
    val count: Int = 0,
    val created: Int = 0,
    val resolved: Int = 0
)

data class HeatmapPointDto(
    val lat: Double,
    val lng: Double,
    val weight: Double? = null,
    val category: String? = null,
    val status: String? = null
)

/** Request body for FCM token registration */
data class FcmTokenRequest(
    val fcmToken: String,
    val platform: String = "android"
)

data class LogbookEntryDto(
    val id: String? = null,
    val action: String? = null,
    val title: String? = null,
    val message: JsonElement? = null,
    val note: JsonElement? = null,
    val notes: JsonElement? = null,
    val details: JsonElement? = null,
    val description: JsonElement? = null,
    val statusFrom: String? = null,
    val statusTo: String? = null,
    val actorName: String? = null,
    val actorRole: String? = null,
    val createdAt: String? = null,
    val timestamp: String? = null,
    val updatedAt: String? = null
) {
    fun toDomain(): LogbookEntry = LogbookEntry(
        id = id,
        action = action ?: title,
        message = message.asFlexibleText()
            ?: note.asFlexibleText()
            ?: notes.asFlexibleText()
            ?: details.asFlexibleText()
            ?: description.asFlexibleText(),
        statusFrom = statusFrom,
        statusTo = statusTo,
        actorName = actorName,
        actorRole = actorRole,
        createdAt = createdAt ?: timestamp ?: updatedAt
    )
}

fun JsonElement?.asFlexibleText(): String? {
    if (this == null || isJsonNull) return null

    if (isJsonPrimitive) {
        val primitive = asJsonPrimitive
        return when {
            primitive.isString -> primitive.asString
            primitive.isBoolean -> primitive.asBoolean.toString()
            primitive.isNumber -> primitive.asNumber.toString()
            else -> primitive.toString()
        }
    }

    if (isJsonArray) {
        return asJsonArray.joinToString(", ") { element ->
            element.asFlexibleText().orEmpty()
        }.ifBlank { toString() }
    }

    if (isJsonObject) {
        val objectCandidates = listOf(
            "message",
            "note",
            "notes",
            "details",
            "description",
            "text",
            "title",
            "label",
            "value",
            "action",
            "summary"
        )
        for (key in objectCandidates) {
            val candidate = asJsonObject.get(key)?.asFlexibleText()
            if (!candidate.isNullOrBlank()) return candidate
        }

        val readablePairs = asJsonObject.entrySet().mapNotNull { (key, value) ->
            value.asFlexibleText()?.takeIf { it.isNotBlank() }?.let { "$key: $it" }
        }
        if (readablePairs.isNotEmpty()) {
            return readablePairs.joinToString(", ")
        }
    }

    return toString()
}
