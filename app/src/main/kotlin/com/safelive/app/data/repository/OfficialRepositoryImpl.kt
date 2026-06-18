package com.safelive.app.data.repository

import com.safelive.app.data.remote.api.TicketApi
import com.safelive.app.domain.model.Ticket
import com.safelive.app.domain.repository.OfficialRepository
import com.safelive.app.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class OfficialRepositoryImpl @Inject constructor(
    private val ticketApi: TicketApi
) : OfficialRepository {

    override fun getAssignedIncidents(
        page: Int,
        status: String?,
        priority: String?
    ): Flow<Resource<List<Ticket>>> = flow {
        emit(Resource.Loading)
        try {
            val response = ticketApi.getTickets(status = status, priority = priority)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.error ?: "Failed to load assigned tickets"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun acceptIncident(id: String): Resource<Ticket> {
        return try {
            val response = ticketApi.updateStatus(id, mapOf("status" to "in_progress"))
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to accept ticket")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun rejectIncident(id: String, reason: String): Resource<Ticket> {
        return try {
            val response = ticketApi.updateStatus(id, mapOf("status" to "rejected", "note" to reason, "notes" to reason, "reason" to reason))
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to reject ticket")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun updateStatus(id: String, status: String, note: String?): Resource<Ticket> {
        return try {
            val map = mutableMapOf("status" to status)
            if (note != null) {
                map["note"] = note
                map["notes"] = note
                map["reason"] = note
            }
            val response = ticketApi.updateStatus(id, map)
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to update status")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override fun getIncidentQueue(): Flow<Resource<List<Ticket>>> = flow {
        emit(Resource.Loading)
        try {
            val response = ticketApi.getTickets(status = "pending")
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.error ?: "Failed to load pending queue"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun uploadResolution(id: String, note: String, imagePaths: List<String>): Resource<Ticket> {
        return try {
            val response = ticketApi.updateProgress(id, mapOf("updateText" to note, "note" to note, "notes" to note, "reason" to note))
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to upload resolution")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
