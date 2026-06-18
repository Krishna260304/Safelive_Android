package com.safelive.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import com.safelive.app.data.remote.api.*
import com.safelive.app.data.remote.interceptor.AuthInterceptor
import com.safelive.app.data.remote.interceptor.TokenRefreshInterceptor
import com.safelive.app.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebSocketClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (com.safelive.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userPreferencesDataStore: UserPreferencesDataStore
    ): AuthInterceptor = AuthInterceptor(userPreferencesDataStore)

    @Provides
    @Singleton
    fun provideTokenRefreshInterceptor(
        userPreferencesDataStore: UserPreferencesDataStore,
        gson: Gson
    ): TokenRefreshInterceptor = TokenRefreshInterceptor(userPreferencesDataStore, gson)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshInterceptor: TokenRefreshInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * A clean OkHttpClient for WebSocket connections only.
     * Must NOT use AuthInterceptor (which adds Content-Type/Accept headers
     * that Cloudflare rejects during the HTTP 101 Upgrade handshake).
     * Token is passed via query param in the URL instead.
     */
    @WebSocketClient
    @Provides
    @Singleton
    fun provideWebSocketOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for long-lived WebSocket
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // WebSocketManager handles its own reconnect
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideIncidentApi(retrofit: Retrofit): IncidentApi = retrofit.create(IncidentApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi = retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideTicketApi(retrofit: Retrofit): TicketApi = retrofit.create(TicketApi::class.java)
}
