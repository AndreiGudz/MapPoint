package com.mappoint.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// Проверка разрешений для местоположения (оба типа)
fun hasLocationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Для Android 10+ достаточно одного из разрешений
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Для Android 9 и ниже нужны оба разрешения
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}

// Проверка точного местоположения (только FINE)
fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

// Проверка разрешений для хранилища (с учетом разных версий Android)
fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ - не нужно явное разрешение для WRITE_EXTERNAL_STORAGE
        // для доступа к собственным файлам приложения
        Environment.isExternalStorageManager() ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Android 10 - READ_EXTERNAL_STORAGE достаточно для scoped storage
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// Функция для запроса разрешений на местоположение
@Composable
fun rememberLocationPermissionLauncher(
    onPermissionGranted: (Boolean) -> Unit, // Boolean = точное ли разрешение
    onPermissionDenied: () -> Unit = {}
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

    val isGranted = fineGranted || coarseGranted

    if (isGranted) {
        onPermissionGranted(fineGranted)
    } else {
        onPermissionDenied()
    }
}

// Функция для запроса разрешений на хранилище
@Composable
fun rememberStoragePermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val allGranted = permissions.all { it.value }

    if (allGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

// Проверка всех необходимых разрешений
fun hasAllPermissions(context: Context): Boolean {
    return hasLocationPermission(context) && hasStoragePermission(context)
}
