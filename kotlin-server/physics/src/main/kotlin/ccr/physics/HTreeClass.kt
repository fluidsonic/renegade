package ccr.physics

import ccr.math.Vector3

// C++: HTreeClass (ww3d2/htree.h) — skeleton/bone hierarchy
// Pivot data mirrors W3dPivot fields (excluding rotation, which is rendering-only).
data class HTreePivot(
    val name: String,
    val parentId: Int,
    val translation: Vector3,
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val rotZ: Float = 0f,
    val rotW: Float = 1f,
)

class HTreeClass(
    val name: String,
    val pivots: List<HTreePivot>,
) {
    // C++: HTreeClass::Num_Bones()
    fun getNumBones(): Int = pivots.size
}
