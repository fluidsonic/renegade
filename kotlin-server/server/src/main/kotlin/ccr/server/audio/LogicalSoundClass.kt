package ccr.server.audio

import ccr.math.Matrix3D
import ccr.math.Vector3

// C++: LogicalSoundClass (WWAudio/LogicalSound.h)
// Represents a logical (gameplay-only) sound that affects AI hearing.
class LogicalSoundClass : SoundSceneObjClass() {

    // C++: uint32 m_TypeMask
    // @JvmField suppresses auto-generated getTypeMask()/setTypeMask() to avoid clash with explicit fun.
    @JvmField var typeMask: Int = 0

    // C++: float m_DropOffRadius (backing for abstract method)
    private var _dropOffRadius: Float = 0f

    // C++: bool m_IsSingleShot
    var isSingleShot: Boolean = false

    // C++: float m_NotifyDelay (in ms)
    var notifyDelayInMs: Long = 0L

    // C++: uint32 m_LastNotification (timestamp)
    var lastNotification: Long = 0L

    // C++: int m_MaxListeners
    var maxListeners: Int = 0

    // C++: RefCountClass* m_UserObj — stores creator reference
    var creatorObj: Any? = null

    // Backing fields for abstract position/transform
    private var _transform: Matrix3D = Matrix3D.IDENTITY

    // -------------------------------------------------------------------------
    // SoundSceneObjClass abstract method implementations
    // -------------------------------------------------------------------------

    override fun setTransform(tm: Matrix3D) { _transform = tm }
    override fun getTransform(): Matrix3D = _transform
    override fun getPosition(): Vector3 = _transform.translation
    override fun setPosition(pos: Vector3) { _transform = _transform.withTranslation(pos) }
    override fun cullSound(cull: Boolean) {}
    override fun isSoundCulled(): Boolean = false
    override fun setDropOffRadius(radius: Float) { _dropOffRadius = radius }
    override fun getDropOffRadius(): Float = _dropOffRadius

    // C++: virtual void Add_To_Scene(bool start_playing = true)
    override fun addToScene(startPlaying: Boolean) {
        val sc = WWAudioClass.getInstance()?.soundScene ?: return
        if (isSingleShot) {
            sc.addSingleShotLogicalSound(this)
        } else {
            sc.addLogicalSound(this)
        }
        scene = sc
    }

    // C++: virtual void Remove_From_Scene()
    override fun removeFromScene() {
        scene?.removeLogicalSound(this)
        scene = null
    }

    // C++: uint32 Get_Type_Mask()
    fun getTypeMask(): Int = typeMask

    // C++: RefCountClass* Peek_User_Obj() { return m_UserObj; }
    fun peekUserObj(): Any? = creatorObj
}
