package com.safelive.app.data.remote.api

import com.safelive.app.data.remote.dto.*
import retrofit2.http.*
import okhttp3.MultipartBody

interface ProfileApi {
    @GET("users/profile")
    suspend fun getProfile(): ApiResponse<UserDto>

    @PUT("users/profile")
    suspend fun updateProfile(@Body request: Map<String, String>): ApiResponse<UserDto>

    @Multipart
    @PUT("users/profile/picture")
    suspend fun uploadProfilePicture(
        @Part image: MultipartBody.Part
    ): ApiResponse<UserDto>

    @GET("users/workers")
    suspend fun getWorkers(): ApiResponse<List<UserDto>>

    @GET("users/managed-officials")
    suspend fun getManagedOfficials(): ApiResponse<List<UserDto>>
}
