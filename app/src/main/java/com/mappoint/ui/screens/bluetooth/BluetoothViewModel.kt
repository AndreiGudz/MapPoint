package com.mappoint.ui.screens.bluetooth

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mappoint.bluetooth.BluetoothClassicManager
import com.mappoint.bluetooth.BluetoothData
import com.mappoint.bluetooth.BluetoothPermissionHelper
import com.mappoint.bluetooth.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class BluetoothUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val discoveredDevices: List<BluetoothDevice> = emptyList(),
    val messages: List<BluetoothData> = emptyList(),
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val connectedDeviceName: String? = null,
    val hasPermissions: Boolean = false
)

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    private var lastGpsTimestamp = 0L
    private val GPS_MIN_INTERVAL_MS = 2000L // минимум 2 секунды между маркерами

    private val bluetoothManager = BluetoothClassicManager(application)

    private val _hasPermissions = MutableStateFlow(BluetoothPermissionHelper.hasPermissions(application))
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<BluetoothData>>(emptyList())
    val messages: StateFlow<List<BluetoothData>> = _messages.asStateFlow()

    init {
        // Отслеживание состояния подключения
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _uiState.update { it.copy(
                    connectionState = state,
                    connectedDeviceName = if (state == ConnectionState.CONNECTED) "ESP32" else null
                ) }
            }
        }

        // Отслеживание состояния поиска устройств
        viewModelScope.launch {
            bluetoothManager.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(discoveredDevices = devices) }
            }
        }

        // Получение входящих данных
        viewModelScope.launch {
            bluetoothManager.incomingData.collect { data ->
                addMessage(data)
                handleIncomingData(data)
            }
        }

        updateBluetoothState()
        checkPermissions()
    }

    private fun checkPermissions() {
        val granted = BluetoothPermissionHelper.hasPermissions(getApplication())
        _hasPermissions.value = granted
        _uiState.update { it.copy(hasPermissions = granted) }
    }

    /**
     * Вызывается экраном, когда пользователь предоставил или отклонил разрешения.
     */
    fun onPermissionsResult(granted: Boolean) {
        _hasPermissions.value = granted
        _uiState.update { it.copy(hasPermissions = granted) }
        if (granted) {
            updateBluetoothState()
        }
    }

    fun updateBluetoothState() {
        val isEnabled = try {
            bluetoothManager.isBluetoothEnabled()
        } catch (e: SecurityException) {
            false
        }
        _uiState.update { it.copy(isBluetoothEnabled = isEnabled) }
    }

    fun startDiscovery() {
        if (!_uiState.value.hasPermissions) return
        if (bluetoothManager.isBluetoothEnabled()) {
            _uiState.update { it.copy(isScanning = true) }
            bluetoothManager.startDiscovery()
            viewModelScope.launch {
                delay(10000) // Остановить поиск через 10 секунд
                stopDiscovery()
            }
        }
    }

    fun stopDiscovery() {
        bluetoothManager.stopDiscovery()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!_uiState.value.hasPermissions) return
        viewModelScope.launch {
            bluetoothManager.connectToDevice(device)
            stopDiscovery()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothManager.disconnect()
        }
    }

    /**
     * Запрос текущих GPS-координат у ESP32
     * Отправляет команду {"type":"request_gps"}
     */
    fun requestGpsFromDevice() {
        if (!_uiState.value.hasPermissions) {
            Log.w("BluetoothVM", "Cannot request GPS: no permissions")
            return
        }
        if (uiState.value.connectionState != ConnectionState.CONNECTED) {
            Log.w("BluetoothVM", "Cannot request GPS: not connected")
            return
        }
        val requestJson = JSONObject().apply {
            put("type", "request_gps")
        }.toString()
        sendJson(requestJson)
    }

    /**
     * Отправка произвольной JSON-строки.
     */
    fun sendJson(jsonString: String) {
        if (!_uiState.value.hasPermissions) return
        viewModelScope.launch {
            bluetoothManager.sendJson(jsonString)
        }
    }

    private fun addMessage(data: BluetoothData) {
        viewModelScope.launch {
            _messages.update { currentMessages ->
                (currentMessages + data).takeLast(100)
            }
        }
    }

    private fun handleIncomingData(data: BluetoothData) {
        // Обрабатываем только JSON-данные
        val now = System.currentTimeMillis()
        if (now - lastGpsTimestamp < GPS_MIN_INTERVAL_MS) {
            Log.d("BluetoothVM", "GPS data ignored (too frequent)")
            return
        }
        lastGpsTimestamp = now
        val json = data.parsedJson ?: return
        when (json["type"]) {
            "gps_data" -> {
                val lat = (json["latitude"] as? Number)?.toDouble()
                val lng = (json["longitude"] as? Number)?.toDouble()
                val title = json["title"] as? String
                val description = json["description"] as? String
                if (lat != null && lng != null) {
                    handleGpsData(lat, lng, title, description)
                }
            }
            // Можно добавить другие типы сообщений от ESP32
            else -> {
                Log.d("BluetoothVM", "Unknown message type: ${json["type"]}")
            }
        }
    }

    private fun handleGpsData(latitude: Double, longitude: Double, title: String?, description: String?) {
        onGpsDataReceived?.invoke(latitude, longitude, title, description)
    }

    var onGpsDataReceived: ((Double, Double, String?, String?) -> Unit)? = null

    fun clearMessages() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }
}