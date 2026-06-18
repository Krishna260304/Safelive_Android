package com.safelive.app.data.remote.api

import com.safelive.app.data.remote.dto.*
import retrofit2.http.*

interface NotificationApi {
    @GET("notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationDto>>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<Map<String, Int>>

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): ApiResponse<Any>

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): ApiResponse<Any>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): ApiResponse<Any>

    @POST("notifications/register-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): ApiResponse<Any>
}
