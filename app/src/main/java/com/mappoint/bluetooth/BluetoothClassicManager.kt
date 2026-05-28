package com.mappoint.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothClassicManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothManager"
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        fun getEnableBluetoothIntent(): Intent {
            return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        }
    }

    private var isReceiverRegistered = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _incomingData = MutableSharedFlow<BluetoothData>()
    val incomingData: SharedFlow<BluetoothData> = _incomingData.asSharedFlow()

    private var currentDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val discoveryReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (hasBluetoothPermissions()) {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (device != null && device.name != null) {
                            addDeviceIfNew(device)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Discovery finished")
                }
            }
        }
    }

    init {
        if (hasBluetoothPermissions()) {
            registerReceivers()
        } else {
            Log.e(TAG, "Missing Bluetooth permissions, receiver not registered")
        }
    }

    private fun hasBluetoothPermissions(): Boolean = BluetoothPermissionHelper.hasPermissions(context)

    private fun ensureReceiverRegistered() {
        if (!isReceiverRegistered && BluetoothPermissionHelper.hasPermissions(context)) {
            registerReceivers()
            isReceiverRegistered = true
        }
    }

    private fun registerReceivers() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(discoveryReceiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver: ${e.message}")
        }
    }

    private fun addDeviceIfNew(device: BluetoothDevice) {
        val currentDevices = _discoveredDevices.value.toMutableList()
        if (currentDevices.none { it.address == device.address }) {
            currentDevices.add(device)
            _discoveredDevices.value = currentDevices
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking Bluetooth state: ${e.message}")
            false
        }
    }

    fun startDiscovery() {
        ensureReceiverRegistered()
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Missing permissions for discovery")
            return
        }
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth not enabled")
            return
        }
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
            _discoveredDevices.value = emptyList()
            bluetoothAdapter?.startDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting discovery: ${e.message}")
        }
    }

    fun stopDiscovery() {
        if (!hasBluetoothPermissions()) return
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping discovery: ${e.message}")
        }
    }

    suspend fun connectToDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermissions()) return@withContext false
        try {
            disconnect()
            stopDiscovery()
            currentDevice = device
            _connectionState.value = ConnectionState.CONNECTING

            bluetoothSocket = try {
                device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception creating socket: ${e.message}")
                return@withContext false
            }

            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            _connectionState.value = ConnectionState.CONNECTED
            Log.d(TAG, "Connected to ${device.name}")
            startReceiving()
            return@withContext true
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            _connectionState.value = ConnectionState.DISCONNECTED
            closeSocket()
            return@withContext false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during connection: ${e.message}")
            _connectionState.value = ConnectionState.DISCONNECTED
            closeSocket()
            return@withContext false
        }
    }

    /**
     * Отправка строки (JSON) на ESP32.
     */
    suspend fun sendJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermissions()) return@withContext false
        return@withContext try {
            val bytes = jsonString.toByteArray(Charsets.UTF_8)
            outputStream?.write(bytes)
            outputStream?.flush()

            val bluetoothData = BluetoothData(
                type = DataType.OUTGOING,
                data = jsonString,
                deviceAddress = currentDevice?.address ?: "",
                timestamp = System.currentTimeMillis(),
                parsedJson = tryParseJson(jsonString)
            )
            _incomingData.emit(bluetoothData)
            Log.d(TAG, "Sent JSON: $jsonString")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Send failed: ${e.message}")
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception sending data: ${e.message}")
            false
        }
    }

    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val buffer = ByteArray(4096)
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val rawData = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        val parsed = parseReceivedData(rawData)
                        _incomingData.emit(parsed)
                        Log.d(TAG, "Received: $rawData")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Receive error: ${e.message}")
                    if (_connectionState.value == ConnectionState.CONNECTED) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        closeSocket()
                    }
                    break
                }
                delay(10)
            }
        }
    }

    private fun parseReceivedData(rawData: String): BluetoothData {
        val json = tryParseJson(rawData)
        return BluetoothData(
            type = DataType.INCOMING,
            data = rawData,
            deviceAddress = currentDevice?.address ?: "",
            timestamp = System.currentTimeMillis(),
            parsedJson = json
        )
    }

    private fun tryParseJson(text: String): Map<String, Any>? {
        return try {
            val jsonObject = JSONObject(text)
            // Преобразуем JSONObject в Map<String, Any>
            jsonObject.keys().asSequence().associateWith { key ->
                jsonObject.get(key)
            }
        } catch (e: Exception) {
            Log.d(TAG, "TryParseJson error: ${e.message}")
            null
        }
    }

    fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        closeSocket()
        currentDevice = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "Disconnected")
    }

    private fun closeSocket() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
        }
    }

    fun cleanup() {
        scope.cancel()
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Receiver not registered ${e.message}")
        }
        disconnect()
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class DataType {
    INCOMING,
    OUTGOING
}

data class BluetoothData(
    val type: DataType,
    val data: String,
    val deviceAddress: String,
    val timestamp: Long,
    val parsedJson: Map<String, Any>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothData

        if (type != other.type) return false
        if (data != other.data) return false
        if (deviceAddress != other.deviceAddress) return false
        if (timestamp != other.timestamp) return false
        if (parsedJson != other.parsedJson) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + deviceAddress.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (parsedJson?.hashCode() ?: 0)
        return result
    }
}