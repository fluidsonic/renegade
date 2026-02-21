package ccr.physics.dynamic

open class LightPhysClass : DecorationPhysClass() {
    var lightColor: FloatArray = floatArrayOf(1f, 1f, 1f)
    var lightIntensity: Float = 1.0f
    var lightRadius: Float = 5.0f
    var lightType: Int = 0 // 0=point, 1=spot
}
