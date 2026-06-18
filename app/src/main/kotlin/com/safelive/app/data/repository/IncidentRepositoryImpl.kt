package com.safelive.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.safelive.app.data.local.dao.DraftIncidentDao
import com.safelive.app.data.local.dao.IncidentDao
import com.safelive.app.data.local.entity.DraftIncidentEntity
import com.safelive.app.data.local.entity.IncidentEntity
import com.safelive.app.data.remote.api.IncidentApi
import com.safelive.app.data.remote.dto.IncidentCreateRequest
import com.safelive.app.domain.model.*
import com.safelive.app.domain.repository.IncidentRepository
import com.safelive.app.utils.Resource
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepositoryImpl @Inject constructor(
    private val incidentApi: IncidentApi,
    private val incidentDao: IncidentDao,
    private val draftIncidentDao: DraftIncidentDao,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : IncidentRepository {

    override fun getIncidents(
        page: Int, status: String?, category: String?,
        priority: String?, search: String?
    ): Flow<Resource<List<Incident>>> = flow {
        emit(Resource.Loading)
        try {
            val response = incidentApi.getIncidents(
                page = page, status = status, category = category,
                priority = priority, search = search
            )
            if (response.success && response.data != null) {
                val incidents = response.data.map { it.toDomain() }
                incidentDao.insertIncidents(incidents.map { it.toEntity() })
                emit(Resource.Success(incidents))
            } else {
                emit(Resource.Error(response.error ?: "Failed to load incidents"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun getIncidentById(id: String): Resource<Incident> {
        return try {
            val response = incidentApi.getIncidentById(id)
            if (response.success && response.data != null) {
                val incident = response.data.toDomain()
                incidentDao.insertIncident(incident.toEntity())
                Resource.Success(incident)
            } else {
                Resource.Error(response.error ?: "Incident not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun getIncidentLogbook(id: String): Resource<List<LogbookEntry>> {
        return try {
            val response = incidentApi.getIncidentLogbook(id)
            if (response.success) {
                Resource.Success(response.data.orEmpty().map { it.toDomain() })
            } else {
                Resource.Error(response.error ?: "Failed to load incident logbook")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun createIncident(
        title: String, description: String, category: String, priority: String,
        latitude: Double?, longitude: Double?, location: String?, pincode: String?, imagePaths: List<String>
    ): Resource<Incident> {
        return try {
            val encodedImages = imagePaths.mapNotNull { encodeImageToBase64(it) }
            if (imagePaths.isNotEmpty() && encodedImages.isEmpty()) {
                return Resource.Error("Failed to process incident images")
            }

            val resolvedLocation = buildString {
                append(location.orEmpty())
                if (!pincode.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append("Pincode: ")
                    append(pincode)
                }
            }.ifBlank { location.orEmpty() }

            val request = IncidentCreateRequest(
                title = title,
                description = description,
                category = category,
                priority = priority,
                location = resolvedLocation,
                latitude = latitude ?: 0.0,
                longitude = longitude ?: 0.0,
                pincode = null,
                images = encodedImages.takeIf { it.isNotEmpty() }
            )

            val response = incidentApi.createIncident(request)
            if (response.success && response.data != null) {
                val incident = response.data.toDomain()
                incidentDao.insertIncident(incident.toEntity())
                Resource.Success(incident)
            } else {
                Resource.Error(response.error ?: "Failed to create incident")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun updateIncidentStatus(id: String, status: String, note: String?): Resource<Incident> {
        return try {
            val map = mutableMapOf("status" to status)
            if (note != null) {
                map["note"] = note
                map["notes"] = note
                map["reason"] = note
            }
            val response = incidentApi.updateIncident(id, map)
            if (response.success && response.data != null) {
                val incident = response.data.toDomain()
                incidentDao.insertIncident(incident.toEntity())
                Resource.Success(incident)
            } else {
                Resource.Error(response.error ?: "Failed to update status")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun getDashboardStats(): Resource<DashboardStats> {
        return try {
            val responseStats = incidentApi.getStats()
            if (responseStats.success && responseStats.data != null) {
                val stats = responseStats.data.toDomain()
                val responseList = incidentApi.getIncidents(limit = 5)
                val recent = responseList.data?.map { it.toDomain() } ?: emptyList()
                Resource.Success(DashboardStats(stats = stats, recentIncidents = recent))
            } else {
                Resource.Error(responseStats.error ?: "Failed to load stats")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun getNearbyIncidents(latitude: Double, longitude: Double): Resource<List<Incident>> {
        return Resource.Error("API missing nearby incidents endpoint explicitly; use map filtering")
    }

    override fun getCachedIncidents(): Flow<List<Incident>> {
        return incidentDao.getAllIncidents().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveDraft(draft: DraftIncident): Long {
        return draftIncidentDao.insertDraft(draft.toEntity())
    }

    override fun getDrafts(): Flow<List<DraftIncident>> {
        return draftIncidentDao.getAllDrafts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deleteDraft(id: Int) = draftIncidentDao.deleteDraftById(id)

    private fun encodeImageToBase64(imagePath: String): String? {
        return try {
            val bytes = when {
                imagePath.startsWith("content://") -> {
                    val uri = Uri.parse(imagePath)
                    context.contentResolver.openInputStream(uri)?.use { inputStream -> inputStream.readBytes() }
                }
                imagePath.startsWith("file://") -> {
                    val uri = Uri.parse(imagePath)
                    uri.path?.let { path ->
                        val file = File(path)
                        if (file.exists()) file.readBytes() else null
                    }
                }
                else -> {
                    val file = File(imagePath)
                    if (file.exists()) {
                        file.readBytes()
                    } else {
                        context.contentResolver.openInputStream(Uri.parse(imagePath))?.use { inputStream ->
                            inputStream.readBytes()
                        }
                    }
                }
            }

            bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to encode incident image: %s", imagePath)
            null
        }
    }

    private fun Incident.toEntity() = IncidentEntity(
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
        imagesJson = gson.toJson(images),
        imageUrlsJson = gson.toJson(imageUrls),
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

    private fun IncidentEntity.toDomain(): Incident {
        val imagesList = try {
            gson.fromJson(imagesJson, Array<String>::class.java)?.toList()
        } catch (e: Exception) { null }

        val imageUrlsList = try {
            gson.fromJson(imageUrlsJson, Array<String>::class.java)?.toList()
        } catch (e: Exception) { null }

        return Incident(
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
            images = imagesList,
            imageUrls = imageUrlsList,
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
            progressPercent = null,
            workerIds = null
        )
    }

    private fun DraftIncident.toEntity() = DraftIncidentEntity(
        id = id, title = title, description = description, category = category,
        priority = priority, latitude = latitude, longitude = longitude,
        address = address, localImagePathsJson = gson.toJson(localImagePaths),
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun DraftIncidentEntity.toDomain(): DraftIncident {
        val paths = try {
            gson.fromJson(localImagePathsJson, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
        return DraftIncident(
            id = id, title = title, description = description, category = category,
            priority = priority, latitude = latitude, longitude = longitude,
            address = address, localImagePaths = paths,
            createdAt = createdAt, updatedAt = updatedAt
        )
    }

}
