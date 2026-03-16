package com.mappoint.ui.screens.map

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mappoint.ui.components.MarkerData
import com.mappoint.ui.components.OsmMapView
import com.mappoint.utils.hasLocationPermission
import androidx.core.net.toUri

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToInput: () -> Unit = {}
) {
    // Получаем состояния из ViewModel
    val center by mapViewModel.center.collectAsStateWithLifecycle()
    val zoomLevel by mapViewModel.zoomLevel.collectAsStateWithLifecycle()
    val frame by mapViewModel.frame.collectAsStateWithLifecycle()
    val markers by mapViewModel.markers.collectAsStateWithLifecycle()
    val selectedMarker by mapViewModel.selectedMarker.collectAsStateWithLifecycle()
    val isLoading by mapViewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Функция для открытия ссылки в браузере
    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
        }
    }

    // Преобразуем MapPoint в MarkerData для компонента карты
    val markerDataList = markers.map { point ->
        MarkerData(
            latitude = point.latitude,
            longitude = point.longitude,
            title = point.title
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карта") },
                actions = {
                    // Кнопка для центрирования на текущем местоположении
                    IconButton(
                        onClick = {
                            if (hasLocationPermission(context)) {
                                if (mapViewModel.isLocationEnabled())
                                    mapViewModel.centerOnMyLocation()
                                else
                                    Toast.makeText(context,
                                        "Определение местоположения не доступно",
                                        Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context,
                                    "Разрешение на получения местоположения не предоставлено",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Мое местоположение")
                    }

                    // Кнопка удаления выбранной точки
                    if (selectedMarker != null) {
                        IconButton(
                            onClick = {
                                selectedMarker?.let { mapViewModel.removeMarker(it) }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить точку")
                        }
                    }

                    // Кнопка очистки всех маркеров
                    IconButton(onClick = { mapViewModel.clearAllMarkers() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить все")
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                // Кнопка добавления новой точки
                FloatingActionButton(
                    onClick = {
                        onNavigateToInput()
                        selectedMarker?.let {
                            mapViewModel.centerOnMarker(it, 15.0)
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = "Добавить точку")
                }

                // Кнопка центрирования на выбранной точке
                if (selectedMarker != null) {
                    FloatingActionButton(
                        onClick = {
                            selectedMarker?.let {
                                mapViewModel.centerOnMarker(it, 15.0)
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Центрировать на точке")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Основная карта
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                center = center,
                zoomLevel = zoomLevel,
                frame = frame,
                markers = markerDataList,
                markerClickListener = mapViewModel.getMarkerClickListener(),
            )

            // Индикатор загрузки
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Отображение информации о выбранной точке
            selectedMarker?.let { marker ->
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = marker.title,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Широта: ${String.format("%.6f", marker.latitude)}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Долгота: ${String.format("%.6f", marker.longitude)}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Атрибуция OpenStreetMap
            Text(
                text = "© OpenStreetMap contributors",
                modifier = Modifier
                    .align(Alignment.BottomStart)   // левый нижний угол, чтобы не перекрывать FAB
                    .padding(8.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clickable { openUrl("https://openstreetmap.org/copyright") },
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontSize = 10.sp
            )
        }
    }
}
