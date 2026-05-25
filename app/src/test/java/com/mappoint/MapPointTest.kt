package com.mappoint

import com.mappoint.ui.screens.map.MapPoint
import org.junit.Assert
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

        Assert.assertEquals(55.7558, point.latitude, 0.0)
        Assert.assertEquals(37.6176, point.longitude, 0.0)
        Assert.assertEquals("Москва", point.title)
        Assert.assertEquals("Тест", point.description)
    }
}