package ccr.server.audio

import ccr.math.Vector3

// C++: Sound3DClass : public AudibleSoundClass (WWAudio/Sound3D.h)
// A spatially placed 3D sound.
open class Sound3DClass : AudibleSoundClass() {

    // C++: bool m_TransformInitted
    var isTransformInitted: Boolean = false

    // C++: bool m_AutoCalcVelocity
    var autoCalcVelocity: Boolean = false

    // C++: Vector3 m_CurrentVelocity
    var currentVelocity: Vector3 = Vector3.ZERO

    // C++: float m_MaxVolRadius
    var maxVolRadius: Float = 0f

    // C++: bool m_IsStatic
    var isStatic: Boolean = false

    // C++: virtual void Set_Velocity(const Vector3&)
    open fun setVelocity(vel: Vector3) { currentVelocity = vel }

    // C++: void Make_Static()
    fun makeStatic() { isStatic = true }

    override fun asSound3DClass(): Sound3DClass = this
}
