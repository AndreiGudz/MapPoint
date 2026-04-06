package com.mappoint.ui.screens.bluetooth

import android.Manifest
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mappoint.utils.bluetooth.BluetoothClassicManager
import com.mappoint.utils.bluetooth.BluetoothData
import com.mappoint.utils.bluetooth.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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

    private val bluetoothManager = BluetoothClassicManager(application)

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
        val application = getApplication<Application>()
        val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(application, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(application, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(application, BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(application, BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
        _uiState.update { it.copy(hasPermissions = hasPermissions) }
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
     * Отправка произвольной JSON-строки.
     */
    fun sendJson(jsonString: String) {
        if (!_uiState.value.hasPermissions) return
        viewModelScope.launch {
            bluetoothManager.sendJson(jsonString)
        }
    }

    /**
     * Удобный метод для отправки GPS-координат в формате JSON.
     */
    fun sendGpsData(latitude: Double, longitude: Double, additionalData: Map<String, Any> = emptyMap()) {
        val jsonObject = JSONObject().apply {
            put("type", "gps_data")
            put("latitude", latitude)
            put("longitude", longitude)
            put("timestamp", System.currentTimeMillis())
            additionalData.forEach { (key, value) ->
                put(key, value)
            }
        }
        sendJson(jsonObject.toString())
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
        val json = data.parsedJson ?: return
        when (json["type"]) {
            "gps_data" -> {
                val lat = (json["latitude"] as? Number)?.toDouble()
                val lng = (json["longitude"] as? Number)?.toDouble()
                if (lat != null && lng != null) {
                    handleGpsData(lat, lng)
                }
            }
            // Можно добавить другие типы сообщений от ESP32
            else -> {
                Log.d("BluetoothVM", "Unknown message type: ${json["type"]}")
            }
        }
    }

    private fun handleGpsData(latitude: Double, longitude: Double) {
        onGpsDataReceived?.invoke(latitude, longitude)
    }

    var onGpsDataReceived: ((Double, Double) -> Unit)? = null

    fun clearMessages() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }
}