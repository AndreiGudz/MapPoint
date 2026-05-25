package com.mappoint.ui.screens.bluetooth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mappoint.bluetooth.BluetoothClassicManager
import com.mappoint.bluetooth.BluetoothData
import com.mappoint.bluetooth.BluetoothPermissionHelper
import com.mappoint.bluetooth.ConnectionState
import com.mappoint.bluetooth.DataType
import com.mappoint.bluetooth.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var locationEnabled by remember {  mutableStateOf(LocationHelper.isLocationEnabled(context)) }

    LaunchedEffect(Unit) {
        bluetoothViewModel.onGpsDataReceived = onGpsDataReceived
    }

    // Единственный лаунчер для запроса разрешений
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val required = BluetoothPermissionHelper.getRequiredPermissions()
        val allGranted = required.all { permissions[it] == true }
        bluetoothViewModel.onPermissionsResult(allGranted)
    }

    // Включение Bluetooth
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            bluetoothViewModel.updateBluetoothState()
        }
    }

    // Включение GPS
    val enableLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        locationEnabled = LocationHelper.isLocationEnabled(context)
    }

    // Запрашиваем разрешения при старте, если их нет
    LaunchedEffect(Unit) {
        if (!bluetoothViewModel.hasPermissions.value) {
            permissionLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions())
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
            // Проверка разрешений через uiState.hasPermissions
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
                        Text("Требуются разрешения")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(BluetoothPermissionHelper.getRequiredPermissions())
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
                                val enableIntent = BluetoothClassicManager.getEnableBluetoothIntent()
                                enableBluetoothLauncher.launch(enableIntent)
                            }
                        ) {
                            Text("Включить Bluetooth")
                        }
                    }
                }
            }

            // Проверка включен ли GPS на Android 10-11
            val isOldAndroidVersion = Build.VERSION.SDK_INT <= Build.VERSION_CODES.R
            if (uiState.hasPermissions && !locationEnabled && isOldAndroidVersion) {
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
                        Text("Для поиска Bluetooth требуется включить геолокацию")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val enableIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            enableLocationLauncher.launch(enableIntent)
                        }) {
                            Text("Включить геолокацию")
                        }
                    }
                }
            }

            // Основной интерфейс, только если есть разрешения
            if (uiState.hasPermissions) {
                var selectedTab by remember { mutableIntStateOf(0) }
                PrimaryTabRow(selectedTabIndex = selectedTab) {
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
                }
            }
        }
    }
}
