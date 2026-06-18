package com.safelive.app.data.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safelive.app.MainActivity
import com.safelive.app.R
import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import com.safelive.app.data.remote.api.NotificationApi
import com.safelive.app.data.remote.dto.FcmTokenRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * SafeLive Firebase Cloud Messaging service.
 *
 * Handles:
 * 1. New FCM token → save locally + register with backend
 * 2. Incoming push messages → route to correct notification channel
 */
@AndroidEntryPoint
class SafeLiveFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var userPreferencesDataStore: UserPreferencesDataStore
    @Inject lateinit var notificationApi: NotificationApi

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed")
        serviceScope.launch {
            // Save locally
            userPreferencesDataStore.saveFcmToken(token)
            // Register with backend
            try {
                notificationApi.registerFcmToken(FcmTokenRequest(fcmToken = token))
                Timber.d("FCM token registered with backend")
            } catch (e: Exception) {
                Timber.e(e, "Failed to register FCM token with backend")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val notificationType = data["type"] ?: "system"
        val title = message.notification?.title ?: data["title"] ?: "SafeLive"
        val body = message.notification?.body ?: data["body"] ?: ""
        val referenceId = data["referenceId"] ?: data["reference_id"]

        Timber.d("FCM message received: type=$notificationType, title=$title")

        val channelId = when (notificationType) {
            "sos", "emergency_sos" -> NotificationChannelSetup.CHANNEL_SOS
            "emergency", "critical_alert", "warning" -> NotificationChannelSetup.CHANNEL_EMERGENCY
            "alert", "system_alert" -> NotificationChannelSetup.CHANNEL_ALERTS
            "chat", "message", "ticket_message" -> NotificationChannelSetup.CHANNEL_CHAT
            else -> NotificationChannelSetup.CHANNEL_SYSTEM
        }

        showNotification(
            channelId = channelId,
            title = title,
            body = body,
            notificationType = notificationType,
            referenceId = referenceId
        )
    }

    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        notificationType: String,
        referenceId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", notificationType)
            referenceId?.let { putExtra("reference_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = when (channelId) {
            NotificationChannelSetup.CHANNEL_SOS -> NotificationCompat.PRIORITY_MAX
            NotificationChannelSetup.CHANNEL_EMERGENCY -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (e: SecurityException) {
            Timber.w("Cannot show notification — POST_NOTIFICATIONS permission not granted")
        }
    }
}
