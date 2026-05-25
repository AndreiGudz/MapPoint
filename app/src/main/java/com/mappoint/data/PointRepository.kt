package com.mappoint.data

import kotlinx.coroutines.flow.Flow

class PointRepository(private val pointDao: PointDao) {
    fun getAllPoints(): Flow<List<PointEntity>> = pointDao.getAllPoints()

    suspend fun insertPoint(point: PointEntity) = pointDao.insert(point)

    suspend fun deletePoint(point: PointEntity) = pointDao.delete(point)

    suspend fun deleteAllPoints() = pointDao.deleteAll()
}