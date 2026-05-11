package com.mappoint

import com.mappoint.ui.screens.map.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class MapPointTest {

    @Test
    fun createMapPoint_correctValues() {

        val point = MapPoint(
            latitude = 55.7558,
            longitude = 37.6176,
            title = "Москва",
            description = "Тест"
        )

        assertEquals(55.7558, point.latitude, 0.0)
        assertEquals(37.6176, point.longitude, 0.0)
        assertEquals("Москва", point.title)
        assertEquals("Тест", point.description)
    }
}