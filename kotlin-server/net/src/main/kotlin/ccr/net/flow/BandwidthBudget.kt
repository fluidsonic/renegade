package ccr.net.flow

// C++: bandwidth management in cConnection (wwnet/connect.h)
// Divides the total server bandwidth budget among connected clients.

class BandwidthBudget(
    // Total server bandwidth in bytes/sec
    var totalBps: Int = 1_500_000,
) {
    // Divide bandwidth equally among connected hosts
    fun perHostBps(connectedCount: Int): Int {
        if (connectedCount <= 0) return totalBps
        return totalBps / connectedCount
    }
}
