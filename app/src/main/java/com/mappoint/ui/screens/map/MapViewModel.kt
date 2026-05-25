package com.mappoint.ui.screens.map

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappoint.data.AppDatabase
import com.mappoint.data.PointEntity
import com.mappoint.data.PointRepository
import com.mappoint.utils.LocationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Класс для представления точки на карте
data class MapPoint(
    val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// Класс для панели ввода
data class InputFormState(
    val latitude: String = "",
    val longitude: String = "",
    val title: String = "",
    val description: String = "",
    val isLatitudeValid: Boolean = true,
    val isLongitudeValid: Boolean = true
)

const val minZoomLevel = 2.0
const val maxZoomLevel = 17.9
const val startZoomLevel = 15.0

class MapViewModel(application: Application) : AndroidViewModel(application) {

    // Источник данных
    private val locationProvider = LocationProvider(application)
    private val repository: PointRepository

    // Счётчик для обновления координат
    private val _frame = MutableStateFlow(0)
    val frame = _frame.asStateFlow()

    // Текущий центр карты
    private val _center = MutableStateFlow(GeoPoint(55.7558, 37.6173)) // Москва по умолчанию
    val center: StateFlow<GeoPoint> = _center.asStateFlow()

    // Последние текущие координаты
    private val _lastLocation = MutableStateFlow<GeoPoint?>(null)
    val lastLocation: StateFlow<GeoPoint?> = _lastLocation.asStateFlow()

    // Уровень зума
    private val _zoomLevel = MutableStateFlow(startZoomLevel)
    val zoomLevel: StateFlow<Double> = _zoomLevel.asStateFlow()

    // Список маркеров на карте
    private val _markersFlow = MutableStateFlow<List<MapPoint>>(emptyList())
    val markers: StateFlow<List<MapPoint>> = _markersFlow.asStateFlow()


    // Текущий выбранный маркер
    private val _selectedMarker = MutableStateFlow<MapPoint?>(null)
    val selectedMarker: StateFlow<MapPoint?> = _selectedMarker.asStateFlow()

    // Флаг загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Состояние поля ввода
    private val _formState = MutableStateFlow(InputFormState())
    val formState: StateFlow<InputFormState> = _formState.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = PointRepository(database.pointDao())

        // Подписываемся на изменения в БД и преобразуем Entity в MapPoint
        viewModelScope.launch {
            repository.getAllPoints().collect { entities ->
                val points = entities.map { entity ->
                    MapPoint(
                        id = entity.id,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        title = entity.title,
                        description = entity.description,
                        timestamp = entity.timestamp
                    )
                }
                _markersFlow.value = points
                // Если выбранный маркер был удалён, сбрасываем
                if (_selectedMarker.value != null && !points.contains(_selectedMarker.value)) {
                    _selectedMarker.value = null
                }
            }
        }

        // Запускаем подписку на обновления местоположения при создании ViewModel
        viewModelScope.launch {
            locationProvider.getLocationFlow().collect { location ->
                _lastLocation.value = GeoPoint(location.latitude, location.longitude)
            }
        }
    }

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
        if (_lastLocation.value == null) {
            viewModelScope.launch {
                _lastLocation.value = fetchCurrentLocationOnce()
            }
        }
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        _lastLocation.value?.let { geoPoint ->
            setCenter(geoPoint.latitude, geoPoint.longitude)
            addMarker(geoPoint.latitude, geoPoint.longitude, "Текущая в ${formatter.format(Date())}")
        }
    }

    // Для одноразового запроса
    suspend fun fetchCurrentLocationOnce(): GeoPoint? {
        return locationProvider.getCurrentLocation()?.let {
            GeoPoint(it.latitude, it.longitude)
        }
    }

    // Проверка доступности получения текущей позиции
    fun isLocationEnabled(): Boolean {
        return locationProvider.isLocationEnabled()
    }

    // Добавление маркера
    fun addMarker(latitude: Double, longitude: Double, title: String = "") {
        viewModelScope.launch {
            val point = MapPoint(
                latitude = latitude,
                longitude = longitude,
                title = if (title.isBlank()) "Точка ${_markersFlow.value.size + 1}" else title,
                timestamp = System.currentTimeMillis()
            )
            repository.insertPoint(point.toEntity())
            _selectedMarker.value = point
            setCenter(latitude, longitude)
        }
    }

    // Удаление маркера
    fun removeMarker(marker: MapPoint) {
        viewModelScope.launch {
            repository.deletePoint(marker.toEntity())
            if (_selectedMarker.value == marker) {
                _selectedMarker.value = null
            }
        }
    }

    // Удаление всех маркеров
    fun clearAllMarkers() {
        viewModelScope.launch {
            repository.deleteAllPoints()
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

    // Получени маркера по широте и долготе
    private fun findMarker(latitude: Double, longitude: Double) : MapPoint? {
        return _markersFlow.value.find {
            it.latitude == latitude && it.longitude == longitude
        }
    }

    // Получить регирование на нажатие на маркер
    fun getMarkerClickListener() : Marker.OnMarkerClickListener {
        return Marker.OnMarkerClickListener { marker, mapView ->
            marker?.showInfoWindow()
            mapView?.controller?.animateTo(marker.position)
            _selectedMarker.value = findMarker(marker.position.latitude, marker.position.longitude)
            true
        }
    }

    fun updateFormLatitude(value: String) {
        val isValid = if (value.isBlank()) {
            true
        } else {
            value.toDoubleOrNull()?.let { it in -90.0..90.0 } ?: false
        }
        _formState.update { it.copy(latitude = value, isLatitudeValid = isValid) }
    }

    fun updateFormLongitude(value: String) {
        val isValid = if (value.isBlank()) {
            true
        } else {
            value.toDoubleOrNull()?.let { it in -180.0..180.0 } ?: false
        }
        _formState.update { it.copy(longitude = value, isLongitudeValid = isValid) }
    }

    fun updateFormTitle(value: String) {
        _formState.update { it.copy(title = value) }
    }

    fun updateFormDescription(value: String) {
        _formState.update { it.copy(description = value) }
    }

    fun addPointFromForm(): Boolean {
        val lat = _formState.value.latitude.toDoubleOrNull()
        val lng = _formState.value.longitude.toDoubleOrNull()
        if (lat == null || lng == null) return false

        val title = if (_formState.value.title.isBlank()) {
            "Точка ${_markersFlow.value.size + 1}"
        } else {
            _formState.value.title
        }

        addMarker(lat, lng, title)
        clearForm()
        return true
    }

    fun clearForm() {
        _formState.value = InputFormState()
    }

    // Вспомогательная функция преобразования
    private fun MapPoint.toEntity(): PointEntity = PointEntity(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        title = this.title,
        description = this.description,
        timestamp = this.timestamp
    )
}