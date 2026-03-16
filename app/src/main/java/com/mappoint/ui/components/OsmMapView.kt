package com.mappoint.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mappoint.ui.screens.map.startZoomLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(55.7558, 37.6173),
    frame: Int = 0,
    zoomLevel: Double = startZoomLevel,
    markers: List<MarkerData> = emptyList(),
    onMapReady: (MapView) -> Unit = {},
    markerClickListener: Marker.OnMarkerClickListener
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Инициализация osmdroid
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )

    // Создаем MapView и сохраняем ссылку
    val mapView = remember {
        MapView(context).apply {
            // Настройка карты
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = com.mappoint.ui.screens.map.minZoomLevel
            maxZoomLevel = com.mappoint.ui.screens.map.maxZoomLevel

            // Настройка кеширования для оффлайн работы
            setUseDataConnection(true)
            tileProvider.tileSource = TileSourceFactory.MAPNIK

            // Начальная позиция
            controller.setZoom(zoomLevel)
            controller.setCenter(center)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Обрабатываем анимацию перемещения
    LaunchedEffect(frame) {
        mapView.controller.animateTo(center, zoomLevel, 1000L)
        Log.d("OsmMapView", "Animating to: $center, zoom: $zoomLevel")
    }

    // Обновляем маркеры при изменении списка
    LaunchedEffect(markers) {
        mapView.overlays.removeIf { it is Marker }

        markers.forEach { markerData ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(markerData.latitude, markerData.longitude)
            marker.title = markerData.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setOnMarkerClickListener(markerClickListener)

            // Установка иконки
            if (markerData.iconResId != null) {
                val drawable = ContextCompat.getDrawable(context, markerData.iconResId)
                marker.icon = drawable
            } else {
                // Стандартная иконка
                marker.icon = ContextCompat.getDrawable(
                    context,
                    org.osmdroid.library.R.drawable.osm_ic_follow_me_on
                )
            }

            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    // Вставляем MapView в Compose
    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )

    // 1. Создание оверлея
    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)

    // 2. Настройка оверлея
    locationOverlay.enableMyLocation() // Включить определение местоположения
    locationOverlay.enableFollowLocation() // Карта будет следовать за пользователем
    locationOverlay.isDrawAccuracyEnabled = true // Отображать радиус точности

    // 3. Добавление оверлея на карту
    mapView.overlays.add(locationOverlay)

    // Вызываем callback когда карта готова
    LaunchedEffect(Unit) {
        onMapReady(mapView)

        // 4. Центрировать на пользователе при старте
        locationOverlay.runOnFirstFix {
            scope.launch(Dispatchers.Main) {
                mapView.controller.animateTo(locationOverlay.myLocation)
            }
        }
    }
}

// Data class для маркеров
data class MarkerData(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val iconResId: Int? = null
)