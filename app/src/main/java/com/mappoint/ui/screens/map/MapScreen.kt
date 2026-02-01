package com.mappoint.ui.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mappoint.ui.components.MarkerData
import com.mappoint.ui.components.OsmMapView
import com.mappoint.utils.hasLocationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToInput: () -> Unit = {}
) {
    // Получаем состояния из ViewModel
    val center by viewModel.center.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val markers by viewModel.markers.collectAsState()
    val selectedMarker by viewModel.selectedMarker.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val hasLocation = remember { hasLocationPermission(context) }

    // Преобразуем MapPoint в MarkerData для компонента карты
    val markerDataList = remember(markers) {
        markers.map { point ->
            MarkerData(
                latitude = point.latitude,
                longitude = point.longitude,
                title = point.title
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карта") },
                actions = {
                    IconButton(onClick = { viewModel.clearAllMarkers() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Очистить все")
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = onNavigateToInput,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = "Добавить точку")
                }

                if (selectedMarker != null) {
                    FloatingActionButton(
                        onClick = {
                            selectedMarker?.let {
                                viewModel.setCenter(it.latitude, it.longitude)
                                viewModel.setZoom(15.0)
                            }
                        }
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Центрировать")
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
                markers = markerDataList,
                onMapReady = { mapView ->
                    // Можно настроить дополнительные параметры карты
                    // например, при загрузке карты
                }
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
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Широта: ${marker.latitude}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Долгота: ${marker.longitude}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}