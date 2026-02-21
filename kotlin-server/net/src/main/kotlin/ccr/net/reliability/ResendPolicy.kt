package ccr.net.reliability

// C++: Adjust_Resend_Timeout() in wwnet/rhost.cpp
// Dynamic resend timeout adapts based on observed round-trip times.
//
// Algorithm (from C++ comments):
//   - Start at DEFAULT_RESEND_TIMEOUT_MS
//   - Every RESEND_RESET_INTERVAL_MS: timeout = min(3*avg_ping, 1.3*max_ping)
//   - Clamped to [MIN_RESEND_TIMEOUT_MS, MAX_RESEND_TIMEOUT_MS]
//   - The "reset" prevents rare outlandishly large pings from permanently inflating the timeout

private const val DEFAULT_RESEND_TIMEOUT_MS = 333
private const val MIN_RESEND_TIMEOUT_MS = 333
private const val MAX_RESEND_TIMEOUT_MS = 3000
private const val RESEND_RESET_INTERVAL_MS = 2000L

class ResendPolicy {
    var timeoutMs: Int = DEFAULT_RESEND_TIMEOUT_MS
        private set

    private var numPings: Int = 0
    private var totalPingMs: Long = 0
    private var maxPingMs: Int = 0
    private var lastResetTimeMs: Long = System.currentTimeMillis()

    // Called when an ACK is received for a packet that was NOT resent
    // (resent packets don't contribute to ping measurement — C++ comment: "only from non-resent packets")
    fun recordPing(pingMs: Int) {
        numPings++
        totalPingMs += pingMs
        if (pingMs > maxPingMs) maxPingMs = pingMs
    }

    // C++: Adjust_Resend_Timeout — call periodically to adapt the timeout
    fun tick(nowMs: Long = System.currentTimeMillis()) {
        if (nowMs - lastResetTimeMs < RESEND_RESET_INTERVAL_MS) return
        if (numPings == 0) return

        val avgPing = (totalPingMs / numPings).toInt()
        val candidate = minOf(3 * avgPing, (1.3 * maxPingMs).toInt())
        timeoutMs = candidate.coerceIn(MIN_RESEND_TIMEOUT_MS, MAX_RESEND_TIMEOUT_MS)

        // Reset sample window
        numPings = 0
        totalPingMs = 0
        maxPingMs = 0
        lastResetTimeMs = nowMs
    }
}
