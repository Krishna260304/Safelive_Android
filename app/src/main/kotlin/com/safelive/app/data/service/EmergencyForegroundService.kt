package com.safelive.app.data.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.safelive.app.R
import timber.log.Timber

class EmergencyForegroundService : Service() {

    companion object {
        const val ACTION_START_SOS = "ACTION_START_SOS"
        const val ACTION_STOP_SOS = "ACTION_STOP_SOS"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SOS -> startSos()
            ACTION_STOP_SOS -> stopSos()
        }
        return START_STICKY
    }

    private fun startSos() {
        val notification = NotificationCompat.Builder(this, NotificationChannelSetup.CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("SOS Emergency Active")
            .setContentText("Your location is being shared with emergency contacts and officials.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
        Timber.d("Emergency SOS Foreground Service started")
        // TODO: Establish location tracking updates to WebSocket
    }

    private fun stopSos() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Timber.d("Emergency SOS Foreground Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
