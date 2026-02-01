package com.mappoint.ui.components

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mappoint.ui.screens.map.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(55.7558, 37.6173), // Москва по умолчанию
    zoomLevel: Double = com.mappoint.ui.screens.map.startZoomLevel,
    markers: List<MarkerData> = emptyList(),
    onMapReady: (MapView) -> Unit = {}
) {
    // Получаем контекст вне AndroidView
    val context = LocalContext.current

    // Инициализация osmdroid
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                // Настройка карты
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel = com.mappoint.ui.screens.map.minZoomLevel
                maxZoomLevel = com.mappoint.ui.screens.map.maxZoomLevel

                // Установка начальной позиции
                controller.setZoom(zoomLevel)
                controller.setCenter(center)

                // Настройка кеширования для оффлайн работы
                setUseDataConnection(true)
                tileProvider.tileSource = TileSourceFactory.MAPNIK
            }
        },
        update = { mapView ->
            // Обновляем центр и зум
            mapView.controller.setCenter(center)
            mapView.controller.setZoom(zoomLevel)

            // Обновление маркеров при изменении списка
            mapView.overlays.removeIf { it is Marker }

            markers.forEach { markerData ->
                val marker = Marker(mapView)
                marker.position = GeoPoint(markerData.latitude, markerData.longitude)
                marker.title = markerData.title
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Используем Context из MapView или внешний context
                val markerContext = mapView.context

                // Можно установить свою иконку
                if (markerData.iconResId != null) {
                    val drawable = ContextCompat.getDrawable(markerContext, markerData.iconResId)
                    marker.icon = drawable
                } else {
                    // Установим стандартную иконку из ресурсов osmdroid
                    marker.icon = ContextCompat.getDrawable(markerContext,
                        org.osmdroid.library.R.drawable.osm_ic_follow_me_on)
                }

                mapView.overlays.add(marker)
            }

            mapView.invalidate() // Перерисовка карты

            // Вызываем callback когда карта готова
            onMapReady(mapView)
        }
    )
}

// Data class для маркеров
data class MarkerData(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val iconResId: Int? = null
)