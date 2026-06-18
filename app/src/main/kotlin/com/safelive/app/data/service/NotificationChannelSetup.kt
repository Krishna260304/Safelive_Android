package com.safelive.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Centralised notification channel registration.
 * Call [createChannels] once from SafeLiveApplication.onCreate().
 */
object NotificationChannelSetup {

    const val CHANNEL_SOS = "sos_channel"
    const val CHANNEL_EMERGENCY = "emergency_channel"
    const val CHANNEL_ALERTS = "alerts_channel"
    const val CHANNEL_SYSTEM = "system_channel"
    const val CHANNEL_CHAT = "chat_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_SOS,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Critical SOS emergency alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 600)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority emergency notifications from backend AI"
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_ALERTS,
                "System Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General warnings and alerts"
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_SYSTEM,
                "System Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "App status and background sync notifications"
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ticket chat and citizen-official messages"
                enableVibration(true)
                setShowBadge(true)
            }
        )

        manager.createNotificationChannels(channels)
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
