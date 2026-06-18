package com.safelive.app.data.remote.interceptor

import com.google.gson.Gson
import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import com.safelive.app.data.remote.dto.RefreshTokenRequest
import com.safelive.app.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

class TokenRefreshInterceptor @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val gson: Gson
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        if (response.code == 401) {
            response.close()

            val refreshToken = runBlocking {
                userPreferencesDataStore.refreshToken.first()
            }

            if (!refreshToken.isNullOrBlank()) {
                val newTokens = runBlocking { refreshAccessToken(refreshToken) }
                if (newTokens != null) {
                    val (newAccessToken, newRefreshToken) = newTokens
                    runBlocking {
                        userPreferencesDataStore.saveTokens(newAccessToken, newRefreshToken)
                    }

                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                    return chain.proceed(newRequest)
                } else {

                    runBlocking {
                        userPreferencesDataStore.clearSession()
                    }
                }
            }
        }

        return response
    }

    private fun refreshAccessToken(refreshToken: String): Pair<String, String>? {
        return try {
            val client = OkHttpClient.Builder().build()
            val body = gson.toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${Constants.BASE_URL}auth/refresh-token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return null
                val map = gson.fromJson(json, Map::class.java)
                val data = map["data"] as? Map<*, *> ?: return null
                val accessToken = data["access_token"] as? String ?: return null
                val newRefreshToken = data["refresh_token"] as? String ?: return null
                Pair(accessToken, newRefreshToken)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh token")
            null
        }
    }
}
