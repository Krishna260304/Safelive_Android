package com.safelive.app.data.remote.api

import com.safelive.app.data.remote.dto.*
import retrofit2.http.*

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): ApiResponse<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: Map<String, String>): ApiResponse<RegisterResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: Map<String, String>): ApiResponse<LoginResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: Map<String, String>): ApiResponse<Any>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: Map<String, String>): ApiResponse<Any>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Any>

    @POST("auth/password/change/request-otp")
    suspend fun requestChangePasswordOtp(@Body request: Map<String, String>): ApiResponse<Map<String, String>>

    @POST("auth/password/change/confirm")
    suspend fun confirmChangePassword(@Body request: Map<String, String>): ApiResponse<Any>

    @POST("auth/toggle-2fa")
    suspend fun toggle2FA(@Body request: Map<String, Boolean>): ApiResponse<Any>
}
