package com.mappoint

import com.mappoint.ui.components.MarkerData
import com.mappoint.ui.components.getKey
import org.junit.Assert.*
import org.junit.Test

class MarkerDataTest {

    @Test
    fun testMarkerDataCreation() {
        val marker = MarkerData(
            latitude = 55.7558,
            longitude = 37.6173,
            title = "Test Marker",
            description = "Test Description",
            iconResId = 123
        )

        assertEquals(55.7558, marker.latitude, 0.0001)
        assertEquals(37.6173, marker.longitude, 0.0001)
        assertEquals("Test Marker", marker.title)
        assertEquals("Test Description", marker.description)
        assertEquals(123, marker.iconResId)
    }

    @Test
    fun testMarkerDataDefaultValues() {
        val marker = MarkerData(
            latitude = 55.7558,
            longitude = 37.6173
        )

        assertEquals(55.7558, marker.latitude, 0.0001)
        assertEquals(37.6173, marker.longitude, 0.0001)
        assertEquals("", marker.title)
        assertEquals("", marker.description)
        assertNull(marker.iconResId)
    }

    @Test
    fun testMarkerDataGetKey() {
        val marker1 = MarkerData(55.7558, 37.6173, "Point A")
        val marker2 = MarkerData(55.7558, 37.6173, "Point A")
        val marker3 = MarkerData(59.9343, 30.3351, "Point B")

        // Одинаковые координаты и заголовок должны давать одинаковый ключ
        assertEquals(marker1.getKey(), marker2.getKey())
        // Разные координаты должны давать разные ключи
        assertNotEquals(marker1.getKey(), marker3.getKey())
    }

    @Test
    fun testMarkerDataEquality() {
        val marker1 = MarkerData(55.7558, 37.6173, "Point", "Desc", 123)
        val marker2 = MarkerData(55.7558, 37.6173, "Point", "Desc", 123)
        val marker3 = MarkerData(59.9343, 30.3351, "Other", "Desc", 123)

        assertEquals(marker1, marker2)
        assertNotEquals(marker1, marker3)
    }
}