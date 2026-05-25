package com.mappoint.ui.screens.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mappoint.bluetooth.BluetoothData
import com.mappoint.bluetooth.DataType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatTab(
    messages: List<BluetoothData>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRequestGps: () -> Unit,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()

    // Автоматическая прокрутка к последнему сообщению при отправке/получении
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().testTag("ChatTab")
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

            items(messages) { message ->
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

            IconButton(
                onClick = { onRequestGps() },
                modifier = Modifier.testTag("RequestGpsButton")
            ) {
                Icon(
                    Icons.Default.GpsNotFixed,
                    contentDescription = "Запросить GPS",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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