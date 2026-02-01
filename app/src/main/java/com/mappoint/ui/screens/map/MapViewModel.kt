package com.mappoint.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

// Класс для представления точки на карте
data class MapPoint(
    val id: Int? = null,
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val description: String = ""
)

const val minZoomLevel = 3.0
const val maxZoomLevel = 25.0
const val startZoomLevel = 15.0

class MapViewModel : ViewModel() {

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
    }

    fun setZoom(zoom: Double) {
        _zoomLevel.value = zoom.coerceIn(minZoomLevel, maxZoomLevel)
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
            _center.value = GeoPoint(latitude, longitude)
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

    // Выбор маркера
    fun selectMarker(marker: MapPoint?) {
        _selectedMarker.value = marker
        marker?.let {
            _center.value = GeoPoint(it.latitude, it.longitude)
        }
    }
}