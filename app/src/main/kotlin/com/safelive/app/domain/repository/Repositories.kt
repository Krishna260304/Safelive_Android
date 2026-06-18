package com.safelive.app.domain.repository

import com.safelive.app.domain.model.*
import com.safelive.app.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun register(
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        userType: String,
        address: String,
        pincode: String,
        officialRole: String? = null,
        workerSpecialization: String? = null
    ): Resource<User>
    suspend fun forgotPassword(email: String): Resource<String>
    suspend fun verifyOtp(email: String, otp: String): Resource<String>
    suspend fun resetPassword(email: String, otp: String, newPassword: String): Resource<String>
    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<String>
    suspend fun toggle2FA(enabled: Boolean): Resource<String>
    suspend fun logout(): Resource<Unit>
    suspend fun getMe(): Resource<User>
    fun isLoggedIn(): Flow<Boolean>
    fun getUserType(): Flow<String?>
    fun getUserId(): Flow<String?>
    fun getUserName(): Flow<String?>
    fun getOfficialRole(): Flow<String?>
}

interface IncidentRepository {
    fun getIncidents(
        page: Int = 1,
        status: String? = null,
        category: String? = null,
        priority: String? = null,
        search: String? = null
    ): Flow<Resource<List<Incident>>>

    suspend fun getIncidentById(id: String): Resource<Incident>
    suspend fun createIncident(
        title: String,
        description: String,
        category: String,
        priority: String,
        latitude: Double?,
        longitude: Double?,
        location: String?,
        pincode: String?,
        imagePaths: List<String>
    ): Resource<Incident>

    suspend fun updateIncidentStatus(id: String, status: String, note: String?): Resource<Incident>
    suspend fun getDashboardStats(): Resource<DashboardStats>
    suspend fun getNearbyIncidents(latitude: Double, longitude: Double): Resource<List<Incident>>
    fun getCachedIncidents(): Flow<List<Incident>>
    suspend fun saveDraft(draft: DraftIncident): Long
    fun getDrafts(): Flow<List<DraftIncident>>
    suspend fun deleteDraft(id: Int)
}

interface ChatRepository {
    fun getMessages(incidentId: String): Flow<Resource<List<Message>>>
    suspend fun sendMessage(incidentId: String, content: String): Resource<Message>
}

interface NotificationRepository {
    fun getNotifications(): Flow<Resource<List<Notification>>>
    fun getUnreadCount(): Flow<Int>
    suspend fun markRead(id: String): Resource<Unit>
    suspend fun markAllRead(): Resource<Unit>
    suspend fun deleteNotification(id: String): Resource<Unit>
    suspend fun syncNotifications()
}

interface ProfileRepository {
    suspend fun getProfile(): Resource<User>
    suspend fun updateProfile(fullName: String?, mobile: String?, address: String?, pincode: String?): Resource<User>
    suspend fun uploadProfilePicture(imagePath: String): Resource<User>
    fun getCachedProfile(): Flow<User?>
}

interface PincodeRepository {
    suspend fun lookupPincode(pincode: String): Resource<String>
}

interface OfficialRepository {
    fun getAssignedIncidents(
        page: Int = 1,
        status: String? = null,
        priority: String? = null
    ): Flow<Resource<List<Ticket>>>

    suspend fun acceptIncident(id: String): Resource<Ticket>
    suspend fun rejectIncident(id: String, reason: String): Resource<Ticket>
    suspend fun updateStatus(id: String, status: String, note: String?): Resource<Ticket>
    fun getIncidentQueue(): Flow<Resource<List<Ticket>>>
    suspend fun uploadResolution(id: String, note: String, imagePaths: List<String>): Resource<Ticket>
}
