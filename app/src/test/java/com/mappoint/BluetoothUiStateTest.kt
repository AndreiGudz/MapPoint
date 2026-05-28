package com.mappoint

import android.bluetooth.BluetoothDevice
import com.mappoint.bluetooth.ConnectionState
import com.mappoint.ui.screens.bluetooth.BluetoothUiState
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class BluetoothUiStateTest {

    @Test
    fun testDefaultBluetoothUiState() {
        val state = BluetoothUiState()

        assertEquals(ConnectionState.DISCONNECTED, state.connectionState)
        assertTrue(state.discoveredDevices.isEmpty())
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isScanning)
        assertFalse(state.isBluetoothEnabled)
        assertNull(state.connectedDeviceName)
        assertFalse(state.hasPermissions)
    }

    @Test
    fun testBluetoothUiStateWithValues() {
        val mockDevice = mockk<BluetoothDevice>()
        val devices = listOf(mockDevice)

        val state = BluetoothUiState(
            connectionState = ConnectionState.CONNECTED,
            discoveredDevices = devices,
            messages = listOf(),
            isScanning = true,
            isBluetoothEnabled = true,
            connectedDeviceName = "ESP32",
            hasPermissions = true
        )

        assertEquals(ConnectionState.CONNECTED, state.connectionState)
        assertEquals(devices, state.discoveredDevices)
        assertTrue(state.isScanning)
        assertTrue(state.isBluetoothEnabled)
        assertEquals("ESP32", state.connectedDeviceName)
        assertTrue(state.hasPermissions)
    }

    @Test
    fun testBluetoothUiStateCopy() {
        val original = BluetoothUiState(
            connectionState = ConnectionState.CONNECTING,
            isScanning = true
        )

        val copied = original.copy(isScanning = false)

        assertEquals(ConnectionState.CONNECTING, copied.connectionState)
        assertFalse(copied.isScanning)
    }

    @Test
    fun testBluetoothUiStateEquality() {
        val state1 = BluetoothUiState(
            connectionState = ConnectionState.CONNECTED,
            isBluetoothEnabled = true
        )

        val state2 = BluetoothUiState(
            connectionState = ConnectionState.CONNECTED,
            isBluetoothEnabled = true
        )

        val state3 = BluetoothUiState(
            connectionState = ConnectionState.DISCONNECTED,
            isBluetoothEnabled = true
        )

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }
}