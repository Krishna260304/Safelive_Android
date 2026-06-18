package com.safelive.app.data.remote.api

import com.safelive.app.data.remote.dto.*
import retrofit2.http.*

interface TicketApi {
    @GET("tickets/{id}")
    suspend fun getTicketById(@Path("id") id: String): ApiResponse<TicketDto>

    @GET("tickets")
    suspend fun getTickets(
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null,
        @Query("category") category: String? = null
    ): ApiResponse<List<TicketDto>>

    @PATCH("tickets/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): ApiResponse<TicketDto>

    @POST("tickets/{id}/assign")
    suspend fun assignTicket(
        @Path("id") id: String,
        @Body request: Map<String, Any>
    ): ApiResponse<TicketDto>

    @POST("tickets/{id}/progress-update")
    suspend fun updateProgress(
        @Path("id") id: String,
        @Body request: Map<String, Any>
    ): ApiResponse<TicketDto>

    @POST("tickets/{id}/assign-supervisor")
    suspend fun assignSupervisor(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): ApiResponse<TicketDto>

    @GET("tickets/{id}/logbook")
    suspend fun getLogbook(@Path("id") id: String): ApiResponse<List<LogbookEntryDto>>

    @GET("tickets/stats")
    suspend fun getStats(): ApiResponse<TicketStatsDto>
}
