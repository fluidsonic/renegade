package ccr.server.level.lsd

data class BackgroundData(
    val skyTintFactor: Float = 1.0f,
    val cloudCover: Float = 0.0f,
    val cloudGloominess: Float = 0.0f,
    val lightningIntensity: Float = 0.0f,
    val lightningStartDistance: Float = 0.0f,
    val lightningEndDistance: Float = 0.0f,
    val lightningHeading: Float = 0.0f,
    val lightningDistribution: Float = 0.0f,
    val timeHours: Int = 12,
    val timeMinutes: Int = 0,
    val lightSourceType: Int = 0,
    val moonType: Int = 0,
)
