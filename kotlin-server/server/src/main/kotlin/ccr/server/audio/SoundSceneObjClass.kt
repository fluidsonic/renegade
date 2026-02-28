package ccr.server.audio

import ccr.math.Matrix3D
import ccr.math.Vector3
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// C++: SoundSceneObjClass (WWAudio/SoundSceneObj.h)
// Base class for all audio scene objects.
abstract class SoundSceneObjClass {

    // C++: uint32 m_ID — auto-allocated unique identifier
    val id: UInt = nextAvailableId.getAndIncrement().toUInt()

    // C++: SoundSceneClass* m_Scene
    var scene: SoundSceneClass? = null

    // C++: AudioCallbackClass* m_Callback
    var callback: AudioCallbackClass? = null

    // C++: uint32 m_RegisteredEvents
    var registeredEvents: Int = 0

    // C++: uint32 m_UserData
    var userData: UInt = 0u

    // C++: RefCountClass* m_UserObj
    var userObj: Any? = null

    init {
        globalSoundMap[id] = this
    }

    // C++: virtual void Set_Position(const Vector3&)
    abstract fun setPosition(pos: Vector3)

    // C++: virtual Vector3 Get_Position()
    abstract fun getPosition(): Vector3

    // C++: virtual void Set_Transform(const Matrix3D&)
    abstract fun setTransform(tm: Matrix3D)

    // C++: virtual Matrix3D Get_Transform()
    abstract fun getTransform(): Matrix3D

    // C++: virtual void Cull_Sound(bool)
    abstract fun cullSound(cull: Boolean)

    // C++: virtual bool Is_Sound_Culled()
    abstract fun isSoundCulled(): Boolean

    // C++: virtual void Add_To_Scene(bool start_playing = true)
    abstract fun addToScene(startPlaying: Boolean = true)

    // C++: virtual void Remove_From_Scene()
    abstract fun removeFromScene()

    // C++: virtual void Set_Drop_Off_Radius(float)
    abstract fun setDropOffRadius(radius: Float)

    // C++: virtual float Get_Drop_Off_Radius()
    abstract fun getDropOffRadius(): Float

    // C++: virtual AudibleSoundClass* As_AudibleSoundClass() { return NULL; }
    open fun asAudibleSoundClass(): AudibleSoundClass? = null

    // C++: virtual Sound3DClass* As_Sound3DClass() { return NULL; }
    open fun asSound3DClass(): Sound3DClass? = null

    // C++: virtual SoundPseudo3DClass* As_SoundPseudo3DClass() { return NULL; }
    open fun asSoundPseudo3DClass(): SoundPseudo3DClass? = null

    // C++: virtual FilteredSoundClass* As_FilteredSoundClass() { return NULL; }
    open fun asFilteredSoundClass(): FilteredSoundClass? = null

    // C++: Remove from global map on disposal
    fun dispose() {
        globalSoundMap.remove(id)
    }

    companion object {
        const val SOUND_OBJ_START_ID: UInt = 1_000_000_000u
        private val nextAvailableId = AtomicLong(SOUND_OBJ_START_ID.toLong())

        // C++: global sound object registry by ID
        val globalSoundMap: ConcurrentHashMap<UInt, SoundSceneObjClass> = ConcurrentHashMap()

        // C++: SoundSceneObjClass* Find_Sound_Object(uint32 id)
        fun findSoundObject(id: UInt): SoundSceneObjClass? = globalSoundMap[id]
    }
}
