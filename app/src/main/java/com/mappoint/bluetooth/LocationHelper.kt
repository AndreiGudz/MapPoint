package com.mappoint.bluetooth

import android.content.Context
import android.location.LocationManager

object LocationHelper {
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            locationManager.isLocationEnabled
        } catch (e: SecurityException) {
            false
        }
    }
}