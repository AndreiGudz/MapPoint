package com.mappoint.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

// Проверка разрешений на местоположение
fun hasLocationPermission(context: Context): Boolean {
    // Для Android 10+ проверяем оба разрешения
    return hasFineLocationPermission(context) || hasCoarseLocationPermission(context)
}

// Проверка точного местоположения (для Android 12+ может потребоваться)
fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

// Проверка приблизительного местоположения
fun hasCoarseLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

// Получение разрешения на местоположение
@Composable
fun rememberLocationPermissionLauncher(
    onPermissionGranted: (hasFineLocation: Boolean) -> Unit,
    onPermissionDenied: () -> Unit = {}
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

    when {
        fineGranted || coarseGranted -> onPermissionGranted(fineGranted)
        else -> onPermissionDenied()
    }
}

// Проверка, нужно ли показывать обоснование для разрешения
fun shouldShowLocationRationale(context: Context): Boolean {
    // TODO: Здесь можно реализовать логику для показа обоснования
    // если пользователь уже отказывал в разрешении
    return false
}

// Получение строки описания разрешений
fun getLocationPermissionDescription(context: Context): String {
    return "Приложению требуется доступ к вашему местоположению. " +
        "Вы можете выбрать точное или приблизительное местоположение."
}