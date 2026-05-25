package com.mappoint.ui.screens.bluetooth

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mappoint.bluetooth.ConnectionState

@Composable
fun DevicesTab(
    uiState: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (android.bluetooth.BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().testTag("DevicesTab")
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
                    hasPermissions = uiState.hasPermissions,
                    onConnect = { onConnect(device) }
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: android.bluetooth.BluetoothDevice,
    isConnecting: Boolean,
    hasPermissions: Boolean,
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
                @SuppressLint("MissingPermission")
                val deviceName = if (hasPermissions) {
                    device.name ?: "Unknown Device"
                } else {
                    "Разрешения не предоставлены"
                }
                Text(
                    text = deviceName,
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