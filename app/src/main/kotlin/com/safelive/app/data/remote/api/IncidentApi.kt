package com.safelive.app.data.remote.api

import com.safelive.app.data.remote.dto.*
import retrofit2.http.*

interface IncidentApi {
    @GET("issues")
    suspend fun getIncidents(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("priority") priority: String? = null,
        @Query("search") search: String? = null,
        @Query("sortBy") sortBy: String? = null
    ): ApiResponse<List<IncidentDto>>

    @GET("issues/{id}")
    suspend fun getIncidentById(@Path("id") id: String): ApiResponse<IncidentDto>

    @GET("issues/{id}/logbook")
    suspend fun getIncidentLogbook(@Path("id") id: String): ApiResponse<List<LogbookEntryDto>>

    @POST("issues")
    suspend fun createIncident(@Body request: IncidentCreateRequest): ApiResponse<IncidentDto>

    @PUT("issues/{id}")
    suspend fun updateIncident(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): ApiResponse<IncidentDto>

    @DELETE("issues/{id}")
    suspend fun deleteIncident(@Path("id") id: String): ApiResponse<Any>

    @GET("issues/stats")
    suspend fun getStats(): ApiResponse<IncidentStatsDto>

    @GET("analytics/dashboard")
    suspend fun getDashboardData(): ApiResponse<DashboardDataDto>

    @GET("analytics/trends")
    suspend fun getAnalyticsTrends(@Query("days") days: Int = 14): ApiResponse<List<DailyCountDto>>

    @GET("analytics/heatmap")
    suspend fun getHeatmap(): ApiResponse<List<HeatmapPointDto>>
}
