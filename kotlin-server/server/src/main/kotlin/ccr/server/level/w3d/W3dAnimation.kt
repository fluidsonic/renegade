package ccr.server.level.w3d

data class W3dAnimationChannel(
    val firstFrame: Int,
    val lastFrame: Int,
    val vectorLen: Int,
    val flags: Int,
    val pivotIdx: Int,
    val data: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is W3dAnimationChannel) return false
        return firstFrame == other.firstFrame && lastFrame == other.lastFrame &&
               pivotIdx == other.pivotIdx && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = 31 * firstFrame + lastFrame + pivotIdx
}

data class W3dAnimation(
    val name: String,
    val hierarchyName: String,
    val numFrames: Int,
    val frameRate: Int,
    val channels: List<W3dAnimationChannel>,
)
