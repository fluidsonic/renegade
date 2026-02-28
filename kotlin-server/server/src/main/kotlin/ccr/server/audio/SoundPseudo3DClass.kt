package ccr.server.audio

import ccr.math.Vector3

// C++: SoundPseudo3DClass : public Sound3DClass (WWAudio/SoundPseudo3D.h)
// A pseudo-3D sound — positional but velocity is ignored.
open class SoundPseudo3DClass : Sound3DClass() {

    override fun asSoundPseudo3DClass(): SoundPseudo3DClass = this

    // C++: virtual void Set_Velocity(const Vector3&) — no-op for pseudo 3D
    override fun setVelocity(vel: Vector3) {}
}
