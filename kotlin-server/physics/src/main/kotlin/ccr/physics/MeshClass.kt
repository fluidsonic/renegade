package ccr.physics

import ccr.math.Vector3

// C++: MeshClass : public RenderObjClass (ww3d2/mesh.h)
open class MeshClass : RenderObjClass() {
    override val classId: Int get() = RenderObjClass.CLASSID_MESH
    override fun asMeshClass(): MeshClass = this
    fun getVertexCount(): Int = TODO("stub")
    // Returns array of vertex positions — each element is a Vector3
    fun getVertexArray(): Array<Vector3> = TODO("stub")
    // Returns array of triangles — each element is an IntArray of 3 vertex indices
    fun getPolygonArray(): Array<IntArray> = TODO("stub")
    fun getModel(): MeshClass = TODO("stub")  // returns the underlying model
    fun enableAlternateMaterialDescription(enable: Boolean): Unit = TODO("stub")
    fun peekModel(): MeshClass? = TODO("stub")
}
