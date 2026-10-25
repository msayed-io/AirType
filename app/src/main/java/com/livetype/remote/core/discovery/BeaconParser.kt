package com.livetype.remote.core.discovery

import org.json.JSONObject

/**
 * The only discovery contract that exists on the wire: the Windows app answers our
 * UDP probe on port 53018 with a small JSON beacon. It never advertises over mDNS,
 * so parsing lives here as a pure function that a plain JVM test can exercise
 * without an Android device.
 */
object BeaconParser {

    const val PROBE_TYPE = "livetype_discover"
    const val DEFAULT_PORT = 53017
    const val DEFAULT_NAME = "LiveType Desktop"

    /**
     * @param rawJson the datagram payload.
     * @param fallbackHost the sender address, used when the beacon omits `host`.
     * @return the discovered PC, or null for our own probe echo and foreign packets.
     */
    fun parse(rawJson: String, fallbackHost: String): DiscoveredDevice? {
        val json = try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            return null // non-JSON or foreign UDP traffic
        }

        val type = json.optString("type")
        if (type != "livetype_beacon" && type != "livetype_pong") return null

        val host = json.optString("host").ifBlank { fallbackHost }
        if (host.isBlank()) return null

        val port = json.optInt("port", DEFAULT_PORT)
        val name = json.optString("name").ifBlank { DEFAULT_NAME }
        val sessionId = json.optString("sessionId").takeIf { it.isNotBlank() }

        return DiscoveredDevice(
            id = "$host:$port",
            name = name,
            host = host,
            port = port,
            sessionId = sessionId,
            lastSeenMs = System.currentTimeMillis(),
        )
    }
}
