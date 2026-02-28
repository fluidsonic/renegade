package ccr.physics

// C++: Animatable3DObjClass : public RenderObjClass (ww3d2/animobj.h)
open class Animatable3DObjClass : RenderObjClass() {
    override fun asAnimatable3DObjClass(): Animatable3DObjClass = this
    fun setAnimation(animName: String, mode: Int = 0, frame: Float = 0f): Unit = TODO("stub")
    fun setAnimation(anim: HAnimClass, frame: Float, mode: Int): Unit = Unit
    fun getAnimationName(): String = TODO("stub")
    fun getNumFrames(): Int = TODO("stub")
    fun isLoaded(): Boolean = TODO("stub")
    fun getTotalTime(): Float = TODO("stub")
}
