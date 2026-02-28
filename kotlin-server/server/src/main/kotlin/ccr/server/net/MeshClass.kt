package ccr.server.net

import ccr.math.AABox
import ccr.math.Vector3
import ccr.server.level.Sphere

// C++: MeshClass : public RenderObjClass (ww3d2/mesh.h)
// Render object for rendering polygon meshes.
open class MeshClass : RenderObjClass() {

    // C++: MeshModelClass* Model
    var meshModel: Any? = null

    // C++: LightEnvironmentClass* LightEnvironment — no-op on server
    var lightEnvironment: Any? = null

    // C++: int BaseVertexOffset
    var baseVertexOffset: Int = 0

    // C++: MeshClass* NextVisibleSkin
    var nextVisibleSkin: MeshClass? = null

    // C++: unsigned MeshDebugId
    var meshDebugId: Int = 0

    // C++: bool IsDisabledByDebugger
    var debuggerDisabled: Boolean = false

    // -------------------------------------------------------------------------
    // Identification
    // -------------------------------------------------------------------------

    // C++: virtual int Class_ID() const { return CLASSID_MESH; }
    override val classId: Int get() = CLASSID_MESH

    // C++: virtual RenderObjClass* Clone() const
    override fun clone(): RenderObjClass = MeshClass().also { dst ->
        dst.meshModel = meshModel
        dst.baseVertexOffset = baseVertexOffset
        dst.meshDebugId = meshDebugId
        dst.debuggerDisabled = debuggerDisabled
    }

    // C++: virtual const char* Get_Name() const  — delegates to model (stub returns "UNNAMED")
    override fun getName(): String = "UNNAMED"

    // C++: virtual void Set_Name(const char* name)  — sets on model (no-op stub)
    override fun setName(name: String) {}

    // -------------------------------------------------------------------------
    // Bounding Volumes — empty on server (no model geometry loaded)
    // -------------------------------------------------------------------------

    override fun getObjSpaceBoundingSphere(): Sphere = Sphere(Vector3(), 0f)
    override fun getObjSpaceBoundingBox(): AABox = AABox(Vector3(), Vector3())

    // -------------------------------------------------------------------------
    // Scale / Material / Sort — no-op on server
    // -------------------------------------------------------------------------

    override fun scale(scale: Float) {}
    override fun scale(x: Float, y: Float, z: Float) {}
    override fun getMaterialInfo(): Any? = null
    override fun getSortLevel(): Int = 0
    override fun setSortLevel(level: Int) {}

    // -------------------------------------------------------------------------
    // MeshClass-specific interface
    // -------------------------------------------------------------------------

    // C++: MeshModelClass* Peek_Model()
    fun peekModel(): MeshClass? = this  // server stub: model IS the mesh

    // C++: MeshModelClass* Get_Model()
    fun getModel(): MeshClass = this  // server stub: model IS the mesh

    // C++: void Set_Next_Visible_Skin(MeshClass* next_visible)
    fun setNextVisibleSkin(next: MeshClass?) {
        nextVisibleSkin = next
    }

    // C++: MeshClass* Peek_Next_Visible_Skin()
    fun peekNextVisibleSkin(): MeshClass? = nextVisibleSkin

    // C++: void Set_Base_Vertex_Offset(int base)
    fun setBaseVertexOffset(base: Int) {
        baseVertexOffset = base
    }

    // C++: int Get_Base_Vertex_Offset()
    fun getBaseVertexOffset(): Int = baseVertexOffset

    // C++: unsigned Get_Debug_Id() const
    fun getDebugId(): Int = meshDebugId

    // C++: void Set_Debugger_Disable(bool b)
    fun setDebuggerDisable(b: Boolean) {
        debuggerDisabled = b
    }

    // C++: bool Is_Disabled_By_Debugger() const
    fun isDisabledByDebugger(): Boolean = debuggerDisabled

    // Alternate material description switching (no-op on server)
    fun enableAlternateMaterialDescription(enable: Boolean) {}

    // Vertex/polygon data — stubs for callers that need mesh geometry
    fun getVertexCount(): Int = 0
    fun getVertexArray(): Array<Vector3> = emptyArray()
    fun getPolygonArray(): Array<IntArray> = emptyArray()

    // -------------------------------------------------------------------------
    // As_* casts
    // -------------------------------------------------------------------------

    override fun asMeshClass(): MeshClass = this
}
