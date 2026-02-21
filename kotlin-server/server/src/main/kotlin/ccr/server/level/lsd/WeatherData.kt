package ccr.server.level.lsd

data class WeatherData(
    val windHeading: Float = 0.0f,
    val windSpeed: Float = 0.0f,
    val windVariability: Float = 0.0f,
    val rainDensity: Float = 0.0f,
    val snowDensity: Float = 0.0f,
    val ashDensity: Float = 0.0f,
    val fogEnabled: Boolean = false,
    val fogStart: Float = 100.0f,
    val fogEnd: Float = 500.0f,
)
