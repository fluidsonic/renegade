package ccr.net.flow

// C++: Adjust_Flow_If_Necessary() in wwnet/rhost.cpp (lines 336-486)
// Per-host flow controller: maintains a threshold priority and bandwidth multiplier.
//
// Threshold priority starts high and is dynamically lowered if the send queue is too large.
// Objects with priority below the threshold are not sent in that tick.

private const val INITIAL_THRESHOLD_PRIORITY = 0.0
private const val TP_INCREMENT = 0.01
private const val TP_TOLERANCE_DOWN = 0.1
private const val TP_TOLERANCE_UP = 0.05
private const val TP_MAX = 100.0
private const val TP_MIN = 0.0

class FlowController {
    // Threshold priority: objects below this are not sent
    var thresholdPriority: Double = INITIAL_THRESHOLD_PRIORITY
        private set

    // Bandwidth multiplier: scales packet volume this tick
    var bandwidthMultiplier: Float = 1.0f
        private set

    // Average object priority observed last tick
    var averageObjectPriority: Float = 0f

    // C++: Adjust_Flow_If_Necessary — called each network tick
    // targetBps: bytes/sec budget for this host
    // actualBpsSent: bytes/sec actually sent last interval
    fun adjust(targetBps: Int, actualBpsSent: Int, sampleTimeMs: Float) {
        if (targetBps <= 0 || sampleTimeMs <= 0f) return

        val targetBytesThisTick = targetBps * (sampleTimeMs / 1000f)
        val overrun = actualBpsSent / targetBytesThisTick.coerceAtLeast(1f)

        // Adjust bandwidth multiplier: if we're sending too much, throttle down
        bandwidthMultiplier = when {
            overrun > 1.1f -> (bandwidthMultiplier * 0.9f).coerceAtLeast(0.1f)
            overrun < 0.9f -> (bandwidthMultiplier * 1.1f).coerceAtMost(1.0f)
            else -> bandwidthMultiplier
        }

        // Adjust threshold priority based on whether we had excess or deficit
        thresholdPriority = when {
            overrun > 1.0f + TP_TOLERANCE_DOWN -> (thresholdPriority + TP_INCREMENT).coerceAtMost(TP_MAX)
            overrun < 1.0f - TP_TOLERANCE_UP   -> (thresholdPriority - TP_INCREMENT).coerceAtLeast(TP_MIN)
            else -> thresholdPriority
        }
    }

    fun shouldSend(objectPriority: Float): Boolean {
        return objectPriority.toDouble() >= thresholdPriority
    }
}
