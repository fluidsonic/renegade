package ccr.server.audio

// C++: AudioCallbackClass (WWAudio/AudioCallback.h)
// Callback interface for audio events fired by SoundSceneObjClass instances.
interface AudioCallbackClass {
    companion object {
        const val EVENT_SOUND_STARTED: Int = 0x0001
        const val EVENT_SOUND_ENDED: Int = 0x0002
        const val EVENT_LOGICAL_HEARD: Int = 0x0004
    }

    fun onSoundStarted(sound: SoundSceneObjClass) {}
    fun onSoundEnded(sound: SoundSceneObjClass) {}
    fun onLogicalHeard(listener: LogicalListenerClass, sound: LogicalSoundClass) {}
}
