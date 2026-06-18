package com.safelive.app.data.remote.api

import com.safelive.app.data.remote.dto.*
import retrofit2.http.*

interface ChatApi {
    @GET("incidents/{id}/messages")
    suspend fun getIncidentMessages(@Path("id") id: String): ApiResponse<List<MessageDto>>

    @POST("incidents/{id}/messages")
    suspend fun sendIncidentMessage(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): ApiResponse<MessageDto>
}
