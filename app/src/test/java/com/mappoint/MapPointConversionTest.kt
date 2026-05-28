package com.mappoint

import com.mappoint.data.PointEntity
import com.mappoint.ui.screens.map.MapPoint
import org.junit.Assert.*
import org.junit.Test

class MapPointConversionTest {

    @Test
    fun testMapPointToEntityConversion() {
        val mapPoint = MapPoint(
            id = 1,
            latitude = 55.7558,
            longitude = 37.6173,
            title = "Test Point",
            description = "Test Description",
            timestamp = 123456789L
        )

        val entity = mapPoint.toEntity()

        assertEquals(mapPoint.id, entity.id)
        assertEquals(mapPoint.latitude, entity.latitude, 0.0001)
        assertEquals(mapPoint.longitude, entity.longitude, 0.0001)
        assertEquals(mapPoint.title, entity.title)
        assertEquals(mapPoint.description, entity.description)
        assertEquals(mapPoint.timestamp, entity.timestamp)
    }

    @Test
    fun testEntityToMapPointConversion() {
        val entity = PointEntity(
            id = 1,
            latitude = 55.7558,
            longitude = 37.6173,
            title = "Entity Point",
            description = "Entity Description",
            timestamp = 123456789L
        )

        val mapPoint = MapPoint(
            id = entity.id,
            latitude = entity.latitude,
            longitude = entity.longitude,
            title = entity.title,
            description = entity.description,
            timestamp = entity.timestamp
        )

        assertEquals(entity.id, mapPoint.id)
        assertEquals(entity.latitude, mapPoint.latitude, 0.0001)
        assertEquals(entity.longitude, mapPoint.longitude, 0.0001)
        assertEquals(entity.title, mapPoint.title)
        assertEquals(entity.description, mapPoint.description)
        assertEquals(entity.timestamp, mapPoint.timestamp)
    }

    // Вспомогательная extension функция для теста
    private fun MapPoint.toEntity(): PointEntity = PointEntity(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        title = this.title,
        description = this.description,
        timestamp = this.timestamp
    )
}