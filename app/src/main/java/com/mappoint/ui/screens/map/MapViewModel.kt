package com.mappoint.ui.screens.map

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappoint.utils.LocationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Класс для представления точки на карте
data class MapPoint(
    val id: Int? = null,
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val description: String = ""
)

const val minZoomLevel = 3.0
const val maxZoomLevel = 17.0
const val startZoomLevel = 15.0

class MapViewModel(application: Application) : AndroidViewModel(application) {
    // Счётчик для обновления координат
    private val _frame = MutableStateFlow(0)
    val frame = _frame.asStateFlow()

    // Текущий центр карты
    private val _center = MutableStateFlow(GeoPoint(55.7558, 37.6173)) // Москва по умолчанию
    val center: StateFlow<GeoPoint> = _center.asStateFlow()

    // Уровень зума
    private val _zoomLevel = MutableStateFlow(startZoomLevel)
    val zoomLevel: StateFlow<Double> = _zoomLevel.asStateFlow()

    // Список маркеров на карте
    private val _markers = MutableStateFlow<List<MapPoint>>(emptyList())
    val markers: StateFlow<List<MapPoint>> = _markers.asStateFlow()

    // Текущий выбранный маркер
    private val _selectedMarker = MutableStateFlow<MapPoint?>(null)
    val selectedMarker: StateFlow<MapPoint?> = _selectedMarker.asStateFlow()

    // Флаг загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Функции для управления картой
    fun setCenter(latitude: Double, longitude: Double) {
        _center.value = GeoPoint(latitude, longitude)
        _frame.value++
    }

    fun setZoom(zoom: Double) {
        _zoomLevel.value = zoom.coerceIn(minZoomLevel, maxZoomLevel)
        _frame.value++
    }



    // Центрирование на текущем местоположении
    fun centerOnMyLocation() {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            location?.let {
                setCenter(it.latitude, it.longitude)
            }
        }
    }

    // Добавление маркера
    fun addMarker(latitude: Double, longitude: Double, title: String = "") {
        viewModelScope.launch {
            val newMarker = MapPoint(
                latitude = latitude,
                longitude = longitude,
                title = if (title.isBlank()) "Точка ${_markers.value.size + 1}" else title
            )

            _markers.value = _markers.value + newMarker
            _selectedMarker.value = newMarker
            setCenter(latitude, longitude)
        }
    }

    // Удаление маркера
    fun removeMarker(marker: MapPoint) {
        viewModelScope.launch {
            _markers.value = _markers.value.filter { it != marker }
            if (_selectedMarker.value == marker) {
                _selectedMarker.value = null
            }
        }
    }

    // Удаление всех маркеров
    fun clearAllMarkers() {
        viewModelScope.launch {
            _markers.value = emptyList()
            _selectedMarker.value = null
        }
    }

    // Центрирование с анимацией
    fun centerOnMarker(marker: MapPoint, zoom: Double = 15.0) {
        viewModelScope.launch {
            setCenter(marker.latitude, marker.longitude)
            setZoom(zoom)
            _selectedMarker.value = marker
        }
    }

    private fun findMarker(latitude: Double, longitude: Double) : MapPoint? {
        return _markers.value.find {
            it.latitude == latitude && it.longitude == longitude
        }
    }

    fun getMarkerClickListener() : Marker.OnMarkerClickListener {
        return Marker.OnMarkerClickListener { marker, mapView ->
            marker!!.showInfoWindow()
            mapView!!.controller
                .animateTo(marker.position)
            _selectedMarker.value = findMarker(marker.position.latitude, marker.position.longitude)
            true
        }
    }
}