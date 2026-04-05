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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteSweep

// Для верхней панели и кнопок поверх карты
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapScaffold(
    onMenuClick: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    selectedMarker: MapPoint?,
    onDeleteSelectedMarker: () -> Unit,
    onNavigateToInput: () -> Unit,
    onNavigateToBluetooth: () -> Unit,
    onCenterOnSelectedMarker: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карта") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Меню маркеров")
                    }
                },
                actions = {
                    IconButton(onClick = onCurrentLocationClick) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Мое местоположение")
                    }
                    IconButton(onClick = onNavigateToBluetooth) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth")
                    }
                    if (selectedMarker != null) {
                        IconButton(onClick = onDeleteSelectedMarker) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить точку")
                        }
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
                        onClick = onCenterOnSelectedMarker,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Центрировать на точке")
                    }
                }
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

private const val DEFAULT_ZOOM_ON_MARKER = 15.0

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToInput: () -> Unit = {},
    onNavigateToBluetooth: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Состояния из ViewModel
    val center by mapViewModel.center.collectAsStateWithLifecycle()
    val zoomLevel by mapViewModel.zoomLevel.collectAsStateWithLifecycle()
    val frame by mapViewModel.frame.collectAsStateWithLifecycle()
    val markers by mapViewModel.markers.collectAsStateWithLifecycle()
    val selectedMarker by mapViewModel.selectedMarker.collectAsStateWithLifecycle()
    val isLoading by mapViewModel.isLoading.collectAsStateWithLifecycle()

    // Преобразование точек в формат для карты (кешируем, чтобы не пересоздавать при каждой рекомпозиции)
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

    // Вспомогательная функция для закрытия drawer
    val closeDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.close() }
    }

    // Обработчики действий с маркерами
    val onCenterOnMarker: (MapPoint) -> Unit = { marker: MapPoint ->
        mapViewModel.centerOnMarker(marker, DEFAULT_ZOOM_ON_MARKER)
        closeDrawer()
    }

    val onDeleteMarker: (MapPoint) -> Unit = { marker: MapPoint ->
        mapViewModel.removeMarker(marker)
    }

    val onDeleteAllMarkers: () -> Unit = {
        mapViewModel.clearAllMarkers()
        closeDrawer()
    }

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

    MapDeleteDrawer(
        markers = markers,
        selectedMarker = selectedMarker,
        drawerState = drawerState,
        onMarkerClick = onCenterOnMarker,
        onDeleteMarker = onDeleteMarker,
        onDeleteAllMarkers = onDeleteAllMarkers,
        onCenterOnMarker = onCenterOnMarker,
        onCloseDrawer = closeDrawer
    ) {
        MapScaffold(
            onMenuClick = { coroutineScope.launch { drawerState.open() } },
            onCurrentLocationClick = onCurrentPosition,
            selectedMarker = selectedMarker,
            onDeleteSelectedMarker = { selectedMarker?.let(onDeleteMarker) },
            onNavigateToInput = onNavigateToInput,
            onNavigateToBluetooth = onNavigateToBluetooth,
            onCenterOnSelectedMarker = { selectedMarker?.let(onCenterOnMarker) },
            { innerPadding ->
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
            })
    }
}

@Composable
// Для карты
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
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = center,
            zoomLevel = zoomLevel,
            frame = frame,
            markers = markers,
            markerClickListener = markerClickListener,
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        selectedMarker?.let { marker ->
            SelectedMarkerInfo(marker = marker, modifier = Modifier.align(Alignment.TopStart))
        }

        MapAttribution(modifier = Modifier.align(Alignment.BottomStart), context = context)
    }
}

// Вывод информации о маркере
@Composable
private fun SelectedMarkerInfo(marker: MapPoint, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = marker.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black
        )
        Text(
            text = "Широта: ${"%.6f".format(marker.latitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
        Text(
            text = "Долгота: ${"%.6f".format(marker.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
}

// Указание аттрибуции OpenStreetMap
@Composable
private fun MapAttribution(modifier: Modifier = Modifier, context: Context) {
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