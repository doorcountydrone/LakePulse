package com.lakepulse.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class DeviceLocationClient(
    private val context: Context,
) {
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getLocation(): UserLocation? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        lastKnown(manager)?.let { return it }

        return withTimeoutOrNull(8_000L) {
            requestCurrent(manager)
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): UserLocation? {
        val candidates = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        return candidates.maxByOrNull { it.time }?.toUserLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrent(manager: LocationManager): UserLocation? {
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            else -> return null
        }

        return suspendCancellableCoroutine { continuation ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location.toUserLocation())
                    }
                }
            }

            try {
                manager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper(),
                )
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                manager.removeUpdates(listener)
            }
        }
    }

    private fun Location.toUserLocation(): UserLocation =
        UserLocation(latitude = latitude, longitude = longitude)
}
