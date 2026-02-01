package com.mappoint.ui.screens.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.mappoint.ui.screens.map.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    mapViewModel: MapViewModel,
    inputViewModel: InputViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by inputViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Показываем уведомления при ошибках или успехе
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
                inputViewModel.onEvent(InputScreenEvent.ResetState)
            }
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("Точка успешно добавлена!")
                // Добавляем точку на карту
                inputViewModel.getCoordinates()?.let { (lat, lng) ->
                    mapViewModel.addMarker(lat, lng, state.title)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ввод координат") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    content = {
                        Text(data.visuals.message)
                    }
                )
            }
        },
        bottomBar = {
            if (state.isLoading) {
                Box() {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)

                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Карточка с информацией
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
                            contentDescription = "Информация",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Формат ввода координат",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Широта: от -90° до 90° (например: 55.7558)\n" +
                                "• Долгота: от -180° до 180° (например: 37.6173)\n" +
                                "• Используйте точку как разделитель дробной части",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Поля для ввода координат
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = state.latitude,
                    onValueChange = { inputViewModel.onEvent(InputScreenEvent.LatitudeChanged(it)) },
                    label = { Text("Широта") },
                    placeholder = { Text("55.7558") },
                    modifier = Modifier.weight(1f),
                    isError = !state.isLatitudeValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        if (state.latitude.isNotBlank()) {
                            IconButton(onClick = { inputViewModel.onEvent(InputScreenEvent.LatitudeChanged("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = state.longitude,
                    onValueChange = { inputViewModel.onEvent(InputScreenEvent.LongitudeChanged(it)) },
                    label = { Text("Долгота") },
                    placeholder = { Text("37.6173") },
                    modifier = Modifier.weight(1f),
                    isError = !state.isLongitudeValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        if (state.longitude.isNotBlank()) {
                            IconButton(onClick = { inputViewModel.onEvent(InputScreenEvent.LongitudeChanged("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    }
                )
            }

            // Поле для названия
            OutlinedTextField(
                value = state.title,
                onValueChange = { inputViewModel.onEvent(InputScreenEvent.TitleChanged(it)) },
                label = { Text("Название точки") },
                placeholder = { Text("Москва, Красная площадь") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Поле для описания
            OutlinedTextField(
                value = state.description,
                onValueChange = { inputViewModel.onEvent(InputScreenEvent.DescriptionChanged(it)) },
                label = { Text("Описание") },
                placeholder = { Text("Дополнительная информация") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { inputViewModel.onEvent(InputScreenEvent.ClearAll) },
                    modifier = Modifier.weight(1f),
                    enabled = state.latitude.isNotBlank() ||
                            state.longitude.isNotBlank() ||
                            state.title.isNotBlank() ||
                            state.description.isNotBlank()
                ) {
                    Text("Очистить")
                }

                Button(
                    onClick = { inputViewModel.onEvent(InputScreenEvent.AddPoint) },
                    modifier = Modifier.weight(1f),
                    enabled = state.latitude.isNotBlank() &&
                            state.longitude.isNotBlank() &&
                            state.isLatitudeValid &&
                            state.isLongitudeValid
                ) {
                    Text("Добавить точку")
                }
            }

            // Кнопка возврата на карту
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вернуться на карту")
            }
        }
    }
}