package ccr.server.audio

// C++: FilteredSoundClass : public SoundPseudo3DClass (WWAudio/FilteredSound.h)
// A pseudo-3D sound with DSP filter effects (reverb/EQ stub).
class FilteredSoundClass : SoundPseudo3DClass() {

    // C++: virtual FilteredSoundClass* As_FilteredSoundClass()
    override fun asFilteredSoundClass(): FilteredSoundClass = this
}
