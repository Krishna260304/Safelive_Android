package com.safelive.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
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
    val isVerified: Boolean
)

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey
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
    val imagesJson: String?,
    val imageUrlsJson: String?,
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
    val isSynced: Boolean = true
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val incidentId: String,
    val senderId: String,
    val senderName: String,
    val senderType: String,
    val content: String,
    val timestamp: String,
    val isRead: Boolean,
    val isSent: Boolean = true
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val referenceId: String?,
    val isRead: Boolean = false,
    val createdAt: String?,
    val receivedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "draft_incidents")
data class DraftIncidentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "Medium",
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val localImagePathsJson: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
