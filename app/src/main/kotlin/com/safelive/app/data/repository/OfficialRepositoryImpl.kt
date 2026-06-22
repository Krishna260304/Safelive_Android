package com.safelive.app.data.repository

import com.safelive.app.data.remote.api.TicketApi
import com.safelive.app.domain.model.Ticket
import com.safelive.app.domain.model.LogbookEntry
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

    override suspend fun getTicketById(id: String): Resource<Ticket> {
        return try {
            val response = ticketApi.getTicketById(id)
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Ticket not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
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

    override suspend fun assignWorkers(
        ticketId: String,
        workerId: String,
        assigneeName: String,
        assigneePhone: String?,
        assigneePhoto: String?,
        notes: String?
    ): Resource<Ticket> {
        return try {
            val request = mutableMapOf<String, Any>(
                "workerId" to workerId,
                "workerIds" to listOf(workerId),
                "assignedTo" to assigneeName,
                "assigneeName" to assigneeName
            )
            assigneePhone?.takeIf { it.isNotBlank() }?.let { request["assigneePhone"] = it }
            assigneePhoto?.takeIf { it.isNotBlank() }?.let { request["assigneePhoto"] = it }
            notes?.takeIf { it.isNotBlank() }?.let { request["notes"] = it }

            val response = ticketApi.assignTicket(ticketId, request)
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to assign worker")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun assignSupervisor(ticketId: String, supervisorId: String, notes: String?): Resource<Ticket> {
        return try {
            val request = mutableMapOf("supervisorId" to supervisorId)
            notes?.takeIf { it.isNotBlank() }?.let { request["notes"] = it }
            val response = ticketApi.assignSupervisor(ticketId, request)
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to assign supervisor")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun updateProgress(ticketId: String, updateText: String, editLastUpdate: Boolean?): Resource<Ticket> {
        return try {
            val request = mutableMapOf<String, Any>(
                "updateText" to updateText
            )
            editLastUpdate?.let { request["editLastUpdate"] = it }
            val response = ticketApi.updateProgress(ticketId, request)
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to update progress")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun getTicketLogbook(ticketId: String): Resource<List<LogbookEntry>> {
        return try {
            val response = ticketApi.getLogbook(ticketId)
            if (response.success) {
                Resource.Success(response.data.orEmpty().map { it.toDomain() })
            } else {
                Resource.Error(response.error ?: "Failed to load logbook")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun rejectIncident(id: String, reason: String): Resource<Ticket> {
        return try {
            val request = mutableMapOf("status" to "rejected")
            reason.takeIf { it.isNotBlank() }?.let {
                request["note"] = it
                request["notes"] = it
                request["reason"] = it
                request["message"] = it
            }
            val response = ticketApi.updateStatus(id, request)
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
            val request = mutableMapOf("status" to status)
            note?.takeIf { it.isNotBlank() }?.let {
                request["note"] = it
                request["notes"] = it
                request["reason"] = it
                request["message"] = it
            }
            val response = ticketApi.updateStatus(id, request)
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
            val response = ticketApi.getTickets()
            if (response.success && response.data != null) {
                val actionableStatuses = setOf(
                    "open",
                    "pending",
                    "verified",
                    "assigned",
                    "in_progress",
                    "inspection pending",
                    "work assigned",
                    "awaiting verification",
                    "reopened"
                )
                val tickets = response.data
                    .map { it.toDomain() }
                    .filter { ticket ->
                        val normalizedStatus = ticket.status.lowercase().replace('_', ' ')
                        normalizedStatus !in setOf("resolved", "closed", "rejected") &&
                            (normalizedStatus in actionableStatuses || ticket.assignedTo.isNullOrBlank().not() || ticket.workerId != null || ticket.workerIds.orEmpty().isNotEmpty())
                    }
                emit(Resource.Success(tickets))
            } else {
                emit(Resource.Error(response.error ?: "Failed to load ticket queue"))
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
