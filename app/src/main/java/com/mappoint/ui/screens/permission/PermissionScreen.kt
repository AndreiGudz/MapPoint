package com.mappoint.ui.screens.permission

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mappoint.utils.*

@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var showLocationRationale by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLocationPermissionLauncher(
        onPermissionGranted = { hasFineLocation ->
            // Разрешение получено
            if (hasLocationPermission(context)) {
                onAllPermissionsGranted()
            }
        },
        onPermissionDenied = {
            // Пользователь отказал
            showLocationRationale = true
        }
    )

    // Проверяем при запуске, есть ли уже разрешения
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            onAllPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("PermissionScreen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Map,
            contentDescription = "Карта",
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Добро пожаловать в MapPoint!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Приложение для работы с картами и координатами",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Карточка с информацией о разрешении на местоположение
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Местоположение",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Доступ к местоположению",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = getLocationPermissionDescription(context),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (showLocationRationale) {
                    Text(
                        text = "⚠️ Вы отказали в доступе. Для полного функционала приложения рекомендуется разрешить доступ к местоположению.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки для запроса разрешений
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Запрашиваем оба разрешения
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("GrantLocationPermissionButton")
                    ) {
                        Text("Разрешить доступ к местоположению")
                    }

                    Button(
                        onClick = {
                            // Пропускаем запрос разрешений
                            onAllPermissionsGranted()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ContinueWithoutPermissionButton"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Продолжить без разрешения")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Информация о приложении
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Способен работать оффлайн оффлайн\n" +
                            "• Сохраняет карты для автономного использования\n" +
                            "• Добавление точек по координатам\n" +
                            "• Поддержка Bluetooth для получения и отправки данных",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}