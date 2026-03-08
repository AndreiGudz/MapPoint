package com.mappoint.utils

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Получение последнего известного местоположения
     * @return Location или null если недоступно
     */
    suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission(context)) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                continuation.resume(location)
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
            .addOnCanceledListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
    }

    /**
     * Получение одноразового текущего местоположения
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission(context)) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                )
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener { exception ->
                        // Если не получилось, пробуем получить последнее известное
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLocation ->
                                continuation.resume(lastLocation)
                            }
                            .addOnFailureListener { e ->
                                continuation.resume(null)
                            }
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }

    /**
     * Поток для получения обновлений местоположения в реальном времени
     * @param intervalMillis интервал обновлений в миллисекундах
     */
    fun getLocationUpdates(intervalMillis: Long = 5000L): Flow<Location> = callbackFlow {
        // Проверяем разрешения
        if (!hasLocationPermission(context)) {
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        )
            .setMinUpdateIntervalMillis(1000L)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.locations.lastOrNull()?.let { location ->
                    launch {
                        trySend(location)
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Проверка включен ли GPS (упрощенная проверка)
     */
    fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                    as android.location.LocationManager
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /**
         * Статический метод для быстрого получения местоположения
         */
        suspend fun getCurrentLocation(context: Context): Location? {
            return LocationService(context).getCurrentLocation()
        }
    }
}