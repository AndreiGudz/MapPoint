package com.mappoint.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mappoint.ui.screens.map.startZoomLevel
import org.osmdroid.config.Configuration
import org.osmdroid.library.R
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
    markerClickListener: Marker.OnMarkerClickListener,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Проверяем, уничтожается ли активность
    val mapView = remember {
        MapView(context).apply {
            // Настройка карты
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = com.mappoint.ui.screens.map.minZoomLevel
            maxZoomLevel = com.mappoint.ui.screens.map.maxZoomLevel
            // Настройка кеширования для оффлайн работы
            setUseDataConnection(true)  // можно использовать интернет
            tileProvider.tileSource = TileSourceFactory.MAPNIK
            controller.setCenter(center)
            controller.setZoom(zoomLevel)
        }
    }

    // Создание оверлея текущей позиции
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            // Настройка оверлея
            enableMyLocation() // Включить определение местоположения
//            enableFollowLocation() // Карта будет следовать за пользователем
            isDrawAccuracyEnabled = true // Отображать радиус точности
        }

    }
    // Добавление оверлея текущей позиции на карту
    DisposableEffect(Unit) {
        mapView.overlays.add(myLocationOverlay)
        onDispose {
            mapView.overlays.remove(myLocationOverlay)
            myLocationOverlay.onDetach(mapView)
        }
    }

    // Для отслеживания жизненного цикла приложения и реакции на Запуск/Паузу
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    myLocationOverlay.enableMyLocation()
                    myLocationOverlay.enableFollowLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    myLocationOverlay.disableMyLocation()
                    myLocationOverlay.disableFollowLocation()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDetach()
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
        myLocationOverlay.disableFollowLocation()
//        mapView.controller.animateTo(center, zoomLevel, 1000L)
        mapView.controller.setCenter(center)
        mapView.controller.setZoom(zoomLevel)
    }

    // Обновляем маркеры при изменении списка
    LaunchedEffect(markers) {
        updateMarkers(mapView, markers, context, markerClickListener)
    }

    // Вставляем MapView в Compose
    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )
}

// создание маркера для MapView из MarkerData
private fun createMarker(
    mapView: MapView,
    markerData: MarkerData,
    context: Context,
    markerClickListener: Marker.OnMarkerClickListener
): Marker = Marker(mapView).apply {
    position = GeoPoint(markerData.latitude, markerData.longitude)
    title = markerData.title
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    setOnMarkerClickListener(markerClickListener)
    icon = if (markerData.iconResId != null) {
        ContextCompat.getDrawable(context, markerData.iconResId)
    } else {
        ContextCompat.getDrawable(context, R.drawable.osm_ic_follow_me_on)
    }
}

// Вспомогательная функция для обновления маркеров
private fun updateMarkers(
    mapView: MapView,
    markers: List<MarkerData>,
    context: Context,
    markerClickListener: Marker.OnMarkerClickListener
) {
    mapView.overlays.removeIf { it is Marker }
    markers.forEach { markerData ->
        val marker = createMarker(mapView, markerData, context, markerClickListener)
        mapView.overlays.add(marker)
    }
    mapView.invalidate()
}

// Data class для маркеров
data class MarkerData(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val iconResId: Int? = null
)

fun MarkerData.getKey(): String = "$latitude:$longitude:${title.hashCode()}"