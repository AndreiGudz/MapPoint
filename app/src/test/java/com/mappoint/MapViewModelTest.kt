package com.mappoint

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mappoint.ui.screens.map.MapViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Создаём viewModel с реальным контекстом приложения
        // Для unit-тестов используем mock Application
        val mockContext = mockk<android.app.Application>(relaxed = true)
        viewModel = MapViewModel(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ==================== Валидация полей формы ====================

    @Test
    fun `form validation - valid latitude passes`() {
        viewModel.updateFormLatitude("55.7558")
        assertTrue(viewModel.formState.value.isLatitudeValid)
        assertEquals("55.7558", viewModel.formState.value.latitude)
    }

    @Test
    fun `form validation - invalid latitude fails`() {
        viewModel.updateFormLatitude("100")
        assertFalse(viewModel.formState.value.isLatitudeValid)
    }

    @Test
    fun `form validation - negative latitude passes`() {
        viewModel.updateFormLatitude("-33.8688")
        assertTrue(viewModel.formState.value.isLatitudeValid)
    }

    @Test
    fun `form validation - valid longitude passes`() {
        viewModel.updateFormLongitude("37.6173")
        assertTrue(viewModel.formState.value.isLongitudeValid)
        assertEquals("37.6173", viewModel.formState.value.longitude)
    }

    @Test
    fun `form validation - invalid longitude fails`() {
        viewModel.updateFormLongitude("200")
        assertFalse(viewModel.formState.value.isLongitudeValid)
    }

    @Test
    fun `form validation - empty fields are considered valid`() {
        viewModel.updateFormLatitude("")
        viewModel.updateFormLongitude("")
        assertTrue(viewModel.formState.value.isLatitudeValid)
        assertTrue(viewModel.formState.value.isLongitudeValid)
    }

    @Test
    fun `clearForm - resets all form fields`() {
        viewModel.updateFormLatitude("55.7558")
        viewModel.updateFormLongitude("37.6173")
        viewModel.updateFormTitle("Тест")
        viewModel.updateFormDescription("Описание")

        viewModel.clearForm()

        assertEquals("", viewModel.formState.value.latitude)
        assertEquals("", viewModel.formState.value.longitude)
        assertEquals("", viewModel.formState.value.title)
        assertEquals("", viewModel.formState.value.description)
        assertTrue(viewModel.formState.value.isLatitudeValid)
        assertTrue(viewModel.formState.value.isLongitudeValid)
    }

    @Test
    fun `updateFormTitle - updates title correctly`() {
        viewModel.updateFormTitle("Новый заголовок")
        assertEquals("Новый заголовок", viewModel.formState.value.title)
    }

    @Test
    fun `updateFormDescription - updates description correctly`() {
        viewModel.updateFormDescription("Новое описание")
        assertEquals("Новое описание", viewModel.formState.value.description)
    }
}