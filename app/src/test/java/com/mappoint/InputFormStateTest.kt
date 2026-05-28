package com.mappoint

import com.mappoint.ui.screens.map.InputFormState
import org.junit.Assert.*
import org.junit.Test

class InputFormStateTest {

    @Test
    fun testDefaultInputFormState() {
        val state = InputFormState()

        assertEquals("", state.latitude)
        assertEquals("", state.longitude)
        assertEquals("", state.title)
        assertEquals("", state.description)
        assertTrue(state.isLatitudeValid)
        assertTrue(state.isLongitudeValid)
    }

    @Test
    fun testInputFormStateWithValues() {
        val state = InputFormState(
            latitude = "55.7558",
            longitude = "37.6173",
            title = "Moscow",
            description = "Red Square",
            isLatitudeValid = true,
            isLongitudeValid = true
        )

        assertEquals("55.7558", state.latitude)
        assertEquals("37.6173", state.longitude)
        assertEquals("Moscow", state.title)
        assertEquals("Red Square", state.description)
        assertTrue(state.isLatitudeValid)
        assertTrue(state.isLongitudeValid)
    }

    @Test
    fun testInputFormStateCopy() {
        val original = InputFormState(
            latitude = "55.7558",
            longitude = "37.6173",
            title = "Original",
            description = "Original description"
        )

        val copied = original.copy(title = "New Title")

        assertEquals("55.7558", copied.latitude)
        assertEquals("37.6173", copied.longitude)
        assertEquals("New Title", copied.title)
        assertEquals("Original description", copied.description)
    }

    @Test
    fun testInputFormStateEquals() {
        val state1 = InputFormState(
            latitude = "55.7558",
            longitude = "37.6173",
            title = "Point"
        )

        val state2 = InputFormState(
            latitude = "55.7558",
            longitude = "37.6173",
            title = "Point"
        )

        val state3 = InputFormState(
            latitude = "59.9343",
            longitude = "30.3351",
            title = "SPb"
        )

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }
}