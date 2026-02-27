package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: WeatherMgrClass (WeatherMgr.h/.cpp) — manages wind/rain/snow/ash weather effects.
// Uses well-known networkId NETID_SERVER_WEATHER; Get_Network_Class_ID() returns 0 (default).
// Set_Delete_Pending is a no-op (singleton manager).
//
// Export_Rare format:
//   For each of 6 params (WIND_HEADING, WIND_SPEED, WIND_VARIABILITY,
//   RAIN_DENSITY, SNOW_DENSITY, ASH_DENSITY):
//     normalTarget: float
//     normalDuration: float
//     overrideTarget: float
//     overrideDuration: float
//   WindOverrideCount: uint (int)
//   PrecipitationOverrideCount: uint (int)
//
// Reuses BackgroundParameter (defined in BackgroundMgr.kt) — same structure as WeatherParameterClass.
class WeatherMgr(
    val windHeading: BackgroundParameter = BackgroundParameter(),
    val windSpeed: BackgroundParameter = BackgroundParameter(),
    val windVariability: BackgroundParameter = BackgroundParameter(),
    val rainDensity: BackgroundParameter = BackgroundParameter(),
    val snowDensity: BackgroundParameter = BackgroundParameter(),
    val ashDensity: BackgroundParameter = BackgroundParameter(),
    val windOverrideCount: Int = 0,
    val precipitationOverrideCount: Int = 0,
) : NetworkObject() {
    override val networkClassId: Int = 0
    override fun delete() {}
    override fun setDeletePending() {}  // singleton manager, never deleted

    override fun exportRare(packet: BitStream) {
        listOf(windHeading, windSpeed, windVariability, rainDensity, snowDensity, ashDensity)
            .forEach { it.exportTo(packet) }
        packet.addInt(windOverrideCount)
        packet.addInt(precipitationOverrideCount)
    }
}
