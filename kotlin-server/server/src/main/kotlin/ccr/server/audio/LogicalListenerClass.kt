package ccr.server.audio

import ccr.math.Matrix3D
import ccr.math.Vector3

// C++: LogicalListenerClass (WWAudio/LogicalListener.h)
// Represents an AI listener that can "hear" logical sounds in range.
class LogicalListenerClass : SoundSceneObjClass() {

    // C++: float m_Scale
    var scale: Float = 1f

    // C++: uint32 m_TypeMask
    var typeMask: Int = 0

    // C++: uint32 m_Timestamp
    var timestamp: Long = 0L

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
    override fun setDropOffRadius(radius: Float) {}
    override fun getDropOffRadius(): Float = 0f

    // C++: virtual void Add_To_Scene(bool start_playing = true)
    override fun addToScene(startPlaying: Boolean) {
        val sc = WWAudioClass.getInstance()?.soundScene ?: return
        sc.addLogicalListener(this)
        scene = sc
    }

    // C++: virtual void Remove_From_Scene()
    override fun removeFromScene() {
        scene?.removeLogicalListener(this)
        scene = null
    }

    companion object {
        // C++: static float GlobalScale
        var globalScale: Float = 1f
    }
}
