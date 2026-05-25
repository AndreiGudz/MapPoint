package com.mappoint.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PointDao {
    @Query("SELECT * FROM points ORDER BY timestamp DESC")
    fun getAllPoints(): Flow<List<PointEntity>>

    @Insert
    suspend fun insert(point: PointEntity)

    @Delete
    suspend fun delete(point: PointEntity)

    @Query("DELETE FROM points")
    suspend fun deleteAll()
}