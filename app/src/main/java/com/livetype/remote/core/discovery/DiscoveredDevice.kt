package com.livetype.remote.core.discovery

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveredDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val sessionId: String? = null,
    val lastSeenMs: Long = System.currentTimeMillis(),
) {
    val displayAddress: String
        get() = "$host:$port"
}
