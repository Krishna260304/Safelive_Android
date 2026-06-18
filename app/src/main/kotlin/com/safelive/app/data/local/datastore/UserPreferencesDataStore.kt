package com.safelive.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.safelive.app.domain.model.User
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
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_ADDRESS = stringPreferencesKey("user_address")
        val USER_PINCODE = stringPreferencesKey("user_pincode")
        val USER_PROFILE_PICTURE_URL = stringPreferencesKey("user_profile_picture_url")
        val USER_WORKER_SPECIALIZATION = stringPreferencesKey("user_worker_specialization")
        val USER_CREATED_AT = stringPreferencesKey("user_created_at")
        val USER_IS_VERIFIED = booleanPreferencesKey("user_is_verified")
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

    val userPhone: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_PHONE] }

    val userAddress: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_ADDRESS] }

    val userPincode: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_PINCODE] }

    val userProfilePictureUrl: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_PROFILE_PICTURE_URL] }

    val userWorkerSpecialization: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_WORKER_SPECIALIZATION] }

    val userCreatedAt: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_CREATED_AT] }

    val userIsVerified: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_IS_VERIFIED] ?: false }

    val isLoggedIn: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_LOGGED_IN] ?: false }

    val cachedProfile: Flow<User?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val id = prefs[Keys.USER_ID].orEmpty().trim()
            val email = prefs[Keys.USER_EMAIL].orEmpty().trim()
            val fullName = prefs[Keys.USER_NAME].orEmpty().trim()
            val userType = prefs[Keys.USER_TYPE].orEmpty().trim()

            if (id.isBlank() && email.isBlank() && fullName.isBlank() && userType.isBlank()) {
                null
            } else {
                User(
                    id = id,
                    email = email,
                    phone = prefs[Keys.USER_PHONE],
                    fullName = fullName,
                    userType = userType.ifBlank { "official" },
                    officialRole = prefs[Keys.OFFICIAL_ROLE],
                    workerSpecialization = prefs[Keys.USER_WORKER_SPECIALIZATION],
                    address = prefs[Keys.USER_ADDRESS],
                    pincode = prefs[Keys.USER_PINCODE],
                    createdAt = prefs[Keys.USER_CREATED_AT],
                    isVerified = prefs[Keys.USER_IS_VERIFIED] ?: false,
                    profilePictureUrl = prefs[Keys.USER_PROFILE_PICTURE_URL]
                )
            }
        }

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

    suspend fun saveUserProfile(user: User) {
        dataStore.edit { prefs ->
            val existingEmail = prefs[Keys.USER_EMAIL].orEmpty().trim()
            val existingPhone = prefs[Keys.USER_PHONE].orEmpty().trim()
            val existingUserType = prefs[Keys.USER_TYPE].orEmpty().trim()
            val existingOfficialRole = prefs[Keys.OFFICIAL_ROLE].orEmpty().trim()
            val existingWorkerSpecialization = prefs[Keys.USER_WORKER_SPECIALIZATION].orEmpty().trim()
            val existingAddress = prefs[Keys.USER_ADDRESS].orEmpty().trim()
            val existingPincode = prefs[Keys.USER_PINCODE].orEmpty().trim()
            val existingCreatedAt = prefs[Keys.USER_CREATED_AT].orEmpty().trim()
            val existingProfilePictureUrl = prefs[Keys.USER_PROFILE_PICTURE_URL].orEmpty().trim()

            prefs[Keys.USER_ID] = user.id
            if (user.email.isNotBlank()) {
                prefs[Keys.USER_EMAIL] = user.email
            } else if (existingEmail.isBlank()) {
                prefs.remove(Keys.USER_EMAIL)
            }

            val resolvedPhone = user.phone?.trim().orEmpty()
            if (resolvedPhone.isNotBlank()) {
                prefs[Keys.USER_PHONE] = resolvedPhone
            } else if (existingPhone.isBlank()) {
                prefs.remove(Keys.USER_PHONE)
            }

            prefs[Keys.USER_NAME] = user.fullName
            if (user.userType.isNotBlank()) {
                prefs[Keys.USER_TYPE] = user.userType
            } else if (existingUserType.isBlank()) {
                prefs.remove(Keys.USER_TYPE)
            }

            if (!user.officialRole.isNullOrBlank()) {
                prefs[Keys.OFFICIAL_ROLE] = user.officialRole
            } else if (existingOfficialRole.isBlank()) {
                prefs.remove(Keys.OFFICIAL_ROLE)
            }

            if (!user.workerSpecialization.isNullOrBlank()) {
                prefs[Keys.USER_WORKER_SPECIALIZATION] = user.workerSpecialization
            } else if (existingWorkerSpecialization.isBlank()) {
                prefs.remove(Keys.USER_WORKER_SPECIALIZATION)
            }

            if (!user.address.isNullOrBlank()) {
                prefs[Keys.USER_ADDRESS] = user.address
            } else if (existingAddress.isBlank()) {
                prefs.remove(Keys.USER_ADDRESS)
            }

            if (!user.pincode.isNullOrBlank()) {
                prefs[Keys.USER_PINCODE] = user.pincode
            } else if (existingPincode.isBlank()) {
                prefs.remove(Keys.USER_PINCODE)
            }

            if (!user.createdAt.isNullOrBlank()) {
                prefs[Keys.USER_CREATED_AT] = user.createdAt
            } else if (existingCreatedAt.isBlank()) {
                prefs.remove(Keys.USER_CREATED_AT)
            }

            prefs[Keys.USER_IS_VERIFIED] = user.isVerified
            if (!user.profilePictureUrl.isNullOrBlank()) {
                prefs[Keys.USER_PROFILE_PICTURE_URL] = user.profilePictureUrl
            } else if (existingProfilePictureUrl.isBlank()) {
                prefs.remove(Keys.USER_PROFILE_PICTURE_URL)
            }
            prefs[Keys.IS_LOGGED_IN] = true
        }
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
