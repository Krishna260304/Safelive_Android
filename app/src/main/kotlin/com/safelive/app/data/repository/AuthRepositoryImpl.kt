package com.safelive.app.data.repository

import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import com.safelive.app.data.remote.api.AuthApi
import com.safelive.app.domain.model.User
import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val dataStore: UserPreferencesDataStore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val request = mapOf("email" to email, "password" to password)
            val response = authApi.login(request)
            if (response.success && response.data != null) {
                dataStore.saveAuthToken(response.data.token)
                dataStore.saveUserProfile(response.data.user.toDomain())
                Resource.Success(response.data.user.toDomain())
            } else {
                Resource.Error(response.error ?: "Login failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        userType: String,
        address: String,
        pincode: String
    ): Resource<User> {
        return try {
            val request = mapOf(
                "name" to fullName,
                "fullName" to fullName,
                "email" to email,
                "phone" to mobile,
                "mobile" to mobile,
                "password" to password,
                "userType" to userType,
                "address" to address,
                "pincode" to pincode
            )
            val response = authApi.register(request)
            if (response.success && response.data != null) {
                dataStore.saveAuthToken(response.data.token)
                dataStore.saveUserProfile(response.data.user.toDomain())
                Resource.Success(
                    response.data.user.toDomain()
                )
            } else {
                Resource.Error(response.error ?: "Registration failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun forgotPassword(email: String): Resource<String> {
        return try {
            val response = authApi.forgotPassword(mapOf("email" to email))
            if (response.success) {
                Resource.Success("OTP sent to email")
            } else {
                Resource.Error(response.error ?: "Failed to send OTP")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun verifyOtp(email: String, otp: String): Resource<String> {
        return try {
            val response = authApi.verifyOtp(mapOf("email" to email, "otp" to otp))
            if (response.success && response.data != null) {
                dataStore.saveAuthToken(response.data.token)
                Resource.Success("OTP verified successfully")
            } else {
                Resource.Error(response.error ?: "OTP verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun resetPassword(email: String, otp: String, newPassword: String): Resource<String> {
        return try {
            val response = authApi.resetPassword(mapOf("email" to email, "otp" to otp, "newPassword" to newPassword))
            if (response.success) {
                Resource.Success("Password reset successfully")
            } else {
                Resource.Error(response.error ?: "Failed to reset password")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Resource<String> {
        return try {
            val response = authApi.changePassword(mapOf("currentPassword" to currentPassword, "newPassword" to newPassword))
            if (response.success) {
                Resource.Success("Password changed successfully")
            } else {
                Resource.Error(response.error ?: "Failed to change password")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun toggle2FA(enabled: Boolean): Resource<String> {
        return try {
            val response = authApi.toggle2FA(mapOf("enabled" to enabled))
            if (response.success) {
                Resource.Success(if (enabled) "2FA enabled" else "2FA disabled")
            } else {
                Resource.Error(response.error ?: "Failed to toggle 2FA")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun logout(): Resource<Unit> {
        return try {
            authApi.logout()
            dataStore.clearAuth()
            Resource.Success(Unit)
        } catch (e: Exception) {
            dataStore.clearAuth()
            Resource.Success(Unit)
        }
    }

    override suspend fun getMe(): Resource<User> {
        return Resource.Error("getMe not explicitly mapped in AuthApi; refer to ProfileApi")
    }

    override fun isLoggedIn(): Flow<Boolean> = dataStore.isLoggedIn

    override fun getUserType(): Flow<String?> = dataStore.userType

    override fun getUserId(): Flow<String?> = dataStore.userId

    override fun getUserName(): Flow<String?> = dataStore.userName
}
