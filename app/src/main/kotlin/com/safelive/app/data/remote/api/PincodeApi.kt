package com.safelive.app.data.remote.api

import com.safelive.app.data.remote.dto.ApiResponse
import com.safelive.app.data.remote.dto.PincodeLookupResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface PincodeApi {
    @GET("public/pincode/{pincode}")
    suspend fun lookupPincode(@Path("pincode") pincode: String): ApiResponse<PincodeLookupResponseDto>
}