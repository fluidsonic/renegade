package ccr.physics

// C++: CompositeRenderObjClass : public RenderObjClass (ww3d2/composite.h)
// Base class for composite (multi-sub-object) render objects. Encapsulates common
// functionality for objects that contain sub-objects (name, bounding volumes, etc.).
open class CompositeRenderObjClass : RenderObjClass() {
    override fun asCompositeRenderObjClass(): CompositeRenderObjClass = this

    private var name: String = ""

    override fun getName(): String = name
    fun setName(name: String) { this.name = name }
}
