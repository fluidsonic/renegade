package ccr.net.connection

import ccr.net.reliability.ReliableChannel
import ccr.net.reliability.ResendPolicy
import ccr.net.reliability.UnreliableChannel
import java.net.InetSocketAddress

// C++: cRemoteHost in wwnet/rhost.h/.cpp
// Per-client state for a connected remote host.

// C++: KEEPALIVE_TIMEOUT_MS = 2000 (netutil.cpp)
// C++: SERVER_CONNECTION_LOSS_TIMEOUT = 15000 (netutil.cpp)
// C++: SERVER_CONNECTION_LOSS_TIMEOUT_LOADING_ALLOWANCE = 45000 (netutil.cpp)
private const val KEEPALIVE_TIMEOUT_MS = 2000L
private const val CONNECTION_LOSS_TIMEOUT_MS = 15_000L
private const val LOADING_EXTRA_TIMEOUT_MS = 45_000L

class RemoteHost(
    val id: Int,
    val address: InetSocketAddress,
    val maximumBps: Int = 28_800,
) {
    val reliable = ReliableChannel()
    val unreliable = UnreliableChannel()
    val resendPolicy = ResendPolicy()

    // Time of last received packet (ms)
    var lastContactTimeMs: Long = System.currentTimeMillis()
        private set

    // Time of last keepalive sent to this host
    var lastKeepaliveTimeMs: Long = 0L
        private set

    // Per-host statistics
    var packetLossPc: Float = 0f

    // True while client is loading the map — extends timeout allowance
    var isLoading: Boolean = false

    // Whether this host must be evicted (e.g. kicked)
    var mustEvict: Boolean = false

    // Service count at the last keepalive (for calculating service rate)
    var lastServiceCount: Int = 0

    val creationTimeMs: Long = System.currentTimeMillis()

    fun touch(nowMs: Long = System.currentTimeMillis()) {
        lastContactTimeMs = nowMs
    }

    fun markKeepaliveSent(nowMs: Long = System.currentTimeMillis()) {
        lastKeepaliveTimeMs = nowMs
    }

    fun isKeepaliveDue(nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs - lastKeepaliveTimeMs > KEEPALIVE_TIMEOUT_MS
    }

    // C++: timeout check in Service_Read — 15s + 45s extra if loading
    fun isTimedOut(nowMs: Long = System.currentTimeMillis()): Boolean {
        val timeout = if (isLoading)
            CONNECTION_LOSS_TIMEOUT_MS + LOADING_EXTRA_TIMEOUT_MS
        else
            CONNECTION_LOSS_TIMEOUT_MS
        return nowMs - lastContactTimeMs > timeout
    }
}
