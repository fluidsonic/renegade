package ccr.physics.static

import ccr.math.Matrix3D
import ccr.math.Vector3
import kotlin.math.abs
import kotlin.math.sin

open class ShakeableStaticPhysClass : StaticAnimPhysClass() {
    var shakeAmplitude: Float = 0f
    var shakeFrequency: Float = 10f
    var shakeElapsed: Float = 0f
    var isShaking: Boolean = false
    private var baseTransform: Matrix3D = Matrix3D.IDENTITY

    fun startShake(amplitude: Float, frequency: Float) {
        shakeAmplitude = amplitude
        shakeFrequency = frequency
        shakeElapsed = 0f
        isShaking = true
        baseTransform = transform
    }

    override fun timestep(dt: Float) {
        super.timestep(dt)
        if (!isShaking) return
        shakeElapsed += dt
        val decay = maxOf(0f, 1f - shakeElapsed * 2f) // decay to zero over 0.5s
        val offset = sin(shakeElapsed * shakeFrequency * 2f * Math.PI.toFloat()) * shakeAmplitude * decay
        transform = baseTransform.withTranslation(baseTransform.translation + Vector3(offset, 0f, 0f))
        if (decay <= 0f || abs(shakeAmplitude * decay) < 0.001f) {
            isShaking = false
            transform = baseTransform
        }
    }
}
