package com.mappoint.utils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    private val gpsProvider = GpsMyLocationProvider(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    init {
        gpsProvider.locationUpdateMinTime = 5000        // 5 сек
        gpsProvider.locationUpdateMinDistance = 10.0f   // 10 метров
    }

    /**
     * Поток обновлений местоположения.
     * Запускается при начале сбора и останавливается при отмене корутины.
     */
    fun getLocationFlow(): Flow<Location> = callbackFlow {
        val listener = IMyLocationConsumer { location, source ->
            // Вызывается при каждом новом местоположении
            trySend(location)
        }
        // Запускаем провайдер с настройками
        gpsProvider.startLocationProvider(listener)
        // При закрытии канала останавливаем провайдер
        awaitClose {
            gpsProvider.stopLocationProvider()
        }
    }

    /**
     * Получение одного текущего местоположения (первое после запуска).
     */
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val listener = IMyLocationConsumer { location, source ->
            if (location != null) {
                continuation.resume(location)
            } else {
                continuation.resume(null)
            }
            // После получения останавливаем провайдер
            gpsProvider.stopLocationProvider()
        }

        gpsProvider.startLocationProvider(listener)

        // Если корутина отменена, останавливаем провайдер
        continuation.invokeOnCancellation {
            gpsProvider.stopLocationProvider()
        }
    }

    /**
     * Проверка, включена ли геолокация на устройстве.
     */
    fun isLocationEnabled(): Boolean {
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return locationManager.isLocationEnabled || gpsEnabled || networkEnabled
    }
}