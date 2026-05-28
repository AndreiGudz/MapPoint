package com.mappoint

import com.mappoint.ui.screens.map.MapPoint
import org.junit.Assert.*
import org.junit.Test

class MapPointTest {

    @Test
    fun testMapPointCreation() {
        val point = MapPoint(
            id = 1,
            latitude = 55.7558,
            longitude = 37.6173,
            title = "Test Point",
            description = "Test Description",
            timestamp = 123456789L
        )

        assertEquals(1, point.id)
        assertEquals(55.7558, point.latitude, 0.0001)
        assertEquals(37.6173, point.longitude, 0.0001)
        assertEquals("Test Point", point.title)
        assertEquals("Test Description", point.description)
        assertEquals(123456789L, point.timestamp)
    }

    @Test
    fun testMapPointDefaultValues() {
        val point = MapPoint(
            latitude = 55.7558,
            longitude = 37.6173
        )

        assertEquals(0, point.id)
        assertEquals(55.7558, point.latitude, 0.0001)
        assertEquals(37.6173, point.longitude, 0.0001)
        assertEquals("", point.title)
        assertEquals("", point.description)
        assertTrue(point.timestamp > 0)
    }

    @Test
    fun testMapPointEquality() {
        val point1 = MapPoint(1, 55.7558, 37.6173, "Point A")
        val point2 = MapPoint(1, 55.7558, 37.6173, "Point A")
        val point3 = MapPoint(2, 59.9343, 30.3351, "Point B")

        assertEquals(point1, point2)
        assertNotEquals(point1, point3)
    }

    @Test
    fun testMapPointHashCode() {
        val point1 = MapPoint(1, 55.7558, 37.6173, "Point A")
        val point2 = MapPoint(1, 55.7558, 37.6173, "Point A")

        assertEquals(point1.hashCode(), point2.hashCode())
    }
}