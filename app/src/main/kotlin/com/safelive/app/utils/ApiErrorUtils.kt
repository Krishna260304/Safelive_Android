package com.safelive.app.utils

import org.json.JSONObject
import retrofit2.HttpException

fun Throwable.toApiErrorMessage(defaultMessage: String = "Unknown error"): String {
    val httpException = this as? HttpException ?: return localizedMessage ?: defaultMessage
    val errorBody = httpException.response()?.errorBody()?.string().orEmpty().trim()
    if (errorBody.isBlank()) return localizedMessage ?: defaultMessage

    return runCatching {
        val json = JSONObject(errorBody)
        sequenceOf("detail", "message", "error")
            .mapNotNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull()
            ?: errorBody
    }.getOrElse { localizedMessage ?: defaultMessage }
}
