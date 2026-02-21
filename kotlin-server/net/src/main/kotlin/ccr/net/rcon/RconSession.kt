package ccr.net.rcon

import java.net.InetSocketAddress

private const val TIMEOUT_MS = 60_000L

class RconSession(
    val address: InetSocketAddress,
) {
    var isAuthenticated: Boolean = false
    var lastActivityMs: Long = System.currentTimeMillis()

    fun touch(nowMs: Long = System.currentTimeMillis()) {
        lastActivityMs = nowMs
    }

    fun isTimedOut(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs - lastActivityMs > TIMEOUT_MS
}
