package com.mappoint.ui.screens.input

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Состояние экрана ввода
data class InputScreenState(
    val latitude: String = "",
    val longitude: String = "",
    val title: String = "",
    val description: String = "",
    val isLatitudeValid: Boolean = true,
    val isLongitudeValid: Boolean = true,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

// События экрана ввода
sealed class InputScreenEvent {
    data class LatitudeChanged(val value: String) : InputScreenEvent()
    data class LongitudeChanged(val value: String) : InputScreenEvent()
    data class TitleChanged(val value: String) : InputScreenEvent()
    data class DescriptionChanged(val value: String) : InputScreenEvent()
    object AddPoint : InputScreenEvent()
    object ClearAll : InputScreenEvent()
    object ResetState : InputScreenEvent()
}

class InputViewModel : ViewModel() {

    private val _state = MutableStateFlow(InputScreenState())
    val state: StateFlow<InputScreenState> = _state.asStateFlow()

    fun onEvent(event: InputScreenEvent) {
        when (event) {
            is InputScreenEvent.LatitudeChanged -> {
                updateLatitude(event.value)
            }

            is InputScreenEvent.LongitudeChanged -> {
                updateLongitude(event.value)
            }

            is InputScreenEvent.TitleChanged -> {
                _state.update { it.copy(title = event.value) }
            }

            is InputScreenEvent.DescriptionChanged -> {
                _state.update { it.copy(description = event.value) }
            }

            InputScreenEvent.AddPoint -> {
                addPoint()
            }

            InputScreenEvent.ClearAll -> {
                clearAll()
            }

            InputScreenEvent.ResetState -> {
                resetState()
            }
        }
    }

    private fun updateLatitude(value: String) {
        val isValid = validateCoordinate(value, -90.0, 90.0)
        _state.update {
            it.copy(
                latitude = value,
                isLatitudeValid = isValid,
                errorMessage = if (!isValid) "Широта должна быть от -90 до 90" else null
            )
        }
    }

    private fun updateLongitude(value: String) {
        val isValid = validateCoordinate(value, -180.0, 180.0)
        _state.update {
            it.copy(
                longitude = value,
                isLongitudeValid = isValid,
                errorMessage = if (!isValid) "Долгота должна быть от -180 до 180" else null
            )
        }
    }

    private fun validateCoordinate(value: String, min: Double, max: Double): Boolean {
        return try {
            val coord = value.toDouble()
            coord in min..max
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun addPoint() {
        viewModelScope.launch {
            val latitude = _state.value.latitude
            val longitude = _state.value.longitude

            if (latitude.isBlank() || longitude.isBlank()) {
                _state.update { it.copy(errorMessage = "Введите координаты") }
                return@launch
            }

            if (!_state.value.isLatitudeValid || !_state.value.isLongitudeValid) {
                _state.update { it.copy(errorMessage = "Проверьте правильность координат") }
                return@launch
            }

            try {
                val lat = latitude.toDouble()
                val lng = longitude.toDouble()

                // TODO: Здесь позже будет сохранение в базу данных
                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }

                // Через 1 секунду сбрасываем состояние успеха
                launch {
                    kotlinx.coroutines.delay(1000)
                    _state.update { it.copy(isSuccess = false) }
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = "Ошибка при обработке координат: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun clearAll() {
        _state.update {
            InputScreenState()
        }
    }

    private fun resetState() {
        _state.update {
            it.copy(
                errorMessage = null,
                isSuccess = false
            )
        }
    }

    // Получение координат в виде Double (если валидны)
    fun getCoordinates(): Pair<Double, Double>? {
        return try {
            Pair(
                _state.value.latitude.toDouble(),
                _state.value.longitude.toDouble()
            )
        } catch (e: Exception) {
            null
        }
    }
}