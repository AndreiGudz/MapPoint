package com.mappoint.ui.screens.map

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun AddDrawerContent(viewModel: MapViewModel, onClose: () -> Unit) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("AddPointPanel"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок с кнопкой закрытия
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ввод координат",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("Back")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Закрыть")
            }
        }

        // Карточка с информацией о формате
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Поля ввода координат
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = formState.latitude,
                onValueChange = { viewModel.updateFormLatitude(it) },
                label = { Text("Широта") },
                placeholder = { Text("55.7558") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("LatitudeField"),
                isError = !formState.isLatitudeValid && formState.latitude.isNotBlank(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    if (formState.latitude.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateFormLatitude("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                supportingText = {
                    if (!formState.isLatitudeValid && formState.latitude.isNotBlank()) {
                        Text("Широта должна быть от -90 до 90")
                    }
                }
            )

            OutlinedTextField(
                value = formState.longitude,
                onValueChange = { viewModel.updateFormLongitude(it) },
                label = { Text("Долгота") },
                placeholder = { Text("37.6173") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("LongitudeField"),
                isError = !formState.isLongitudeValid && formState.longitude.isNotBlank(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    if (formState.longitude.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateFormLongitude("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                supportingText = {
                    if (!formState.isLongitudeValid && formState.longitude.isNotBlank()) {
                        Text("Долгота должна быть от -180 до 180")
                    }
                }
            )
        }

        // Поле для названия
        OutlinedTextField(
            value = formState.title,
            onValueChange = { viewModel.updateFormTitle(it) },
            label = { Text("Название точки") },
            placeholder = { Text("Москва, Красная площадь") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("TitleField"),
            singleLine = true
        )

        // Поле для описания
        OutlinedTextField(
            value = formState.description,
            onValueChange = { viewModel.updateFormDescription(it) },
            label = { Text("Описание") },
            placeholder = { Text("Дополнительная информация") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("DescriptionField"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.clearForm() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ClearFormButton"),
                enabled = formState.latitude.isNotBlank() ||
                        formState.longitude.isNotBlank() ||
                        formState.title.isNotBlank() ||
                        formState.description.isNotBlank()
            ) {
                Text("Очистить")
            }

            Button(
                onClick = {
                    val added = viewModel.addPointFromForm()
                    if (added) {
                        onClose()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("SavePointButton"),
                enabled = formState.latitude.isNotBlank() &&
                        formState.longitude.isNotBlank() &&
                        formState.isLatitudeValid &&
                        formState.isLongitudeValid
            ) {
                Text("Добавить точку")
            }
        }
    }
}