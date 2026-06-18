package com.safelive.app.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return suspendCancellableCoroutine { cont ->
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(Pair(location.latitude, location.longitude))
                } else {
                    cont.resume(null)
                }
            }.addOnFailureListener { e ->
                Timber.e(e, "Failed to get current location")
                cont.resume(null)
            }
            cont.invokeOnCancellation { cancellationTokenSource.cancel() }
        }
    }

    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        cont.resume(formatAddress(addresses.firstOrNull()))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddress(addresses?.firstOrNull())
            }
        } catch (e: Exception) {
            Timber.e(e, "Geocoding failed")
            "Lat: $latitude, Lng: $longitude"
        }
    }

    private fun formatAddress(address: Address?): String {
        if (address == null) return "Unknown address"
        val parts = mutableListOf<String>()
        for (i in 0..address.maxAddressLineIndex) {
            address.getAddressLine(i)?.let { parts.add(it) }
        }
        return parts.joinToString(", ").ifBlank { "Unknown address" }
    }
}
