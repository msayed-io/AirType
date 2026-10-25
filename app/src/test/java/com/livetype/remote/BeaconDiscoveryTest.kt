package com.livetype.remote

import com.livetype.remote.core.discovery.BeaconParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Discovery is UDP only after the mDNS path was removed, so the beacon contract is the
 * whole discovery surface: a wrong parse means the PC never shows up and the automatic
 * connect (which needs the fresh sessionId) cannot happen.
 */
class BeaconDiscoveryTest {

    @Test
    fun `beacon supplies host port name and the fresh session id`() {
        val device = BeaconParser.parse(
            """{"type":"livetype_beacon","host":"192.168.1.20","port":53017,"name":"Laptop","sessionId":"abc123"}""",
            fallbackHost = "192.168.1.99",
        )

        assertNotNull(device)
        assertEquals("192.168.1.20", device!!.host)
        assertEquals(53017, device.port)
        assertEquals("Laptop", device.name)
        assertEquals("abc123", device.sessionId)
        assertEquals("192.168.1.20:53017", device.id)
    }

    @Test
    fun `pong is accepted like a beacon`() {
        val device = BeaconParser.parse("""{"type":"livetype_pong","host":"10.0.0.5"}""", "10.0.0.5")

        assertNotNull(device)
        assertEquals(BeaconParser.DEFAULT_PORT, device!!.port)
        assertEquals(BeaconParser.DEFAULT_NAME, device.name)
        assertNull(device.sessionId)
    }

    @Test
    fun `missing host falls back to the datagram sender`() {
        val device = BeaconParser.parse("""{"type":"livetype_beacon","port":53017}""", "192.168.0.7")

        assertNotNull(device)
        assertEquals("192.168.0.7", device!!.host)
    }

    @Test
    fun `our own probe and foreign packets are ignored`() {
        assertNull(BeaconParser.parse("""{"type":"livetype_discover","client":"LiveType-Android"}""", "1.2.3.4"))
        assertNull(BeaconParser.parse("""{"type":"something_else"}""", "1.2.3.4"))
        assertNull(BeaconParser.parse("not json at all", "1.2.3.4"))
    }

    @Test
    fun `an empty session id is treated as absent so auto connect never uses it`() {
        val device = BeaconParser.parse(
            """{"type":"livetype_beacon","host":"192.168.1.20","port":53017,"sessionId":""}""",
            "192.168.1.20",
        )

        assertNotNull(device)
        assertNull(device!!.sessionId)
    }
}
