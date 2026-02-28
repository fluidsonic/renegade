package ccr.server.audio

import ccr.math.Matrix3D
import ccr.server.defs.AudibleSoundDefinitionClass
import ccr.server.level.DefinitionRegistry
import kotlin.random.Random

// C++: WWAudioClass (WWAudio/WWAudio.h)
// Singleton audio manager. On the server it manages no real audio hardware,
// but maintains the sound scene for gameplay-relevant audio state.
class WWAudioClass(val definitions: DefinitionRegistry) {

    // C++: SoundSceneClass* m_SoundScene
    val soundScene = SoundSceneClass()

    // C++: AudibleSoundClass* Create_Sound(int definition_id)
    fun createSound(definitionId: Int): AudibleSoundClass? {
        val def = definitions.findById(definitionId.toUInt()) as? AudibleSoundDefinitionClass ?: return null
        return createSoundFromDef(def)
    }

    // C++: AudibleSoundClass* Create_Sound(const char* name)
    fun createSound(defName: String): AudibleSoundClass? {
        val def = definitions.findByName(defName) as? AudibleSoundDefinitionClass ?: return null
        return createSoundFromDef(def)
    }

    // C++: AudibleSoundClass* Create_Continuous_Sound(int definition_id)
    // Creates a sound with loopCount = -1 (infinite loop).
    fun createContinuousSound(definitionId: Int): AudibleSoundClass? {
        val def = definitions.findById(definitionId.toUInt()) as? AudibleSoundDefinitionClass ?: return null
        val sound = createSoundFromDef(def) ?: return null
        sound.loopCount = -1
        return sound
    }

    // C++: uint32 Create_Instant_Sound(int definition_id, const Matrix3D& tm, RefCountClass* user_obj = NULL)
    // Creates, positions, and immediately starts playing a one-shot sound.
    fun createInstantSound(definitionId: Int, tm: Matrix3D, userObj: Any? = null): UInt {
        val sound = createSound(definitionId) ?: return 0u
        sound.setTransform(tm)
        sound.userObj = userObj
        sound.addToScene(startPlaying = true)
        return sound.id
    }

    // C++: private AudibleSoundClass* Create_Sound_From_Def(AudibleSoundDefinitionClass*)
    private fun createSoundFromDef(def: AudibleSoundDefinitionClass): AudibleSoundClass? {
        val sound: AudibleSoundClass = if (def.is3D) Sound3DClass() else AudibleSoundClass()

        sound.definition = def
        sound.type = when (def.type) {
            AudibleSoundDefinitionClass.TYPE_MUSIC       -> AudibleSoundClass.SoundType.MUSIC
            AudibleSoundDefinitionClass.TYPE_DIALOG      -> AudibleSoundClass.SoundType.DIALOG
            AudibleSoundDefinitionClass.TYPE_CINEMATIC   -> AudibleSoundClass.SoundType.CINEMATIC
            else                                          -> AudibleSoundClass.SoundType.SOUND_EFFECT
        }
        sound.priority = def.priority
        sound.loopCount = def.loopCount
        sound.startOffset = def.startOffset.toLong()
        sound.virtualChannel = def.virtualChannel

        // Volume with randomizer
        val volumeRnd = if (def.volumeRandomizer > 0f) {
            (Random.nextFloat() * 2f - 1f) * def.volumeRandomizer
        } else 0f
        sound.volume = (def.volume + volumeRnd).coerceIn(0f, 1f)

        // Pitch with randomizer
        val pitchRnd = if (def.pitchFactorRandomizer > 0f) {
            (Random.nextFloat() * 2f - 1f) * def.pitchFactorRandomizer
        } else 0f
        sound.pitchFactor = def.pitchFactor + pitchRnd

        if (sound is Sound3DClass) {
            sound.maxVolRadius = def.maxVolRadius
            sound.setDropOffRadius(def.dropOffRadius)
        }

        return sound
    }

    companion object {
        private var instance: WWAudioClass? = null

        // No-op instance returned when no level is loaded (no audio definitions available).
        // Returns null for all factory methods — callers are expected to handle null returns.
        private val noopInstance: WWAudioClass = WWAudioClass(DefinitionRegistry())

        // C++: static WWAudioClass* Get_Instance()
        // Returns the initialized instance if available, or a no-op instance when no level is loaded.
        // The no-op instance returns null for all sound factory calls (no definitions available).
        fun getInstance(): WWAudioClass = instance ?: noopInstance

        // C++: static void Init(...)
        fun init(definitions: DefinitionRegistry) {
            instance = WWAudioClass(definitions)
        }

        // C++: static void Shutdown()
        fun shutdown() {
            instance?.soundScene?.flushScene()
            instance = null
        }

        // Returns true if the singleton is initialized (level is loaded with definitions)
        fun isInitialized(): Boolean = instance != null
    }
}
