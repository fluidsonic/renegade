package ccr.server.net

import ccr.math.AABox
import ccr.math.Vector3
import ccr.server.level.Sphere

// C++: CompositeRenderObjClass (ww3d2/composite.h)
// Encapsulates name/bounds bookkeeping shared by all composite (sub-object-containing) render objects.
open class CompositeRenderObjClass : RenderObjClass() {

    // C++: StringClass Name
    protected var name: String = "UNNAMED"

    // C++: StringClass BaseModelName
    var baseModelName: String? = null

    // C++: SphereClass ObjSphere
    protected var objSphere: Sphere = Sphere(Vector3(), 0f)

    // C++: AABoxClass ObjBox
    protected var objBox: AABox = AABox(Vector3(), Vector3())

    // -------------------------------------------------------------------------
    // Identification
    // -------------------------------------------------------------------------

    // C++: virtual const char* Get_Name() const
    override fun getName(): String = name

    // C++: virtual void Set_Name(const char* name)
    override fun setName(name: String) {
        this.name = name
    }

    // -------------------------------------------------------------------------
    // Bounding Volumes
    // -------------------------------------------------------------------------

    // C++: virtual void Get_Obj_Space_Bounding_Sphere(SphereClass& sphere) const { sphere = ObjSphere; }
    override fun getObjSpaceBoundingSphere(): Sphere = objSphere

    // C++: virtual void Get_Obj_Space_Bounding_Box(AABoxClass& box) const { box = ObjBox; }
    override fun getObjSpaceBoundingBox(): AABox = objBox

    // C++: virtual void Update_Obj_Space_Bounding_Volumes()
    override fun updateObjSpaceBoundingVolumes() {}

    // -------------------------------------------------------------------------
    // Poly count — iterate sub-objects
    // -------------------------------------------------------------------------

    // C++: virtual int Get_Num_Polys() const
    override fun getNumPolys(): Int {
        var total = 0
        for (i in 0 until getNumSubObjects()) {
            total += getSubObject(i)?.getNumPolys() ?: 0
        }
        return total
    }

    // -------------------------------------------------------------------------
    // Scene notifications — propagate to sub-objects
    // -------------------------------------------------------------------------

    // C++: virtual void Notify_Added(SceneClass* scene)
    override fun notifyAdded(scene: SceneClass) {
        for (i in 0 until getNumSubObjects()) {
            getSubObject(i)?.notifyAdded(scene)
        }
    }

    // C++: virtual void Notify_Removed(SceneClass* scene)
    override fun notifyRemoved(scene: SceneClass) {
        for (i in 0 until getNumSubObjects()) {
            getSubObject(i)?.notifyRemoved(scene)
        }
    }
}
