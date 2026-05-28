package com.mappoint

import com.mappoint.bluetooth.ConnectionState
import org.junit.Assert.*
import org.junit.Test

class ConnectionStateTest {

    @Test
    fun testConnectionStateValues() {
        assertEquals(0, ConnectionState.DISCONNECTED.ordinal)
        assertEquals(1, ConnectionState.CONNECTING.ordinal)
        assertEquals(2, ConnectionState.CONNECTED.ordinal)
    }

    @Test
    fun testConnectionStateNames() {
        assertEquals("DISCONNECTED", ConnectionState.DISCONNECTED.name)
        assertEquals("CONNECTING", ConnectionState.CONNECTING.name)
        assertEquals("CONNECTED", ConnectionState.CONNECTED.name)
    }
}