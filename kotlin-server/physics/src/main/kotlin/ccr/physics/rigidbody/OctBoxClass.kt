package ccr.physics.rigidbody

import ccr.math.Vector3

// C++: OctBoxClass in wwphys/octbox.h
// Box-shaped rigid body using 8 corner contact points
open class OctBoxClass : RigidBodyClass() {

    val corners = Array(8) { Vector3.ZERO }

    fun setExtent(dx: Float, dy: Float, dz: Float) {
        val hx = dx * 0.5f
        val hy = dy * 0.5f
        val hz = dz * 0.5f
        corners[0] = Vector3(-hx, -hy, -hz)
        corners[1] = Vector3(hx, -hy, -hz)
        corners[2] = Vector3(-hx, hy, -hz)
        corners[3] = Vector3(hx, hy, -hz)
        corners[4] = Vector3(-hx, -hy, hz)
        corners[5] = Vector3(hx, -hy, hz)
        corners[6] = Vector3(-hx, hy, hz)
        corners[7] = Vector3(hx, hy, hz)
        computeInertia(dx, dy, dz)
    }

    fun getWorldCorner(index: Int): Vector3 {
        val local = corners[index]
        return transform.transformPoint(local)
    }
}
