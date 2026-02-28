package ccr.server.net

import ccr.math.AABox
import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.level.Sphere

// C++: RenderObjClass (ww3d2/rendobj.h)
// Base class for all objects that get rendered by WW3D.
open class RenderObjClass {

    companion object {
        // Class IDs — from RenderObjClass enum in rendobj.h (sequential from 0)
        const val CLASSID_MESH                 = 0
        const val CLASSID_HMODEL               = 1
        const val CLASSID_DISTLOD              = 2
        const val CLASSID_PREDLODGROUP         = 3
        const val CLASSID_TILEMAP              = 4
        const val CLASSID_IMAGE3D              = 5   // Obsolete
        const val CLASSID_LINE3D               = 6
        const val CLASSID_BITMAP2D             = 7   // Obsolete
        const val CLASSID_CAMERA               = 8
        const val CLASSID_DYNAMESH             = 9
        const val CLASSID_DYNASCREENMESH       = 10
        const val CLASSID_TEXTDRAW             = 11
        const val CLASSID_FOG                  = 12
        const val CLASSID_LAYERFOG             = 13
        const val CLASSID_LIGHT                = 14
        const val CLASSID_PARTICLEEMITTER      = 15
        const val CLASSID_PARTICLEBUFFER       = 16
        const val CLASSID_SCREENPOINTGROUP     = 17
        const val CLASSID_VIEWPOINTGROUP       = 18
        const val CLASSID_WORLDPOINTGROUP      = 19
        const val CLASSID_TEXT2D               = 20
        const val CLASSID_TEXT3D               = 21
        const val CLASSID_NULL                 = 22
        const val CLASSID_COLLECTION           = 23
        const val CLASSID_FLARE                = 24
        const val CLASSID_HLOD                 = 25
        const val CLASSID_AABOX                = 26
        const val CLASSID_OBBOX                = 27
        const val CLASSID_SEGLINE              = 28
        const val CLASSID_SPHERE               = 29
        const val CLASSID_RING                 = 30
        const val CLASSID_BOUNDFOG             = 31
        const val CLASSID_DAZZLE               = 32
        const val CLASSID_SOUND                = 33
        const val CLASSID_SEGLINETRAIL         = 34
        const val CLASSID_LAND                 = 35
        const val CLASSID_RENEGADE_TERRAIN     = 36
        const val CLASSID_LAST                 = 0x0000FFFF
        const val CLASSID_UNKNOWN              = -1  // 0xFFFFFFFF as Int

        // Bit flags — from protected enum in rendobj.h
        const val COLLISION_TYPE_0              = 0x00000001
        const val COLLISION_TYPE_1              = 0x00000002
        const val COLLISION_TYPE_2              = 0x00000004
        const val COLLISION_TYPE_3              = 0x00000008
        const val COLLISION_TYPE_4              = 0x00000010
        const val COLLISION_TYPE_5              = 0x00000020
        const val COLLISION_TYPE_6              = 0x00000040
        const val COLLISION_TYPE_7              = 0x00000080
        const val COLLISION_TYPE_ALL            = 0x000000FF
        const val COLLISION_TYPE_MASK           = 0x000000FF
        const val IS_VISIBLE                    = 0x00000100
        const val IS_NOT_HIDDEN                 = 0x00000200
        const val IS_NOT_ANIMATION_HIDDEN       = 0x00000400
        const val IS_FORCE_VISIBLE              = 0x00000800
        const val BOUNDING_VOLUMES_VALID        = 0x00002000
        const val IS_TRANSLUCENT                = 0x00004000
        const val SUBOBJS_MATCH_LOD             = 0x00010000
        const val SUBOBJ_TRANSFORMS_DIRTY       = 0x00020000
        const val HAS_USER_LIGHTING             = 0x00040000

        const val IS_REALLY_VISIBLE    = IS_VISIBLE or IS_NOT_HIDDEN or IS_NOT_ANIMATION_HIDDEN
        const val IS_NOT_HIDDEN_AT_ALL = IS_NOT_HIDDEN or IS_NOT_ANIMATION_HIDDEN
        const val DEFAULT_BITS         = COLLISION_TYPE_ALL or IS_NOT_HIDDEN or IS_NOT_ANIMATION_HIDDEN

        // Animation modes — from RenderObjClass enum in rendobj.h
        const val ANIM_MODE_MANUAL = 0
        const val ANIM_MODE_LOOP   = 1
        const val ANIM_MODE_ONCE   = 2
    }

    // C++: uint32_t Bits
    var bits: Int = DEFAULT_BITS

    // C++: Matrix3D Transform
    var transform: Matrix3D = Matrix3D()

    // C++: mutable SphereClass CachedBoundingSphere
    protected var cachedBoundingSphere: Sphere = Sphere(Vector3(), 0f)

    // C++: mutable AABox CachedBoundingBox
    protected var cachedBoundingBox: AABox = AABox(Vector3(), Vector3())

    // C++: float NativeScreenSize
    var nativeScreenSize: Float = 0f

    // C++: mutable bool IsTransformIdentity
    protected var isTransformIdentity: Boolean = true

    // C++: SceneClass* Scene
    var scene: SceneClass? = null

    // C++: RenderObjClass* Container
    var container: RenderObjClass? = null

    // C++: void* User_Data
    var userData: Any? = null

    // -------------------------------------------------------------------------
    // Cloning and Identification
    // -------------------------------------------------------------------------

    // C++: virtual RenderObjClass* Clone() const = 0
    open fun clone(): RenderObjClass = throw UnsupportedOperationException("pure virtual")

    // C++: virtual int Class_ID() const { return CLASSID_UNKNOWN; }
    open val classId: Int get() = CLASSID_UNKNOWN

    // C++: virtual const char* Get_Name() const { return "UNNAMED"; }
    open fun getName(): String = "UNNAMED"

    // C++: virtual void Set_Name(const char* name) {}
    open fun setName(name: String) {}

    // C++: virtual int Get_Num_Polys() const { return 0; }
    open fun getNumPolys(): Int = 0

    // -------------------------------------------------------------------------
    // Rendering (no-op on server — no renderer)
    // -------------------------------------------------------------------------

    open fun render() {}
    open fun specialRender() {}
    open fun onFrameUpdate() {}
    open fun restart() {}

    // -------------------------------------------------------------------------
    // Scene Graph
    // -------------------------------------------------------------------------

    // C++: virtual void Add(SceneClass* scene)
    open fun add(scene: SceneClass) {
        this.scene = scene
    }

    // C++: virtual void Remove()
    open fun remove() {
        this.scene = null
    }

    // C++: virtual SceneClass* Peek_Scene()
    fun peekScene(): SceneClass? = scene

    // C++: virtual void Set_Container(RenderObjClass* con)
    fun setContainer(obj: RenderObjClass?) {
        container = obj
    }

    // C++: virtual RenderObjClass* Get_Container() const
    fun getContainer(): RenderObjClass? = container

    // C++: virtual void Validate_Transform() const — no-op on server
    open fun validateTransform() {}

    // C++: virtual void Set_Transform(const Matrix3D& m)
    open fun setTransform(tm: Matrix3D) {
        transform = tm
        isTransformIdentity = (tm == Matrix3D.IDENTITY)
        invalidateCachedBoundingVolumes()
    }

    // C++: virtual void Set_Position(const Vector3& v)
    open fun setPosition(pos: Vector3) {
        transform = transform.copy(m03 = pos.x, m13 = pos.y, m23 = pos.z)
        isTransformIdentity = false
        invalidateCachedBoundingVolumes()
    }

    // C++: const Matrix3D& Get_Transform() const  (calls Validate_Transform first)
    fun getTransform(): Matrix3D {
        validateTransform()
        return transform
    }

    // C++: Vector3 Get_Position() const
    fun getPosition(): Vector3 = transform.translation

    // C++: Vector3 Get_Translation (convenience alias for translation component)
    fun getTranslation(): Vector3 = transform.translation

    // C++: virtual void Notify_Added(SceneClass* scene) {}
    open fun notifyAdded(scene: SceneClass) {}

    // C++: virtual void Notify_Removed(SceneClass* scene) {}
    open fun notifyRemoved(scene: SceneClass) {}

    // C++: virtual bool Is_In_Scene() { return Scene != NULL; }
    fun isInScene(): Boolean = scene != null

    // -------------------------------------------------------------------------
    // Sub-objects
    // -------------------------------------------------------------------------

    // C++: virtual int Get_Num_Sub_Objects() const { return 0; }
    open fun getNumSubObjects(): Int = 0

    // C++: virtual RenderObjClass* Get_Sub_Object(int index) const { return NULL; }
    open fun getSubObject(index: Int): RenderObjClass? = null

    // C++: virtual RenderObjClass* Get_Sub_Object_By_Name(const char* name) const
    open fun getSubObjectByName(name: String): RenderObjClass? = null

    // C++: virtual int Add_Sub_Object(RenderObjClass* subobj) { return 0; }
    open fun addSubObject(obj: RenderObjClass): Int = 0

    // C++: Add_Render_Object convenience (used in some callers to attach children)
    open fun addRenderObject(child: RenderObjClass) {}

    // C++: virtual int Remove_Sub_Object(RenderObjClass* robj) { return 0; }
    open fun removeSubObject(obj: RenderObjClass): Int = 0

    // C++: virtual int Get_Num_Sub_Objects_On_Bone(int boneindex) const { return 0; }
    open fun getNumSubObjectsOnBone(boneIndex: Int): Int = 0

    // C++: virtual RenderObjClass* Get_Sub_Object_On_Bone(int index, int boneindex) const { return NULL; }
    open fun getSubObjectOnBone(index: Int, boneIndex: Int): RenderObjClass? = null

    // C++: virtual int Get_Sub_Object_Bone_Index(RenderObjClass* subobj) const { return 0; }
    open fun getSubObjectBoneIndex(subObj: RenderObjClass?): Int = 0

    // C++: virtual int Add_Sub_Object_To_Bone(RenderObjClass* subobj, int bone_index)
    open fun addSubObjectToBone(subObj: RenderObjClass, boneIndex: Int): Int = 0

    // C++: virtual int Add_Sub_Object_To_Bone(RenderObjClass* subobj, const char* bname)
    open fun addSubObjectToBone(subObj: RenderObjClass, boneName: String): Int = 0

    // C++: virtual int Remove_Sub_Objects_From_Bone(const char* bname)
    open fun removeSubObjectsFromBone(boneName: String): Int = 0

    // C++: virtual void Update_Sub_Object_Transforms()
    open fun updateSubObjectTransforms() {}

    // -------------------------------------------------------------------------
    // Hierarchical Animation
    // -------------------------------------------------------------------------

    // C++: virtual void Set_Animation(void) {}  [rendobj.h ~line 227]
    open fun setAnimation() {}

    // C++: virtual void Set_Animation(HAnimClass* motion, float frame, int anim_mode = ANIM_MODE_MANUAL) {}
    open fun setAnimation(motion: HAnimClass?, frame: Float, mode: Int) {}

    // Convenience overload with ANIM_MODE_MANUAL default
    open fun setAnimation(motion: HAnimClass?, frame: Float) {
        setAnimation(motion, frame, ANIM_MODE_MANUAL)
    }

    // C++: virtual void Set_Animation(HAnimClass* motion0, float frame0, HAnimClass* motion1, float frame1, float percentage) {}
    open fun setAnimation(motion0: HAnimClass?, frame0: Float, motion1: HAnimClass?, frame1: Float, percentage: Float) {}

    // C++: virtual void Set_Animation(HAnimComboClass* anim_combo) {}
    open fun setAnimation(combo: HAnimComboClass?) {}

    // C++: virtual HAnimClass* Peek_Animation() { return NULL; }
    open fun peekAnimation(): HAnimClass? = null

    // C++: virtual int Get_Num_Bones() { return 0; }
    open fun getNumBones(): Int = 0

    // C++: virtual const char* Get_Bone_Name(int bone_index) { return NULL; }
    open fun getBoneName(boneIndex: Int): String? = null

    // C++: virtual int Get_Bone_Index(const char* bonename) { return 0; }
    open fun getBoneIndex(name: String): Int = 0

    // C++: virtual const Matrix3D& Get_Bone_Transform(int boneindex) { return Get_Transform(); }
    // Base class returns the object's own transform (not identity).
    open fun getBoneTransform(boneIndex: Int): Matrix3D = transform

    // C++: virtual const Matrix3D& Get_Bone_Transform(const char* bonename)
    open fun getBoneTransform(name: String): Matrix3D = getBoneTransform(getBoneIndex(name))

    // C++: virtual void Capture_Bone(int bindex) {}
    open fun captureBone(boneIndex: Int) {}

    // C++: virtual void Release_Bone(int bindex) {}
    open fun releaseBone(boneIndex: Int) {}

    // C++: virtual bool Is_Bone_Captured(int bindex) const { return false; }
    open fun isBoneCaptured(boneIndex: Int): Boolean = false

    // C++: virtual void Control_Bone(int bindex, const Matrix3D& objtm, bool world_space_translation = false) {}
    open fun controlBone(boneIndex: Int, objtm: Matrix3D, worldSpaceTranslation: Boolean = false) {}

    // C++: virtual const HTreeClass* Get_HTree() const { return NULL; }
    open fun getHTree(): HTreeClass? = null

    // -------------------------------------------------------------------------
    // Collision Detection (all no-op on server)
    // -------------------------------------------------------------------------

    open fun castRay(raytest: Any): Boolean = false
    open fun castAABox(boxtest: Any): Boolean = false
    open fun castOBBox(boxtest: Any): Boolean = false
    open fun intersectAABox(boxtest: Any): Boolean = false
    open fun intersectOBBox(boxtest: Any): Boolean = false

    // -------------------------------------------------------------------------
    // Bounding Volumes
    // -------------------------------------------------------------------------

    // C++: virtual const SphereClass& Get_Bounding_Sphere() const  (lazy cache)
    fun getBoundingSphere(): Sphere {
        if ((bits and BOUNDING_VOLUMES_VALID) == 0) {
            updateCachedBoundingVolumes()
        }
        return cachedBoundingSphere
    }

    // C++: virtual const AABox& Get_Bounding_Box() const  (lazy cache)
    fun getBoundingBox(): AABox {
        if ((bits and BOUNDING_VOLUMES_VALID) == 0) {
            updateCachedBoundingVolumes()
        }
        return cachedBoundingBox
    }

    // C++: virtual void Get_Obj_Space_Bounding_Sphere(SphereClass& sphere) const
    open fun getObjSpaceBoundingSphere(): Sphere = Sphere(Vector3(), 0f)

    // C++: virtual void Get_Obj_Space_Bounding_Box(AABox& box) const
    open fun getObjSpaceBoundingBox(): AABox = AABox(Vector3(), Vector3())

    // C++: virtual void Update_Obj_Space_Bounding_Volumes() {}
    open fun updateObjSpaceBoundingVolumes() {}

    // C++: virtual void Update_Cached_Bounding_Volumes() const  (no-op default)
    protected open fun updateCachedBoundingVolumes() {}

    // C++: void Invalidate_Cached_Bounding_Volumes() const
    protected fun invalidateCachedBoundingVolumes() {
        bits = bits and BOUNDING_VOLUMES_VALID.inv()
    }

    // C++: void Validate_Cached_Bounding_Volumes() const
    protected fun validateCachedBoundingVolumes() {
        bits = bits or BOUNDING_VOLUMES_VALID
    }

    // -------------------------------------------------------------------------
    // Visibility
    // -------------------------------------------------------------------------

    // C++: virtual void Set_Visible(int onoff)
    open fun setVisible(vis: Boolean) {
        bits = if (vis) bits or IS_VISIBLE else bits and IS_VISIBLE.inv()
    }

    // C++: virtual int Is_Visible() const
    open fun isVisible(): Boolean = (bits and IS_VISIBLE) != 0

    // C++: virtual void Set_Hidden(int onoff)
    // IS_NOT_HIDDEN means "not hidden" — Set_Hidden(true) clears IS_NOT_HIDDEN
    open fun setHidden(hidden: Boolean) {
        bits = if (hidden) bits and IS_NOT_HIDDEN.inv() else bits or IS_NOT_HIDDEN
    }

    // C++: virtual int Is_Hidden() const
    open fun isHidden(): Boolean = (bits and IS_NOT_HIDDEN) == 0

    // C++: virtual void Set_Animation_Hidden(int onoff)
    open fun setAnimationHidden(hidden: Boolean) {
        bits = if (hidden) bits and IS_NOT_ANIMATION_HIDDEN.inv() else bits or IS_NOT_ANIMATION_HIDDEN
    }

    // C++: virtual int Is_Animation_Hidden() const
    open fun isAnimationHidden(): Boolean = (bits and IS_NOT_ANIMATION_HIDDEN) == 0

    // C++: virtual void Set_Force_Visible(int onoff)
    open fun setForceVisible(force: Boolean) {
        bits = if (force) bits or IS_FORCE_VISIBLE else bits and IS_FORCE_VISIBLE.inv()
    }

    // C++: virtual int Is_Force_Visible() const
    open fun isForceVisible(): Boolean = (bits and IS_FORCE_VISIBLE) != 0

    // C++: virtual int Is_Really_Visible()
    open fun isReallyVisible(): Boolean = (bits and IS_REALLY_VISIBLE) == IS_REALLY_VISIBLE

    // C++: virtual int Is_Not_Hidden_At_All()
    open fun isNotHiddenAtAll(): Boolean = (bits and IS_NOT_HIDDEN_AT_ALL) == IS_NOT_HIDDEN_AT_ALL

    // C++: virtual void Set_Translucent(int onoff)
    open fun setTranslucent(t: Boolean) {
        bits = if (t) bits or IS_TRANSLUCENT else bits and IS_TRANSLUCENT.inv()
    }

    // C++: virtual int Is_Translucent() const
    open fun isTranslucent(): Boolean = (bits and IS_TRANSLUCENT) != 0

    // -------------------------------------------------------------------------
    // Collision Type
    // -------------------------------------------------------------------------

    open fun setCollisionType(type: Int) {
        bits = (bits and COLLISION_TYPE_MASK.inv()) or ((type and COLLISION_TYPE_MASK) or COLLISION_TYPE_ALL)
    }

    open fun getCollisionType(): Int = bits and COLLISION_TYPE_MASK

    // -------------------------------------------------------------------------
    // LOD matching
    // -------------------------------------------------------------------------

    // C++: void Set_Sub_Objects_Match_LOD(int onoff)
    fun setSubObjectsMatchLod(onoff: Boolean) {
        bits = if (onoff) bits or SUBOBJS_MATCH_LOD else bits and SUBOBJS_MATCH_LOD.inv()
    }

    fun isSubObjectsMatchLodEnabled(): Boolean = (bits and SUBOBJS_MATCH_LOD) != 0

    fun setSubObjectTransformsDirty(onoff: Boolean) {
        bits = if (onoff) bits or SUBOBJ_TRANSFORMS_DIRTY else bits and SUBOBJ_TRANSFORMS_DIRTY.inv()
    }

    fun areSubObjectTransformsDirty(): Boolean = (bits and SUBOBJ_TRANSFORMS_DIRTY) != 0

    // -------------------------------------------------------------------------
    // Scale / Sort / Material
    // -------------------------------------------------------------------------

    open fun scale(scale: Float) {}
    open fun scale(x: Float, y: Float, z: Float) {}
    open fun getSortLevel(): Int = 0
    open fun setSortLevel(level: Int) {}
    open fun getMaterialInfo(): Any? = null

    open fun setUserData(data: Any?) { userData = data }
    open fun getUserData(): Any? = userData

    fun getNativeScreenSize(): Float = nativeScreenSize
    fun setNativeScreenSize(size: Float) { nativeScreenSize = size }

    // C++: virtual bool Is_Complete() { return false; }
    open fun isComplete(): Boolean = false

    // C++: enable_remove_on_complete (animation event handling)
    open fun enableRemoveOnComplete(enable: Boolean) {}

    // -------------------------------------------------------------------------
    // Save / Load — stubs on server (no real persist system)
    // -------------------------------------------------------------------------

    // C++: virtual const PersistFactoryClass& Get_Factory() const
    open fun getFactory(): RenderObjFactoryClass = RenderObjFactoryClass()

    open fun save(chunkSave: ChunkSaveClass) {}
    open fun load(chunkLoad: ChunkLoadClass) {}
    open fun buildDependencyList(list: Any): Boolean = false
    open fun buildTextureList(list: Any): Boolean = false

    // -------------------------------------------------------------------------
    // As_* casts (mirrors C++ as_*)
    // -------------------------------------------------------------------------

    open fun asMeshClass(): MeshClass? = null
    open fun asAnimatable3DObjClass(): Animatable3DObjClass? = null

    // -------------------------------------------------------------------------
    // Protected helpers
    // -------------------------------------------------------------------------

    protected open fun updateSubObjectBits() {}
    protected open fun addDependenciesToList(list: Any) {}
}

// C++: SceneClass — the WW3D render scene; server has no actual rendering
open class SceneClass

// C++: PersistFactoryClass specialisation for RenderObjClass (ww3d2/rendobj.h)
// Server: no actual save/load system — stub implementation.
class RenderObjFactoryClass {
    val chunkId: UInt = 0u
    fun save(csave: ChunkSaveClass, obj: RenderObjClass): Unit = Unit
    fun load(cload: ChunkLoadClass): RenderObjClass? = null
}
