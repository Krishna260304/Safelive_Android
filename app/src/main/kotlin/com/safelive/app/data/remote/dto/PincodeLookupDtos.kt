package com.safelive.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PincodeLookupResponseDto(
    val pincode: String?,
    val taluk: String?,
    val district: String?,
    val state: String?,
    val datasetCount: Int? = null,
    val datasetSource: String? = null
)
