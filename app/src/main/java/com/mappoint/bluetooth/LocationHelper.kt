package com.mappoint.bluetooth

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings

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