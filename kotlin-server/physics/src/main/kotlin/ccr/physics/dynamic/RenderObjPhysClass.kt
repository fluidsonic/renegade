package ccr.physics.dynamic

import ccr.physics.collision.RayCollisionTest

open class RenderObjPhysClass : DynamicPhysClass() {
    var renderModelName: String = ""

    override fun castRay(test: RayCollisionTest): Boolean = false // no collision mesh
}
