package com.mappoint.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToBluetooth: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var drawerMode by remember { mutableStateOf(DrawerMode.MANAGE) }

    // Состояния из ViewModel
    val center by mapViewModel.center.collectAsStateWithLifecycle()
    val zoomLevel by mapViewModel.zoomLevel.collectAsStateWithLifecycle()
    val frame by mapViewModel.frame.collectAsStateWithLifecycle()
    val markers by mapViewModel.markers.collectAsStateWithLifecycle()
    val selectedMarker by mapViewModel.selectedMarker.collectAsStateWithLifecycle()
    val isLoading by mapViewModel.isLoading.collectAsStateWithLifecycle()

    // Преобразование точек в формат для карты
    val markerDataList by remember(markers) {
        derivedStateOf {
            markers.map { point ->
                MarkerData(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    title = point.title
                )
            }
        }
    }

    // Функция открытия Drawer в нужном режиме
    val openDrawer: (DrawerMode) -> Unit = { mode ->
        drawerMode = mode
        coroutineScope.launch { drawerState.open() }
    }

    // Функция закрытия Drawer
    val closeDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.close() }
    }

    // Обработчик центрирования на текущем местоположении
    val onCurrentPosition: () -> Unit = {
        when {
            !hasLocationPermission(context) -> Toast.makeText(
                context,
                "Разрешение на получение местоположения не предоставлено",
                Toast.LENGTH_SHORT
            ).show()
            !mapViewModel.isLocationEnabled() -> Toast.makeText(
                context,
                "Определение местоположения не доступно",
                Toast.LENGTH_SHORT
            ).show()
            else -> mapViewModel.centerOnMyLocation()
        }
    }

    // Обработчик удаления выбранного маркера
    val onDeleteSelectedMarker: () -> Unit = {
        selectedMarker?.let { mapViewModel.removeMarker(it) }
    }

    // Обработчик центрирования на выбранном маркере
    val onCenterOnSelectedMarker: () -> Unit = {
        selectedMarker?.let { mapViewModel.centerOnMarker(it, 15.0) }
    }

    MapDrawer(
        drawerState = drawerState,
        mode = drawerMode,
        viewModel = mapViewModel,
        onCloseDrawer = closeDrawer
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Карта") },
                    navigationIcon = {
                        IconButton(onClick = { openDrawer(DrawerMode.MANAGE) }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Меню маркеров")
                        }
                    },
                    actions = {
                        // Кнопка центрирования на текущем местоположении
                        IconButton(onClick = onCurrentPosition) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Моё местоположение")
                        }

                        // Кнопка перехода в Bluetooth экран
                        IconButton(onClick = onNavigateToBluetooth) {
                            Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth")
                        }

                        // Кнопка удаления выбранного маркера (только если выбран)
                        if (selectedMarker != null) {
                            IconButton(onClick = onDeleteSelectedMarker) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить выбранную точку")
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    // Кнопка добавления новой точки
                    FloatingActionButton(
                        onClick = { openDrawer(DrawerMode.ADD) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = "Добавить точку")
                    }

                    // Кнопка центрирования на выбранной точке (только если выбран маркер)
                    if (selectedMarker != null) {
                        FloatingActionButton(
                            onClick = onCenterOnSelectedMarker,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Центрировать на точке")
                        }
                    }
                }
            }
        ) { innerPadding ->
            MapContent(
                modifier = Modifier.padding(innerPadding),
                center = center,
                zoomLevel = zoomLevel,
                frame = frame,
                markers = markerDataList,
                markerClickListener = mapViewModel.getMarkerClickListener(),
                isLoading = isLoading,
                selectedMarker = selectedMarker,
                context = context
            )
        }
    }
}

@Composable
private fun MapContent(
    modifier: Modifier = Modifier,
    center: GeoPoint,
    zoomLevel: Double,
    frame: Int,
    markers: List<MarkerData>,
    markerClickListener: Marker.OnMarkerClickListener,
    isLoading: Boolean,
    selectedMarker: MapPoint?,
    context: Context
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Карта
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = center,
            zoomLevel = zoomLevel,
            frame = frame,
            markers = markers,
            markerClickListener = markerClickListener,
        )

        // Индикатор загрузки
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Информация о выбранном маркере
        selectedMarker?.let { marker ->
            SelectedMarkerInfo(
                marker = marker,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // Атрибуция OpenStreetMap
        MapAttribution(
            modifier = Modifier.align(Alignment.BottomStart),
            context = context
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun SelectedMarkerInfo(
    marker: MapPoint,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .background(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = marker.title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black
        )
        Text(
            text = "Широта: ${String.format("%.6f", marker.latitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
        Text(
            text = "Долгота: ${String.format("%.6f", marker.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
        if (marker.description.isNotBlank()) {
            Text(
                text = marker.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun MapAttribution(
    modifier: Modifier = Modifier,
    context: Context
) {
    Text(
        text = "© OpenStreetMap contributors",
        modifier = modifier
            .padding(8.dp)
            .background(
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable { openUrl(context, "https://openstreetmap.org/copyright") },
        style = MaterialTheme.typography.labelSmall,
        color = Color.Black,
        fontSize = 10.sp
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
    }
}