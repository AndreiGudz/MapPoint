package com.mappoint.ui.screens.bluetooth

import android.Manifest
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mappoint.utils.bluetooth.BluetoothData
import com.mappoint.utils.bluetooth.ConnectionState
import com.mappoint.utils.bluetooth.DataType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onGpsDataReceived: (Double, Double) -> Unit
) {
    val uiState by bluetoothViewModel.uiState.collectAsState()
    val messages by bluetoothViewModel.messages.collectAsState()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        bluetoothViewModel.onGpsDataReceived = onGpsDataReceived
    }

    // Проверка наличия разрешений
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[BLUETOOTH_CONNECT] == true && permissions[BLUETOOTH_SCAN] == true
        } else {
            permissions[BLUETOOTH] == true && permissions[BLUETOOTH_ADMIN] == true
        }

        if (allGranted) {
            bluetoothViewModel.updateBluetoothState()
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            bluetoothViewModel.updateBluetoothState()
        }
    }

    // Запрос разрешений при старте, если их нет
    LaunchedEffect(Unit) {
        if (!hasBluetoothPermissions()) {
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
            } else {
                arrayOf(BLUETOOTH, BLUETOOTH_ADMIN)
            }
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth ESP32") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (uiState.hasPermissions) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                                ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = when (uiState.connectionState) {
                                    ConnectionState.CONNECTED -> "Подключено"
                                    ConnectionState.CONNECTING -> "Подключение..."
                                    ConnectionState.DISCONNECTED -> "Отключено"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.hasPermissions && uiState.connectionState == ConnectionState.CONNECTED) {
                FloatingActionButton(
                    onClick = { bluetoothViewModel.disconnect() },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.BluetoothDisabled, contentDescription = "Отключиться")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Проверка разрешений
            if (!uiState.hasPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Требуются разрешения Bluetooth")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    arrayOf(BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
                                } else {
                                    arrayOf(BLUETOOTH, BLUETOOTH_ADMIN)
                                }
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        ) {
                            Text("Предоставить разрешения")
                        }
                    }
                }
            }

            // Проверка включен ли Bluetooth
            if (uiState.hasPermissions && !uiState.isBluetoothEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Bluetooth выключен")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                enableBluetoothLauncher.launch(enableIntent)
                            }
                        ) {
                            Text("Включить Bluetooth")
                        }
                    }
                }
            }

            // Основной интерфейс, только если есть разрешения
            if (uiState.hasPermissions) {
                var selectedTab by remember { mutableStateOf(0) }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null) },
                        text = { Text("Устройства") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                        text = { Text("Чат") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                        text = { Text("Тесты") }
                    )
                }

                when (selectedTab) {
                    0 -> DevicesTab(
                        uiState = uiState,
                        onStartScan = { bluetoothViewModel.startDiscovery() },
                        onStopScan = { bluetoothViewModel.stopDiscovery() },
                        onConnect = { bluetoothViewModel.connectToDevice(it) }
                    )
                    1 -> ChatTab(
                        messages = messages,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                bluetoothViewModel.sendJson(inputText)
                                inputText = ""
                            }
                        },
                        onClear = { bluetoothViewModel.clearMessages() }
                    )
                    2 -> TestTab(
                        onSendGpsData = { lat, lng ->
                            bluetoothViewModel.sendGpsData(lat, lng)
                        }
                    )
                }
            }
        }
    }
}

@Composable
@RequiresPermission(BLUETOOTH_CONNECT)
fun DevicesTab(
    uiState: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (android.bluetooth.BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartScan,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isScanning && uiState.isBluetoothEnabled
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Сканировать")
            }

            Button(
                onClick = onStopScan,
                modifier = Modifier.weight(1f),
                enabled = uiState.isScanning
            ) {
                Text("Остановить")
            }
        }

        if (uiState.isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.discoveredDevices.isEmpty() && !uiState.isScanning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Нажмите 'Сканировать' для поиска устройств",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(uiState.discoveredDevices) { device ->
                DeviceCard(
                    device = device,
                    isConnecting = uiState.connectionState == ConnectionState.CONNECTING,
                    onConnect = { onConnect(device) }
                )
            }
        }
    }
}

@Composable
@RequiresPermission(BLUETOOTH_CONNECT)
fun DeviceCard(
    device: android.bluetooth.BluetoothDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) { onConnect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Button(onClick = onConnect) {
                    Text("Подключиться")
                }
            }
        }
    }
}

@Composable
fun ChatTab(
    messages: List<BluetoothData>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Нет сообщений",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(messages.reversed()) { message ->
                MessageBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Введите сообщение...") },
                singleLine = true
            )

            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }

            IconButton(onClick = onClear) {
                Icon(Icons.Default.ClearAll, contentDescription = "Очистить")
            }
        }
    }
}

@Composable
fun MessageBubble(message: BluetoothData) {
    val isOutgoing = message.type == DataType.OUTGOING
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOutgoing)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.data,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dateFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TestTab(
    onSendGpsData: (Double, Double) -> Unit
) {
    var testLat by remember { mutableStateOf("55.7558") }
    var testLng by remember { mutableStateOf("37.6173") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Тестовые данные для ESP32",
            style = MaterialTheme.typography.titleLarge
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Отправить GPS координаты (JSON)")

                OutlinedTextField(
                    value = testLat,
                    onValueChange = { testLat = it },
                    label = { Text("Широта") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = testLng,
                    onValueChange = { testLng = it },
                    label = { Text("Долгота") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val lat = testLat.toDoubleOrNull() ?: 55.7558
                        val lng = testLng.toDoubleOrNull() ?: 37.6173
                        onSendGpsData(lat, lng)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отправить JSON")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Формат JSON:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "{\"type\":\"gps_data\",\"latitude\":55.7558,\"longitude\":37.6173,\"timestamp\":1234567890}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}