package ccr.server.audio

import ccr.math.Vector3
import kotlin.math.sqrt

// C++: SoundSceneClass (WWAudio/SoundScene.h)
// Manages all active sound objects in the audio scene.
class SoundSceneClass {

    // C++: RefMultiListClass<AudibleSoundClass> DynamicSounds
    val dynamicSounds: MutableList<AudibleSoundClass> = mutableListOf()

    // C++: RefMultiListClass<AudibleSoundClass> StaticSounds
    val staticSounds: MutableList<AudibleSoundClass> = mutableListOf()

    // C++: RefMultiListClass<LogicalSoundClass> LogicalSounds
    val logicalSounds: MutableList<LogicalSoundClass> = mutableListOf()

    // C++: single-shot logical sounds pending notification
    val singleShotLogicalSounds: MutableList<LogicalSoundClass> = mutableListOf()

    // C++: RefMultiListClass<LogicalListenerClass> LogicalListeners
    val logicalListeners: MutableList<LogicalListenerClass> = mutableListOf()

    // C++: void Add_Sound(AudibleSoundClass*)
    fun addSound(sound: AudibleSoundClass) {
        if (!dynamicSounds.contains(sound)) dynamicSounds.add(sound)
    }

    // C++: void Remove_Sound(AudibleSoundClass*)
    fun removeSound(sound: AudibleSoundClass) {
        dynamicSounds.remove(sound)
    }

    // C++: void Add_Static_Sound(AudibleSoundClass*)
    fun addStaticSound(sound: AudibleSoundClass) {
        if (!staticSounds.contains(sound)) staticSounds.add(sound)
    }

    // C++: void Remove_Static_Sound(AudibleSoundClass*)
    fun removeStaticSound(sound: AudibleSoundClass) {
        staticSounds.remove(sound)
    }

    // C++: void Add_Logical_Sound(LogicalSoundClass*)
    fun addLogicalSound(sound: LogicalSoundClass) {
        if (!logicalSounds.contains(sound)) logicalSounds.add(sound)
    }

    // C++: void Remove_Logical_Sound(LogicalSoundClass*)
    fun removeLogicalSound(sound: LogicalSoundClass) {
        logicalSounds.remove(sound)
        singleShotLogicalSounds.remove(sound)
    }

    // C++: void Add_Single_Shot_Logical_Sound(LogicalSoundClass*)
    fun addSingleShotLogicalSound(sound: LogicalSoundClass) {
        if (!singleShotLogicalSounds.contains(sound)) singleShotLogicalSounds.add(sound)
    }

    // C++: void Add_Logical_Listener(LogicalListenerClass*)
    fun addLogicalListener(listener: LogicalListenerClass) {
        if (!logicalListeners.contains(listener)) logicalListeners.add(listener)
    }

    // C++: void Remove_Logical_Listener(LogicalListenerClass*)
    fun removeLogicalListener(listener: LogicalListenerClass) {
        logicalListeners.remove(listener)
    }

    // C++: void Flush_Scene()
    fun flushScene() {
        dynamicSounds.clear()
        staticSounds.clear()
        logicalSounds.clear()
        singleShotLogicalSounds.clear()
        logicalListeners.clear()
    }

    // C++: void Collect_Logical_Sounds()
    // Server-side AI hearing: fires callbacks when a listener is within range of a logical sound.
    fun collectLogicalSounds() {
        val allSounds = logicalSounds + singleShotLogicalSounds
        for (listener in logicalListeners.toList()) {
            val listenerPos = listener.getPosition()
            val listenerMask = listener.typeMask

            for (sound in allSounds) {
                // C++: type masks must overlap
                if ((listenerMask and sound.typeMask) == 0) continue

                val soundPos = sound.getPosition()
                val dx = listenerPos.x - soundPos.x
                val dy = listenerPos.y - soundPos.y
                val dz = listenerPos.z - soundPos.z
                val dist = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

                val radius = sound.getDropOffRadius() * listener.scale * LogicalListenerClass.globalScale
                if (dist <= radius) {
                    val cb = listener.callback
                    if (cb != null && (listener.registeredEvents and AudioCallbackClass.EVENT_LOGICAL_HEARD) != 0) {
                        cb.onLogicalHeard(listener, sound)
                    }
                }
            }
        }
    }
}
