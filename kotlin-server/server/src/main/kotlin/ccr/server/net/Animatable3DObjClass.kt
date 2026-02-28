package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3

// C++: Animatable3DObjClass : public CompositeRenderObjClass (ww3d2/animobj.h)
// Implements hierarchical animation state for WW3D composite objects.
open class Animatable3DObjClass : CompositeRenderObjClass() {

    companion object {
        // C++: anonymous enum inside Animatable3DObjClass  (stored as CurMotionMode)
        const val MOTION_NONE          = 0
        const val MOTION_BASE_POSE     = 1
        const val MOTION_SINGLE_ANIM   = 2
        const val MOTION_DOUBLE_ANIM   = 3
        const val MOTION_MULTIPLE_ANIM = 4
    }

    // C++: mutable bool IsTreeValid
    protected var isTreeValid: Boolean = false

    // C++: HTreeClass* HTree
    protected var hTree: HTreeClass? = null

    // C++: int CurMotionMode
    protected var curMotionMode: Int = MOTION_NONE

    // Single-anim state (C++: ModeAnim union member)
    protected var motion: HAnimClass? = null
    protected var curFrame: Float = 0f
    protected var prevFrame: Float = 0f
    protected var animMode: Int = 0
    protected var lastSyncTime: Int = 0

    // Double-anim state (C++: ModeInterp union member)
    protected var motion0: HAnimClass? = null
    protected var frame0: Float = 0f
    protected var prevFrame0: Float = 0f
    protected var motion1: HAnimClass? = null
    protected var frame1: Float = 0f
    protected var prevFrame1: Float = 0f
    protected var percentage: Float = 0f

    // Multiple-anim state (C++: ModeCombo union member)
    protected var animCombo: HAnimComboClass? = null

    // -------------------------------------------------------------------------
    // Scene Graph — invalidate tree on transform change
    // -------------------------------------------------------------------------

    // C++: virtual void Set_Transform(const Matrix3D& m)
    override fun setTransform(tm: Matrix3D) {
        super.setTransform(tm)
        isTreeValid = false
    }

    // C++: virtual void Set_Position(const Vector3& v)
    override fun setPosition(pos: Vector3) {
        super.setPosition(pos)
        isTreeValid = false
    }

    // -------------------------------------------------------------------------
    // Animation
    // -------------------------------------------------------------------------

    // C++: virtual void Set_Animation(HAnimClass* motion, float frame, int anim_mode = ANIM_MODE_MANUAL)
    override fun setAnimation(motion: HAnimClass?, frame: Float, mode: Int) {
        curMotionMode = MOTION_SINGLE_ANIM
        this.motion = motion
        this.curFrame = frame
        this.animMode = mode
        isTreeValid = false
    }

    // C++: overload with ANIM_MODE_MANUAL default
    override fun setAnimation(motion: HAnimClass?, frame: Float) {
        setAnimation(motion, frame, ANIM_MODE_MANUAL)
    }

    // C++: virtual void Set_Animation(HAnimClass* motion0, float frame0, HAnimClass* motion1, float frame1, float percentage)
    override fun setAnimation(motion0: HAnimClass?, frame0: Float, motion1: HAnimClass?, frame1: Float, percentage: Float) {
        curMotionMode = MOTION_DOUBLE_ANIM
        this.motion0 = motion0
        this.frame0 = frame0
        this.motion1 = motion1
        this.frame1 = frame1
        this.percentage = percentage
        isTreeValid = false
    }

    // C++: virtual void Set_Animation(HAnimComboClass* anim_combo)
    override fun setAnimation(combo: HAnimComboClass?) {
        curMotionMode = MOTION_MULTIPLE_ANIM
        animCombo = combo
        isTreeValid = false
    }

    // C++: virtual HAnimClass* Peek_Animation()
    // Returns the single-anim motion (matches C++ which returns ModeAnim.Motion)
    override fun peekAnimation(): HAnimClass? = motion

    // C++: virtual bool Is_Animation_Complete() const
    open fun isAnimationComplete(): Boolean {
        val m = motion ?: return true
        if (curMotionMode != MOTION_SINGLE_ANIM) return true
        return curFrame >= m.numFrames - 1
    }

    // C++: float Compute_Current_Frame() const  (protected)
    protected open fun computeCurrentFrame(): Float = curFrame

    // -------------------------------------------------------------------------
    // Bone interface — delegate to HTree
    // -------------------------------------------------------------------------

    // C++: virtual int Get_Num_Bones()
    override fun getNumBones(): Int = hTree?.getNumBones() ?: 0

    // C++: virtual int Get_Bone_Index(const char* bonename)
    override fun getBoneIndex(name: String): Int = hTree?.getBoneIndex(name) ?: 0

    // C++: virtual const char* Get_Bone_Name(int bone_index)
    override fun getBoneName(boneIndex: Int): String? = hTree?.getBoneName(boneIndex)

    // C++: virtual const Matrix3D& Get_Bone_Transform(int boneindex)
    // Server-side: transform hierarchy is not computed — return identity
    override fun getBoneTransform(boneIndex: Int): Matrix3D = Matrix3D()

    // C++: virtual const HTreeClass* Get_HTree() const { return HTree; }
    override fun getHTree(): HTreeClass? = hTree

    // C++: virtual void Set_HTree(HTreeClass* htree)
    fun setHTree(tree: HTreeClass?) {
        hTree = tree
        isTreeValid = false
    }

    // -------------------------------------------------------------------------
    // Sub-object transforms — no-op on server (complex in C++, not needed server-side)
    // -------------------------------------------------------------------------

    // C++: virtual void Update_Sub_Object_Transforms()
    override fun updateSubObjectTransforms() {}

    // -------------------------------------------------------------------------
    // Hierarchy validity helpers (mirrors C++ inlines in animobj.h)
    // -------------------------------------------------------------------------

    // C++: bool Is_Hierarchy_Valid() const { return IsTreeValid; }
    protected fun isHierarchyValid(): Boolean = isTreeValid

    // C++: void Set_Hierarchy_Valid(bool onoff) const { IsTreeValid = onoff; }
    protected fun setHierarchyValid(onoff: Boolean) {
        isTreeValid = onoff
    }

    // -------------------------------------------------------------------------
    // As_* casts
    // -------------------------------------------------------------------------

    override fun asAnimatable3DObjClass(): Animatable3DObjClass = this
}

// C++: HAnimComboClass — combination of multiple animations layered together; stub on server
class HAnimComboClass
