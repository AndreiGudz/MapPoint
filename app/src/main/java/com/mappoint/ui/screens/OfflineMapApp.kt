package com.mappoint.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mappoint.ui.screens.bluetooth.BluetoothScreen
import com.mappoint.ui.screens.map.MapScreen
import com.mappoint.ui.screens.map.MapViewModel
import com.mappoint.ui.screens.permission.PermissionScreen
import com.mappoint.utils.hasLocationPermission

// Определяем экраны приложения
sealed class Screen(val route: String) {
    object Permissions : Screen("permissions")
    object Map : Screen("map")
    object Bluetooth : Screen("bluetooth")
}

@Composable
fun OfflineMapApp(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    // Проверяем, есть ли разрешение на местоположение
    var hasPermission by rememberSaveable { mutableStateOf(hasLocationPermission(context)) }
    // Создаем общий ViewModel для карты
    val mapViewModel: MapViewModel = viewModel()

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = if (hasPermission) Screen.Map.route else Screen.Permissions.route
        ) {
            // Экран запроса разрешений
            composable(Screen.Permissions.route) {
                PermissionScreen(
                    onAllPermissionsGranted = {
                        hasPermission = true
                        navController.navigate(Screen.Map.route) {
                            popUpTo(Screen.Permissions.route) { inclusive = true }
                        }
                    }
                )
            }

            // Экран карты
            composable(Screen.Map.route) {
                MapScreen(
                    mapViewModel = mapViewModel,
                    onNavigateToBluetooth = {
                        navController.navigate(Screen.Bluetooth.route)
                    }
                )
            }

            // Экран для взаимодействия с ESP32 по Bluetooth
            composable(Screen.Bluetooth.route) {
                BluetoothScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGpsDataReceived = { latitude, longitude, title, description ->
                        val finalTitle = if (title.isNullOrBlank()) "From ESP32" else title
                        val finalDescription = description ?: ""
                        // Автоматически добавляем точку на карту при получении GPS данных
                        mapViewModel.addMarker(latitude, longitude, finalTitle, finalDescription)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}