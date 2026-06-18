package com.safelive.app.data.service

import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class LocationHealthEvent {
    object Healthy : LocationHealthEvent()
    object GpsDisabled : LocationHealthEvent()
    object PermissionDenied : LocationHealthEvent()
}

class LocationHealthMonitor(private val context: Context) {

    fun observeLocationHealth(): Flow<LocationHealthEvent> = flow {
        while (true) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (!hasFineLocation || !hasBackgroundLocation) {
                emit(LocationHealthEvent.PermissionDenied)
            } else if (!isGpsEnabled && !isNetworkEnabled) {
                emit(LocationHealthEvent.GpsDisabled)
            } else {
                emit(LocationHealthEvent.Healthy)
            }

            delay(30000) // Check every 30 seconds
        }
    }
}
