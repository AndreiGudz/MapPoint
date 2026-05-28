package com.mappoint

import com.mappoint.bluetooth.BluetoothData
import com.mappoint.bluetooth.DataType
import org.junit.Assert.*
import org.junit.Test

class BluetoothDataTest {

    @Test
    fun testBluetoothDataCreation() {
        val jsonMap = mapOf("type" to "gps_data", "latitude" to 55.7558)
        val data = BluetoothData(
            type = DataType.INCOMING,
            data = "{\"type\":\"gps_data\",\"latitude\":55.7558}",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            timestamp = 123456789L,
            parsedJson = jsonMap
        )

        assertEquals(DataType.INCOMING, data.type)
        assertEquals("{\"type\":\"gps_data\",\"latitude\":55.7558}", data.data)
        assertEquals("AA:BB:CC:DD:EE:FF", data.deviceAddress)
        assertEquals(123456789L, data.timestamp)
        assertEquals(jsonMap, data.parsedJson)
    }

    @Test
    fun testBluetoothDataWithoutParsedJson() {
        val data = BluetoothData(
            type = DataType.OUTGOING,
            data = "test message",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            timestamp = 123456789L
        )

        assertNull(data.parsedJson)
    }

    @Test
    fun testBluetoothDataEquality() {
        val data1 = BluetoothData(
            DataType.INCOMING,
            "test",
            "AA:BB:CC:DD:EE:FF",
            123L,
            mapOf("key" to "value")
        )

        val data2 = BluetoothData(
            DataType.INCOMING,
            "test",
            "AA:BB:CC:DD:EE:FF",
            123L,
            mapOf("key" to "value")
        )

        val data3 = BluetoothData(
            DataType.OUTGOING,
            "test",
            "AA:BB:CC:DD:EE:FF",
            123L,
            mapOf("key" to "value")
        )

        assertEquals(data1, data2)
        assertNotEquals(data1, data3)
    }

    @Test
    fun testBluetoothDataHashCode() {
        val data1 = BluetoothData(DataType.INCOMING, "test", "AA:BB:CC:DD:EE:FF", 123L)
        val data2 = BluetoothData(DataType.INCOMING, "test", "AA:BB:CC:DD:EE:FF", 123L)

        assertEquals(data1.hashCode(), data2.hashCode())
    }
}