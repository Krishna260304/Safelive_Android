package com.safelive.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.safelive.app.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safelive_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey(Constants.PREF_ACCESS_TOKEN)
        val REFRESH_TOKEN = stringPreferencesKey(Constants.PREF_REFRESH_TOKEN)
        val USER_ID = stringPreferencesKey(Constants.PREF_USER_ID)
        val USER_TYPE = stringPreferencesKey(Constants.PREF_USER_TYPE)
        val OFFICIAL_ROLE = stringPreferencesKey("official_role")
        val USER_NAME = stringPreferencesKey(Constants.PREF_USER_NAME)
        val USER_EMAIL = stringPreferencesKey(Constants.PREF_USER_EMAIL)
        val IS_LOGGED_IN = booleanPreferencesKey(Constants.PREF_IS_LOGGED_IN)
        val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
        val EMAIL_ALERTS = booleanPreferencesKey("email_alerts")
    }

    private val dataStore = context.dataStore

    val accessToken: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.ACCESS_TOKEN] }

    val refreshToken: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.REFRESH_TOKEN] }

    val userId: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_ID] }

    val userType: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_TYPE] }

    val officialRole: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.OFFICIAL_ROLE] }

    val userName: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_NAME] }

    val userEmail: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_EMAIL] }

    val isLoggedIn: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_LOGGED_IN] ?: false }

    val pushNotificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PUSH_NOTIFICATIONS] ?: true }

    val emailAlertsEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.EMAIL_ALERTS] ?: true }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveAuthToken(accessToken: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    suspend fun saveUserType(userType: String) {
        dataStore.edit { prefs -> prefs[Keys.USER_TYPE] = userType }
    }

    suspend fun saveOfficialRole(officialRole: String) {
        dataStore.edit { prefs -> prefs[Keys.OFFICIAL_ROLE] = officialRole }
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { prefs -> prefs[Keys.USER_ID] = userId }
    }

    suspend fun saveUserName(userName: String) {
        dataStore.edit { prefs -> prefs[Keys.USER_NAME] = userName }
    }

    suspend fun clearAuth() = clearSession()

    suspend fun saveUserSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userType: String,
        userName: String,
        userEmail: String
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_TYPE] = userType
            prefs[Keys.USER_NAME] = userName
            prefs[Keys.USER_EMAIL] = userEmail
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        try {
            dataStore.edit { it.clear() }
        } catch (e: IOException) {
            Timber.e(e, "Failed to clear session")
        }
    }

    suspend fun updateUserProfile(name: String, email: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_EMAIL] = email
        }
    }

    suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PUSH_NOTIFICATIONS] = enabled }
    }

    suspend fun setEmailAlertsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.EMAIL_ALERTS] = enabled }
    }
}
