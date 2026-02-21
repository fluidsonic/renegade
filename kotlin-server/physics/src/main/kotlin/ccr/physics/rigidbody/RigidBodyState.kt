package ccr.physics.rigidbody

import ccr.math.Quaternion
import ccr.math.Vector3
import ccr.physics.ode.StateVector

data class RigidBodyState(
    val position: Vector3 = Vector3.ZERO,
    val orientation: Quaternion = Quaternion.IDENTITY,
    val linearMomentum: Vector3 = Vector3.ZERO,
    val angularMomentum: Vector3 = Vector3.ZERO,
) {
    fun toVector(out: StateVector) {
        out.add(position.x); out.add(position.y); out.add(position.z)
        out.add(orientation.x); out.add(orientation.y); out.add(orientation.z); out.add(orientation.w)
        out.add(linearMomentum.x); out.add(linearMomentum.y); out.add(linearMomentum.z)
        out.add(angularMomentum.x); out.add(angularMomentum.y); out.add(angularMomentum.z)
    }

    companion object {
        const val FLOAT_COUNT = 13

        fun fromVector(state: StateVector, startIndex: Int): RigidBodyState {
            var i = startIndex
            val pos = Vector3(state[i++], state[i++], state[i++])
            val ori = Quaternion(state[i++], state[i++], state[i++], state[i++])
            val linMom = Vector3(state[i++], state[i++], state[i++])
            val angMom = Vector3(state[i++], state[i++], state[i++])
            return RigidBodyState(pos, ori, linMom, angMom)
        }
    }

    fun lerp(other: RigidBodyState, t: Float) = RigidBodyState(
        position.lerp(other.position, t),
        orientation.slerp(other.orientation, t),
        linearMomentum.lerp(other.linearMomentum, t),
        angularMomentum.lerp(other.angularMomentum, t),
    )
}
