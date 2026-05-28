package com.mappoint

import com.mappoint.data.PointDao
import com.mappoint.data.PointEntity
import com.mappoint.data.PointRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class PointRepositoryTest {

    private lateinit var pointDao: PointDao
    private lateinit var repository: PointRepository

    @Before
    fun setup() {
        pointDao = mockk()
        repository = PointRepository(pointDao)
    }

    @Test
    fun testGetAllPoints() = runBlocking {
        val expectedPoints = listOf(
            PointEntity(1, 55.7558, 37.6173, "Point 1", "Description"),
            PointEntity(2, 59.9343, 30.3351, "Point 2", "Description")
        )
        val flow = flowOf(expectedPoints)
        coEvery { pointDao.getAllPoints() } returns flow

        val result = repository.getAllPoints()

        coVerify(exactly = 1) { pointDao.getAllPoints() }
        assert(result === flow)
    }

    @Test
    fun testInsertPoint() = runBlocking {
        val point = PointEntity(1, 55.7558, 37.6173, "Test Point", "Description")
        coEvery { pointDao.insert(point) } returns Unit

        repository.insertPoint(point)

        coVerify(exactly = 1) { pointDao.insert(point) }
    }

    @Test
    fun testDeletePoint() = runBlocking {
        val point = PointEntity(1, 55.7558, 37.6173, "Test Point", "Description")
        coEvery { pointDao.delete(point) } returns Unit

        repository.deletePoint(point)

        coVerify(exactly = 1) { pointDao.delete(point) }
    }

    @Test
    fun testDeleteAllPoints() = runBlocking {
        coEvery { pointDao.deleteAll() } returns Unit

        repository.deleteAllPoints()

        coVerify(exactly = 1) { pointDao.deleteAll() }
    }
}