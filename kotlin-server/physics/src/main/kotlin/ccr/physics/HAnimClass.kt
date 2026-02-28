package ccr.physics

// C++: HAnimClass (ww3d2/hanim.h) — animation asset
// Stores the data extracted from W3dAnimation (parsed from .w3d files).
// Channel data mirrors W3dAnimationChannel fields.
data class HAnimChannel(
    val firstFrame: Int,
    val lastFrame: Int,
    val vectorLen: Int,
    val flags: Int,
    val pivotIdx: Int,
    val data: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HAnimChannel) return false
        return firstFrame == other.firstFrame && lastFrame == other.lastFrame &&
               pivotIdx == other.pivotIdx && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = 31 * firstFrame + lastFrame + pivotIdx
}

class HAnimClass(
    val name: String,
    val hName: String,
    val numFrames: Int,
    val frameRate: Int,
    val channels: List<HAnimChannel>,
) {
    // C++: HAnimClass::Get_Total_Time() — total duration in seconds
    fun getTotalTime(): Float = if (frameRate > 0) numFrames.toFloat() / frameRate.toFloat() else 0f
}
