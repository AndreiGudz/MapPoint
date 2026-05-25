package com.mappoint.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "points")
data class PointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)