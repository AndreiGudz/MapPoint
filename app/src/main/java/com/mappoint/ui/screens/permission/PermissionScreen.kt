package com.mappoint.ui.screens.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mappoint.utils.rememberLocationPermissionLauncher
import com.mappoint.utils.rememberStoragePermissionLauncher
import com.mappoint.utils.hasFineLocationPermission
import com.mappoint.utils.hasLocationPermission
import com.mappoint.utils.hasStoragePermission

@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit,
    onPartialPermissionsGranted: () -> Unit = {}
) {
    val context = LocalContext.current

    // Создаем ланчеры для запроса разрешений
    val locationPermissionLauncher = rememberLocationPermissionLauncher(
        onPermissionGranted = { isFine ->
            // Можно добавить логику в зависимости от типа разрешения
            if (isFine) {
                // Точное местоположение получено
            } else {
                // Приблизительное местоположение получено
            }
            checkPermissionsAndNavigate(context, onAllPermissionsGranted, onPartialPermissionsGranted)
        },
        onPermissionDenied = {
            // Пользователь отказал в разрешении
        }
    )

    val storagePermissionLauncher = rememberStoragePermissionLauncher(
        onPermissionGranted = {
            checkPermissionsAndNavigate(context, onAllPermissionsGranted, onPartialPermissionsGranted)
        },
        onPermissionDenied = {
            // Пользователь отказал в разрешении
        }
    )

    // Проверяем текущие разрешения
    val hasLocation = hasLocationPermission(context)
    val hasStorage = hasStoragePermission(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Разрешения для работы приложения",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Карточка для разрешения на местоположение
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Местоположение",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Доступ к местоположению",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Необходимо для центрирования карты на вашей позиции. " +
                            "Мы запрашиваем точное (GPS) и приблизительное (сеть) местоположение.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Статус разрешения
                Text(
                    text = if (hasLocation) "✅ Разрешение получено" else "❌ Разрешение не получено",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasLocation) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )

                if (hasLocation) {
                    Text(
                        text = if (hasFineLocationPermission(context))
                            "Точное местоположение (GPS)"
                        else
                            "Приблизительное местоположение",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Запрашиваем оба разрешения одновременно
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !hasLocation
                ) {
                    Text(if (hasLocation) "Разрешение получено" else "Запросить доступ")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Карточка для разрешения на хранилище
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = "Хранилище",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Доступ к хранилищу",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Необходимо для сохранения карт в оффлайн режиме.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Статус разрешения
                Text(
                    text = if (hasStorage) "✅ Разрешение получено" else "❌ Разрешение не получено",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasStorage) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Запрашиваем разрешения в зависимости от версии Android
                        // Android 10+ - только READ
                        storagePermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !hasStorage
                ) {
                    Text(if (hasStorage) "Разрешение получено" else "Запросить доступ")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка продолжения
        Button(
            onClick = {
                checkPermissionsAndNavigate(context, onAllPermissionsGranted, onPartialPermissionsGranted)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Продолжить")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Информация о пропуске
        Text(
            text = "Вы можете продолжить без некоторых разрешений, но функционал будет ограничен",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

// Вспомогательная функция для проверки разрешений и навигации
private fun checkPermissionsAndNavigate(
    context: android.content.Context,
    onAllPermissionsGranted: () -> Unit,
    onPartialPermissionsGranted: () -> Unit
) {
    val hasLocation = hasLocationPermission(context)
    val hasStorage = hasStoragePermission(context)

    when {
        hasLocation && hasStorage -> onAllPermissionsGranted()
        hasLocation || hasStorage -> onPartialPermissionsGranted()
        else -> onPartialPermissionsGranted() // Даже если нет разрешений, продолжаем
    }
}