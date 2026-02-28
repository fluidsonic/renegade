package ccr.physics

import ccr.math.Matrix3D
import ccr.math.Quaternion

// C++: RenderObjClass (ww3d2/rendobj.h)
open class RenderObjClass {
    open fun getName(): String = TODO("stub")
    fun getNumSubObjects(): Int = TODO("stub")
    fun getSubObject(i: Int): RenderObjClass = TODO("stub")
    fun setHidden(hidden: Boolean): Unit = Unit
    fun isHidden(): Boolean = false
    fun getBoneIndex(name: String): Int = TODO("stub")
    fun getBoneTransform(boneIndex: Int): Matrix3D = TODO("stub")
    fun getNumBones(): Int = TODO("stub")
    fun getBoneName(boneIndex: Int): String = TODO("stub")
    fun getSubObjectBoneIndex(subObj: RenderObjClass?): Int = TODO("stub")
    fun getNumSubObjectsOnBone(boneIndex: Int): Int = TODO("stub")
    fun getSubObjectOnBone(boneIndex: Int, index: Int): RenderObjClass = TODO("stub")
    fun addSubObjectToBone(subObj: RenderObjClass, boneIndex: Int): Unit = Unit
    fun addSubObjectToBone(subObj: RenderObjClass, boneName: String): Unit = Unit
    fun removeSubObject(subObj: RenderObjClass): Unit = TODO("stub")
    fun captureBone(boneIndex: Int): Unit = TODO("stub")
    fun releaseBone(boneIndex: Int): Unit = TODO("stub")
    fun isBoneCaptured(boneIndex: Int): Boolean = TODO("stub")
    fun setParentTransform(tm: Matrix3D): Unit = TODO("stub")
    fun getOrientation(): Quaternion = TODO("stub")
    fun getHTree(): HTreeClass? = TODO("stub")
    // As_* casts
    open fun asMeshClass(): MeshClass? = null
    open fun asAnimatable3DObjClass(): Animatable3DObjClass? = null
    open fun asCompositeRenderObjClass(): CompositeRenderObjClass? = null

    open val classId: Int get() = 0

    companion object {
        const val ANIM_MODE_LOOP   = 0
        const val ANIM_MODE_ONCE   = 1
        const val ANIM_MODE_STOP   = 2
        const val ANIM_MODE_TARGET = 3
        const val ANIM_MODE_LOOP_ONCE = 4
        const val ANIM_MODE_MANUAL = 5
        const val CLASSID_MESH = 1
    }
}
