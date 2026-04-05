package com.mappoint.ui.screens.bluetooth

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mappoint.utils.bluetooth.BluetoothClassicManager
import com.mappoint.utils.bluetooth.BluetoothData
import com.mappoint.utils.bluetooth.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _uiState.update { it.copy(
                    connectionState = state,
                    connectedDeviceName = if (state == ConnectionState.CONNECTED) {
                        "ESP32"
                    } else null
                ) }
            }
        }

        viewModelScope.launch {
            bluetoothManager.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(discoveredDevices = devices) }
            }
        }

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
        val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
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
        if (!_uiState.value.hasPermissions) {
            return
        }

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
        if (!_uiState.value.hasPermissions) {
            return
        }

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

    fun sendData(data: String) {
        if (!_uiState.value.hasPermissions) {
            return
        }

        viewModelScope.launch {
            bluetoothManager.sendData(data)
        }
    }

    fun sendTestGpsData(latitude: Double, longitude: Double) {
        if (!_uiState.value.hasPermissions) {
            return
        }

        val latBytes = floatToBytes(latitude.toFloat())
        val lngBytes = floatToBytes(longitude.toFloat())
        val binaryData = latBytes + lngBytes

        viewModelScope.launch {
            bluetoothManager.sendBytes(binaryData)
        }
    }

    fun sendTestJsonData(latitude: Double, longitude: Double, additionalData: Map<String, Any> = emptyMap()) {
        if (!_uiState.value.hasPermissions) {
            return
        }

        val jsonString = buildString {
            append("{")
            append("\"type\":\"gps_data\",")
            append("\"latitude\":$latitude,")
            append("\"longitude\":$longitude,")
            append("\"timestamp\":${System.currentTimeMillis()}")
            if (additionalData.isNotEmpty()) {
                additionalData.forEach { (key, value) ->
                    append(",\"$key\":\"$value\"")
                }
            }
            append("}")
        }

        viewModelScope.launch {
            bluetoothManager.sendData(jsonString)
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
        if (data.isBinary && data.binaryData != null && data.binaryData.size >= 8) {
            val latitude = bytesToFloat(data.binaryData.sliceArray(0..3))
            val longitude = bytesToFloat(data.binaryData.sliceArray(4..7))
            handleGpsData(latitude.toDouble(), longitude.toDouble())
        }

        if (data.parsedJson != null && data.parsedJson["type"] == "gps_data") {
            val latitude = (data.parsedJson["latitude"] as? Double) ?: return
            val longitude = (data.parsedJson["longitude"] as? Double) ?: return
            handleGpsData(latitude, longitude)
        }
    }

    private fun handleGpsData(latitude: Double, longitude: Double) {
        onGpsDataReceived?.invoke(latitude, longitude)
    }

    var onGpsDataReceived: ((Double, Double) -> Unit)? = null

    private fun floatToBytes(value: Float): ByteArray {
        return byteArrayOf(
            (value.toBits() shr 24).toByte(),
            (value.toBits() shr 16).toByte(),
            (value.toBits() shr 8).toByte(),
            value.toBits().toByte()
        )
    }

    private fun bytesToFloat(bytes: ByteArray): Float {
        val intBits = ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
        return Float.fromBits(intBits)
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }
}