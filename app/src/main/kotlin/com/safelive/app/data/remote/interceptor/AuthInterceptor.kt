package com.safelive.app.data.remote.interceptor

import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            userPreferencesDataStore.accessToken.first()
        }

        val requestBuilder = chain.request().newBuilder()
            .header("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer ${token.trim()}")

            val method = chain.request().method
            // Don't force application/json on GET/DELETE or if it's already set (e.g. by @Multipart)
            if (method != "GET" && method != "DELETE" && chain.request().header("Content-Type") == null) {
                requestBuilder.header("Content-Type", "application/json")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
