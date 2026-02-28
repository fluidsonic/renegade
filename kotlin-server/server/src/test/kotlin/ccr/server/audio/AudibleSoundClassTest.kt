package ccr.server.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudibleSoundClassTest {

    @Test
    fun `initial state is STOPPED`() {
        val sound = AudibleSoundClass()
        assertEquals(AudibleSoundClass.SoundState.STOPPED, sound.state)
        assertFalse(sound.isPlaying())
    }

    @Test
    fun `play transitions to PLAYING`() {
        val sound = AudibleSoundClass()
        sound.play()
        assertEquals(AudibleSoundClass.SoundState.PLAYING, sound.state)
        assertTrue(sound.isPlaying())
    }

    @Test
    fun `stop transitions to STOPPED`() {
        val sound = AudibleSoundClass()
        sound.play()
        sound.stop()
        assertEquals(AudibleSoundClass.SoundState.STOPPED, sound.state)
        assertFalse(sound.isPlaying())
    }

    @Test
    fun `pause transitions to PAUSED when playing`() {
        val sound = AudibleSoundClass()
        sound.play()
        sound.pause()
        assertEquals(AudibleSoundClass.SoundState.PAUSED, sound.state)
        assertFalse(sound.isPlaying())
    }

    @Test
    fun `resume transitions back to PLAYING from PAUSED`() {
        val sound = AudibleSoundClass()
        sound.play()
        sound.pause()
        sound.resume()
        assertEquals(AudibleSoundClass.SoundState.PLAYING, sound.state)
        assertTrue(sound.isPlaying())
    }

    @Test
    fun `pause is no-op when already stopped`() {
        val sound = AudibleSoundClass()
        sound.pause()
        assertEquals(AudibleSoundClass.SoundState.STOPPED, sound.state)
    }

    @Test
    fun `getDuration returns length as float`() {
        val sound = AudibleSoundClass()
        sound.length = 5000L
        assertEquals(5000f, sound.getDuration())
    }

    @Test
    fun `setPitchFactor sets pitchFactor`() {
        val sound = AudibleSoundClass()
        sound.setPitchFactor(2.0f)
        assertEquals(2.0f, sound.getPitchFactor())
    }

    @Test
    fun `fadeIn sets fade state`() {
        val sound = AudibleSoundClass()
        sound.fadeIn(500f)
        assertEquals(AudibleSoundClass.FadeType.IN, sound.fadeType)
        assertEquals(500f, sound.fadeTime)
        assertEquals(0f, sound.fadeTimer)
    }

    @Test
    fun `fadeOut sets fade state`() {
        val sound = AudibleSoundClass()
        sound.fadeOut(1000f)
        assertEquals(AudibleSoundClass.FadeType.OUT, sound.fadeType)
        assertEquals(1000f, sound.fadeTime)
        assertEquals(0f, sound.fadeTimer)
    }

    @Test
    fun `getString returns null when no definition`() {
        val sound = AudibleSoundClass()
        kotlin.test.assertNull(sound.getString())
    }

    @Test
    fun `isSoundCulled defaults to false`() {
        val sound = AudibleSoundClass()
        assertFalse(sound.isSoundCulled())
    }

    @Test
    fun `cullSound sets isCulled`() {
        val sound = AudibleSoundClass()
        sound.cullSound(true)
        assertTrue(sound.isSoundCulled())
    }

    @Test
    fun `dropOffRadius round-trips`() {
        val sound = AudibleSoundClass()
        sound.setDropOffRadius(42f)
        assertEquals(42f, sound.getDropOffRadius())
    }

    @Test
    fun `volume field accessor`() {
        val sound = AudibleSoundClass()
        sound.volume = 0.75f
        assertEquals(0.75f, sound.volume)
    }

    @Test
    fun `each sound gets unique id`() {
        val a = AudibleSoundClass()
        val b = AudibleSoundClass()
        assertTrue(a.id != b.id)
    }

    @Test
    fun `asAudibleSoundClass returns this`() {
        val sound = AudibleSoundClass()
        assertTrue(sound.asAudibleSoundClass() === sound)
    }

    @Test
    fun `setTransform updates position`() {
        val sound = AudibleSoundClass()
        sound.setPosition(ccr.math.Vector3(1f, 2f, 3f))
        assertEquals(1f, sound.getPosition().x)
        assertEquals(2f, sound.getPosition().y)
        assertEquals(3f, sound.getPosition().z)
    }

    @Test
    fun `Sound3D subclass asSound3DClass returns this`() {
        val sound = Sound3DClass()
        assertTrue(sound.asSound3DClass() === sound)
        assertTrue(sound.asAudibleSoundClass() === sound)
    }

    @Test
    fun `FilteredSoundClass asFilteredSoundClass returns this`() {
        val sound = FilteredSoundClass()
        assertTrue(sound.asFilteredSoundClass() === sound)
        assertTrue(sound.asSoundPseudo3DClass() === sound)
        assertTrue(sound.asSound3DClass() === sound)
    }
}
