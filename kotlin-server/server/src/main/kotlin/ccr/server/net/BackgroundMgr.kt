package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: BackgroundParameterClass — holds normal and override ramp targets/durations
// Used by BackgroundMgrClass and WeatherMgrClass for each parameter.
data class BackgroundParameter(
    val normalTarget: Float = 0f,
    val normalDuration: Float = 0f,
    val overrideTarget: Float = 0f,
    val overrideDuration: Float = 0f,
)

// C++: BackgroundParameterClass export — writes 4 floats in order
fun BackgroundParameter.exportTo(packet: BitStream) {
    packet.addFloat(normalTarget)
    packet.addFloat(normalDuration)
    packet.addFloat(overrideTarget)
    packet.addFloat(overrideDuration)
}

// C++: BackgroundMgrClass (backgroundmgr.h/.cpp) — manages sky/lighting/cloud effects.
// Uses well-known networkId NETID_SERVER_BACKGROUND; Get_Network_Class_ID() returns 0 (default).
// Set_Delete_Pending is a no-op (singleton manager).
//
// Export_Rare format:
//   For each of 8 params (SKY_TINT_FACTOR, CLOUD_COVER, CLOUD_GLOOMINESS,
//   LIGHTNING_INTENSITY, LIGHTNING_START_DISTANCE, LIGHTNING_END_DISTANCE,
//   LIGHTNING_HEADING, LIGHTNING_DISTRIBUTION):
//     normalTarget: float
//     normalDuration: float
//     overrideTarget: float
//     overrideDuration: float
//   CloudOverrideCount: uint (int)
//   LightningOverrideCount: uint (int)
//   SkyTintOverrideCount: uint (int)
class BackgroundMgr(
    val skyTintFactor: BackgroundParameter = BackgroundParameter(),
    val cloudCover: BackgroundParameter = BackgroundParameter(),
    val cloudGloominess: BackgroundParameter = BackgroundParameter(),
    val lightningIntensity: BackgroundParameter = BackgroundParameter(),
    val lightningStartDistance: BackgroundParameter = BackgroundParameter(),
    val lightningEndDistance: BackgroundParameter = BackgroundParameter(),
    val lightningHeading: BackgroundParameter = BackgroundParameter(),
    val lightningDistribution: BackgroundParameter = BackgroundParameter(),
    val cloudOverrideCount: Int = 0,
    val lightningOverrideCount: Int = 0,
    val skyTintOverrideCount: Int = 0,
) : NetworkObject() {
    override val networkClassId: Int = 0
    val creationDirtyBit = BIT_RARE
    override fun delete() {}
    override fun setDeletePending() {}  // singleton manager, never deleted

    override fun exportRare(packet: BitStream) {
        skyTintFactor.exportTo(packet)
        cloudCover.exportTo(packet)
        cloudGloominess.exportTo(packet)
        lightningIntensity.exportTo(packet)
        lightningStartDistance.exportTo(packet)
        lightningEndDistance.exportTo(packet)
        lightningHeading.exportTo(packet)
        lightningDistribution.exportTo(packet)
        packet.addInt(cloudOverrideCount)
        packet.addInt(lightningOverrideCount)
        packet.addInt(skyTintOverrideCount)
    }
}
