package com.safelive.app.data.repository

import com.safelive.app.data.remote.api.ProfileApi
import com.safelive.app.domain.model.User
import com.safelive.app.domain.repository.ProfileRepository
import com.safelive.app.utils.Resource
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi
) : ProfileRepository {

    override suspend fun getProfile(): Resource<User> {
        return try {
            val response = profileApi.getProfile()
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to load profile")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun updateProfile(fullName: String?, mobile: String?, address: String?, pincode: String?): Resource<User> {
        return try {
            val map = mutableMapOf<String, String>()
            if (fullName != null) {
                map["fullName"] = fullName
                map["name"] = fullName
            }
            if (mobile != null) {
                map["phone"] = mobile
                map["mobile"] = mobile
            }
            if (address != null) map["address"] = address
            if (pincode != null) map["pincode"] = pincode
            val response = profileApi.updateProfile(map)
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to update profile")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun uploadProfilePicture(imagePath: String): Resource<User> {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return Resource.Error("Image file not found")
            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("image", file.name, requestBody)
            val response = profileApi.uploadProfilePicture(part)
            if (response.success && response.data != null) {
                Resource.Success(response.data.toDomain())
            } else {
                Resource.Error(response.error ?: "Failed to upload image")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
