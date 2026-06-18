package com.safelive.app.data.repository

import com.safelive.app.data.remote.api.PincodeApi
import com.safelive.app.domain.repository.PincodeRepository
import com.safelive.app.utils.Resource
import com.safelive.app.utils.ValidationUtils
import com.safelive.app.utils.toApiErrorMessage
import javax.inject.Inject

class PincodeRepositoryImpl @Inject constructor(
    private val pincodeApi: PincodeApi
) : PincodeRepository {

    override suspend fun lookupPincode(pincode: String): Resource<String> {
        val trimmedPincode = pincode.trim()
        if (!ValidationUtils.isValidPincode(trimmedPincode)) {
            return Resource.Error("Enter a valid 6-digit pincode")
        }

        return try {
            val response = pincodeApi.lookupPincode(trimmedPincode)
            val data = response.data
            if (response.success && data != null) {
                val locationParts = listOfNotNull(data.district?.trim(), data.state?.trim())
                    .filter { it.isNotBlank() }
                if (locationParts.isEmpty()) {
                    Resource.Success("Pincode found")
                } else {
                    Resource.Success(locationParts.joinToString(", "))
                }
            } else {
                Resource.Error(response.message ?: response.error ?: "Pincode not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.toApiErrorMessage("Unable to validate pincode"))
        }
    }
}