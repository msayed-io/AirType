package com.livetype.remote.core.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * LAN auto-discovery for LiveType Desktop Bridges.
 *
 * Discovery is UDP only: we broadcast a small probe to port 53018 and the PC answers
 * with a JSON beacon (see [BeaconParser]). The Windows app does not advertise over
 * mDNS at all, so the old NSD path here could never find anything — it was removed
 * instead of being left as dead code that made the docs look wrong.
 */
class NetworkDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    companion object {
        const val TAG = "LiveTypeDiscovery"
        const val DISCOVERY_UDP_PORT = 53018
        const val PROBE_INTERVAL_MS = 2500L
        const val DEVICE_TTL_MS = 15000L
    }

    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val deviceMap = ConcurrentHashMap<String, DiscoveredDevice>()
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var udpJob: Job? = null
    private var cleanupJob: Job? = null

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        acquireMulticastLock()
        startUdpBeaconProbing()
        startStaleDeviceCleaner()
    }

    fun stopDiscovery() {
        _isScanning.value = false
        udpJob?.cancel()
        udpJob = null
        cleanupJob?.cancel()
        cleanupJob = null
        releaseMulticastLock()
    }

    fun refresh() {
        deviceMap.clear()
        _devices.value = emptyList()
        if (_isScanning.value) {
            stopDiscovery()
            startDiscovery()
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("LiveTypeDiscoveryLock")?.apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.let {
                if (!it.isHeld) it.acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MulticastLock", e)
        }
    }

    // -------------------------------------------------------------------------
    // UDP Beacon Probing — the one discovery path that exists on the wire
    // -------------------------------------------------------------------------

    private fun startUdpBeaconProbing() {
        udpJob?.cancel()
        udpJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    soTimeout = 1500
                    bind(InetSocketAddress(0)) // Ephemeral port for sending/receiving replies
                }

                // Listener sub-coroutine for incoming UDP replies
                launch(Dispatchers.IO) {
                    val buffer = ByteArray(2048)
                    val packet = DatagramPacket(buffer, buffer.size)
                    while (isActive) {
                        try {
                            socket.receive(packet)
                            val senderHost = packet.address.hostAddress ?: continue
                            val jsonString = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                            parseUdpBeaconMessage(jsonString, senderHost)
                        } catch (_: java.net.SocketTimeoutException) {
                            // Expected loop timeout
                        } catch (e: Exception) {
                            if (!isActive) break
                            Log.d(TAG, "UDP receive error: ${e.message}")
                        }
                    }
                }

                // Periodic probe transmitter loop
                val probePayload = JSONObject().apply {
                    put("type", BeaconParser.PROBE_TYPE)
                    put("client", "LiveType-Android")
                    put("timestamp", System.currentTimeMillis())
                }.toString().toByteArray(Charsets.UTF_8)

                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val probePacket = DatagramPacket(probePayload, probePayload.size, broadcastAddr, DISCOVERY_UDP_PORT)

                while (isActive) {
                    try {
                        socket.send(probePacket)
                    } catch (e: Exception) {
                        Log.d(TAG, "UDP probe broadcast failed: ${e.message}")
                    }
                    delay(PROBE_INTERVAL_MS)
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP socket setup failed", e)
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun parseUdpBeaconMessage(rawJson: String, fallbackHost: String) {
        BeaconParser.parse(rawJson, fallbackHost)?.let(::registerDevice)
    }

    // -------------------------------------------------------------------------
    // Device Registry & Health
    // -------------------------------------------------------------------------

    private fun registerDevice(device: DiscoveredDevice) {
        deviceMap[device.id] = device
        updateDeviceList()
    }

    private fun updateDeviceList() {
        val now = System.currentTimeMillis()
        val active = deviceMap.values
            .filter { (now - it.lastSeenMs) < DEVICE_TTL_MS }
            .sortedByDescending { it.lastSeenMs }
        _devices.value = active
    }

    private fun startStaleDeviceCleaner() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(5000L)
                val now = System.currentTimeMillis()
                val removed = deviceMap.entries.removeIf { (now - it.value.lastSeenMs) >= DEVICE_TTL_MS }
                if (removed) {
                    updateDeviceList()
                }
            }
        }
    }
}
