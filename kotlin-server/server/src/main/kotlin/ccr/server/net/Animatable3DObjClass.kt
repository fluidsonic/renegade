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

    // C++: mutable bool IsTreeValid — starts false (C++: IsTreeValid(0) in constructor)
    protected var isTreeValid: Boolean = false

    // C++: HTreeClass* HTree
    // @JvmField suppresses auto-generated getHTree()/setHTree() to avoid clashing
    // with the explicit override fun getHTree() and fun setHTree() below.
    @JvmField protected var hTree: HTreeClass? = null

    // C++: int CurMotionMode — C++ constructor sets CurMotionMode(BASE_POSE)
    protected var curMotionMode: Int = MOTION_BASE_POSE

    // Single-anim state (C++: ModeAnim union member)
    protected var motion: HAnimClass? = null
    protected var curFrame: Float = 0f
    protected var prevFrame: Float = 0f
    protected var animMode: Int = ANIM_MODE_MANUAL
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
    // Release — clears held animation references (mirrors C++ Release())
    // C++ Release() releases Add_Ref'd HAnimClass pointers; server omits ref-counting.
    // -------------------------------------------------------------------------

    // C++: void Release() — protected, clears current motion refs based on CurMotionMode
    protected open fun release() {
        when (curMotionMode) {
            MOTION_SINGLE_ANIM -> {
                motion = null
            }
            MOTION_DOUBLE_ANIM -> {
                motion0 = null
                motion1 = null
            }
            // BASE_POSE, MULTIPLE_ANIM, NONE — nothing to release
        }
    }

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

    // C++: virtual void Set_Animation(void) — resets to base pose
    override fun setAnimation() {
        release()
        curMotionMode = MOTION_BASE_POSE
        isTreeValid = false
    }

    // C++: virtual void Set_Animation(HAnimClass* motion, float frame, int anim_mode = ANIM_MODE_MANUAL)
    // C++ logic: if motion != null: Add_Ref; Release; set SINGLE_ANIM state
    //            else: set BASE_POSE; Release
    override fun setAnimation(motion: HAnimClass?, frame: Float, mode: Int) {
        if (motion != null) {
            // C++: motion->Add_Ref() before Release() in case it's the same one
            release()
            curMotionMode = MOTION_SINGLE_ANIM
            this.motion = motion
            prevFrame = curFrame
            curFrame = frame
            animMode = mode
        } else {
            curMotionMode = MOTION_BASE_POSE
            release()
        }
        isTreeValid = false
    }

    // C++: overload with ANIM_MODE_MANUAL default
    override fun setAnimation(motion: HAnimClass?, frame: Float) {
        setAnimation(motion, frame, ANIM_MODE_MANUAL)
    }

    // C++: virtual void Set_Animation(HAnimClass* motion0, float frame0, HAnimClass* motion1, float frame1, float percentage)
    override fun setAnimation(motion0: HAnimClass?, frame0: Float, motion1: HAnimClass?, frame1: Float, percentage: Float) {
        release()
        curMotionMode = MOTION_DOUBLE_ANIM
        this.motion0 = motion0
        this.motion1 = motion1
        prevFrame0 = this.frame0
        prevFrame1 = this.frame1
        this.frame0 = frame0
        this.frame1 = frame1
        this.percentage = percentage
        isTreeValid = false
    }

    // C++: virtual void Set_Animation(HAnimComboClass* anim_combo)
    override fun setAnimation(combo: HAnimComboClass?) {
        release()
        curMotionMode = MOTION_MULTIPLE_ANIM
        animCombo = combo
        isTreeValid = false
    }

    // C++: virtual HAnimClass* Peek_Animation()
    // C++ returns ModeAnim.Motion only when CurMotionMode == SINGLE_ANIM, else NULL
    override fun peekAnimation(): HAnimClass? =
        if (curMotionMode == MOTION_SINGLE_ANIM) motion else null

    // C++: virtual bool Is_Animation_Complete() const
    // C++ checks CurMotionMode == SINGLE_ANIM and AnimMode == ANIM_MODE_ONCE
    open fun isAnimationComplete(): Boolean {
        if (curMotionMode == MOTION_SINGLE_ANIM) {
            val m = motion ?: return false
            if (animMode == ANIM_MODE_ONCE) {
                return curFrame == m.numFrames.toFloat() - 1f
            }
        }
        return false
    }

    // C++: float Compute_Current_Frame() const  (protected)
    // Server-side: returns the stored frame (no time-based progression needed server-side)
    protected open fun computeCurrentFrame(): Float = curFrame

    // -------------------------------------------------------------------------
    // Bone interface — delegate to HTree
    // -------------------------------------------------------------------------

    // C++: virtual int Get_Num_Bones()
    // C++ returns HTree->Num_Pivots() if HTree != null, else 1 (one root bone)
    override fun getNumBones(): Int = hTree?.getNumBones() ?: 1

    // C++: virtual int Get_Bone_Index(const char* bonename)
    override fun getBoneIndex(name: String): Int = hTree?.getBoneIndex(name) ?: 0

    // C++: virtual const char* Get_Bone_Name(int bone_index)
    // C++ returns "RootTransform" when HTree is null
    override fun getBoneName(boneIndex: Int): String = hTree?.getBoneName(boneIndex) ?: "RootTransform"

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
    // Bone capture / control — delegate to HTree (C++ delegates to HTree)
    // -------------------------------------------------------------------------

    // C++: virtual void Capture_Bone(int boneindex)
    override fun captureBone(boneIndex: Int) {
        // C++: if (HTree) HTree->Capture_Bone(boneindex)
        // Server-side HTreeClass has no transform state; no-op
    }

    // C++: virtual void Release_Bone(int boneindex)
    override fun releaseBone(boneIndex: Int) {
        // C++: if (HTree) HTree->Release_Bone(boneindex)
    }

    // C++: virtual bool Is_Bone_Captured(int boneindex) const
    override fun isBoneCaptured(boneIndex: Int): Boolean {
        // C++: if (HTree) return HTree->Is_Bone_Captured(boneindex); else return false
        return false
    }

    // C++: virtual void Control_Bone(int bindex, const Matrix3D& objtm, bool world_space_translation = false)
    override fun controlBone(boneIndex: Int, objtm: Matrix3D, worldSpaceTranslation: Boolean) {
        // C++: if (HTree) { HTree->Control_Bone(bindex, objtm, world_space_translation); Set_Hierarchy_Valid(false); }
        if (hTree != null) {
            isTreeValid = false
        }
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
