package ccr.server.audio

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.defs.AudibleSoundDefinitionClass

// C++: AudibleSoundClass (WWAudio/AudibleSound.h)
// Represents a playing audio instance. Extends SoundSceneObjClass with playback state.
open class AudibleSoundClass : SoundSceneObjClass() {

    // C++: AudibleSoundClass::SoundState enum
    enum class SoundState { STOPPED, PLAYING, PAUSED }

    // C++: AudibleSoundClass::SoundType enum (mirrors AudibleSoundDefinitionClass type constants)
    enum class SoundType { MUSIC, SOUND_EFFECT, DIALOG, CINEMATIC }

    // C++: AudibleSoundClass::FadeType enum
    enum class FadeType { NONE, IN, OUT }

    // C++: SoundState m_State
    var state: SoundState = SoundState.STOPPED

    // C++: SoundType m_Type
    var type: SoundType = SoundType.SOUND_EFFECT

    // C++: FadeType m_FadeType
    var fadeType: FadeType = FadeType.NONE

    // C++: float m_FadeTimer
    var fadeTimer: Float = 0f

    // C++: float m_FadeTime
    var fadeTime: Float = 0f

    // C++: int m_VirtualChannel
    var virtualChannel: Int = 0

    // C++: float m_RuntimePriority
    var runtimePriority: Float = 0f

    // C++: float m_Priority
    var priority: Float = 0f

    // C++: float m_Volume
    var volume: Float = 1f

    // C++: float m_RealVolume
    var realVolume: Float = 1f

    // C++: float m_Pan
    var pan: Float = 0f

    // C++: int m_LoopCount
    var loopCount: Int = 0

    // C++: int m_LoopsLeft
    var loopsLeft: Int = 0

    // C++: uint32 m_Length — duration in ms
    var length: Long = 0L

    // C++: uint32 m_CurrentPosition
    var currentPosition: Long = 0L

    // C++: uint32 m_StartOffset
    var startOffset: Long = 0L

    // C++: float m_PitchFactor
    var pitchFactor: Float = 1f

    // C++: Matrix3D m_ListenerTransform
    var listenerTransform: Matrix3D = Matrix3D.IDENTITY

    // C++: bool m_IsCulled
    var isCulled: Boolean = false

    // C++: bool m_IsDirty
    var isDirty: Boolean = false

    // C++: float m_DropOffRadius (backing for abstract method)
    private var _dropOffRadius: Float = 0f

    // C++: Matrix3D m_Transform (backing for position/transform abstract methods)
    protected var _transform: Matrix3D = Matrix3D.IDENTITY

    // C++: Matrix3D m_PrevTransform
    var prevTransform: Matrix3D = Matrix3D.IDENTITY

    // C++: AudibleSoundDefinitionClass* m_Definition
    var definition: AudibleSoundDefinitionClass? = null

    // C++: LogicalSoundClass* m_LogicalSound
    var logicalSound: LogicalSoundClass? = null

    // -------------------------------------------------------------------------
    // SoundSceneObjClass abstract method implementations
    // -------------------------------------------------------------------------

    override fun setTransform(tm: Matrix3D) {
        _transform = tm
    }

    override fun getTransform(): Matrix3D = _transform

    // C++: Vector3 Get_Position() — translation column of transform
    override fun getPosition(): Vector3 = _transform.translation

    override fun setPosition(pos: Vector3) {
        _transform = _transform.withTranslation(pos)
    }

    override fun cullSound(cull: Boolean) { isCulled = cull }
    override fun isSoundCulled(): Boolean = isCulled
    override fun setDropOffRadius(radius: Float) { _dropOffRadius = radius }
    override fun getDropOffRadius(): Float = _dropOffRadius

    // C++: virtual void Add_To_Scene(bool start_playing = true)
    override fun addToScene(startPlaying: Boolean) {
        val sc = WWAudioClass.getInstance()?.soundScene ?: return
        sc.addSound(this)
        scene = sc
        if (startPlaying) play()
    }

    // C++: virtual void Remove_From_Scene()
    override fun removeFromScene() {
        scene?.removeSound(this)
        scene = null
        stop()
    }

    // -------------------------------------------------------------------------
    // Conversion
    // -------------------------------------------------------------------------

    override fun asAudibleSoundClass(): AudibleSoundClass = this

    // -------------------------------------------------------------------------
    // Playback control
    // -------------------------------------------------------------------------

    // C++: virtual void Play()
    open fun play() {
        state = SoundState.PLAYING
        loopsLeft = loopCount
        val cb = callback
        if (cb != null && (registeredEvents and AudioCallbackClass.EVENT_SOUND_STARTED) != 0) {
            cb.onSoundStarted(this)
        }
    }

    // C++: virtual void Stop()
    open fun stop() {
        state = SoundState.STOPPED
        val cb = callback
        if (cb != null && (registeredEvents and AudioCallbackClass.EVENT_SOUND_ENDED) != 0) {
            cb.onSoundEnded(this)
        }
    }

    // C++: virtual void Pause()
    open fun pause() {
        if (state == SoundState.PLAYING) {
            state = SoundState.PAUSED
        }
    }

    // C++: virtual void Resume()
    open fun resume() {
        if (state == SoundState.PAUSED) {
            state = SoundState.PLAYING
        }
    }

    // C++: bool Is_Playing()
    fun isPlaying(): Boolean = state == SoundState.PLAYING

    // C++: uint32 Get_Duration() — returns duration in milliseconds
    fun getDuration(): Float = length.toFloat()

    // C++: void Fade_In(float time_ms)
    fun fadeIn(timeMs: Float) {
        fadeType = FadeType.IN
        fadeTime = timeMs
        fadeTimer = 0f
    }

    // C++: void Fade_Out(float time_ms)
    fun fadeOut(timeMs: Float) {
        fadeType = FadeType.OUT
        fadeTime = timeMs
        fadeTimer = 0f
    }

    // C++: const char* Get_String() — returns display text from definition
    fun getString(): String? = definition?.displayText

    // C++: void Set_Pitch_Factor(float pitch)
    fun setPitchFactor(pitch: Float) { pitchFactor = pitch }

    // C++: float Get_Pitch_Factor()
    fun getPitchFactor(): Float = pitchFactor

    // Overload: setTransform accepting a Vector3 (position only) — matches C++ usage in BeaconGameObj etc.
    fun setTransform(center: Vector3) {
        _transform = _transform.withTranslation(center)
    }
}
