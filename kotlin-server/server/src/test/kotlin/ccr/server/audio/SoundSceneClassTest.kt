package ccr.server.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SoundSceneClassTest {

    @Test
    fun `addSound and removeSound work correctly`() {
        val scene = SoundSceneClass()
        val sound = AudibleSoundClass()

        scene.addSound(sound)
        assertEquals(1, scene.dynamicSounds.size)
        assertTrue(scene.dynamicSounds.contains(sound))

        scene.removeSound(sound)
        assertEquals(0, scene.dynamicSounds.size)
    }

    @Test
    fun `addSound is idempotent`() {
        val scene = SoundSceneClass()
        val sound = AudibleSoundClass()

        scene.addSound(sound)
        scene.addSound(sound)
        assertEquals(1, scene.dynamicSounds.size)
    }

    @Test
    fun `addLogicalListener and removeLogicalListener work correctly`() {
        val scene = SoundSceneClass()
        val listener = LogicalListenerClass()

        scene.addLogicalListener(listener)
        assertEquals(1, scene.logicalListeners.size)

        scene.removeLogicalListener(listener)
        assertEquals(0, scene.logicalListeners.size)
    }

    @Test
    fun `addLogicalSound and removeLogicalSound work correctly`() {
        val scene = SoundSceneClass()
        val sound = LogicalSoundClass()

        scene.addLogicalSound(sound)
        assertEquals(1, scene.logicalSounds.size)

        scene.removeLogicalSound(sound)
        assertEquals(0, scene.logicalSounds.size)
    }

    @Test
    fun `addSingleShotLogicalSound adds to singleShotLogicalSounds`() {
        val scene = SoundSceneClass()
        val sound = LogicalSoundClass()

        scene.addSingleShotLogicalSound(sound)
        assertEquals(1, scene.singleShotLogicalSounds.size)
        assertEquals(0, scene.logicalSounds.size)
    }

    @Test
    fun `flushScene clears all lists`() {
        val scene = SoundSceneClass()
        scene.addSound(AudibleSoundClass())
        scene.addStaticSound(AudibleSoundClass())
        scene.addLogicalSound(LogicalSoundClass())
        scene.addSingleShotLogicalSound(LogicalSoundClass())
        scene.addLogicalListener(LogicalListenerClass())

        scene.flushScene()

        assertEquals(0, scene.dynamicSounds.size)
        assertEquals(0, scene.staticSounds.size)
        assertEquals(0, scene.logicalSounds.size)
        assertEquals(0, scene.singleShotLogicalSounds.size)
        assertEquals(0, scene.logicalListeners.size)
    }

    @Test
    fun `collectLogicalSounds fires callback when in range`() {
        val scene = SoundSceneClass()

        val sound = LogicalSoundClass().also {
            it.typeMask = 0x01
            it.setDropOffRadius(100f)
            it.setPosition(ccr.math.Vector3(0f, 0f, 0f))
        }
        scene.addLogicalSound(sound)

        var heard = false
        val listener = LogicalListenerClass().also {
            it.typeMask = 0x01
            it.setPosition(ccr.math.Vector3(50f, 0f, 0f))
            it.callback = object : AudioCallbackClass {
                override fun onLogicalHeard(listener: LogicalListenerClass, sound: LogicalSoundClass) {
                    heard = true
                }
            }
            it.registeredEvents = AudioCallbackClass.EVENT_LOGICAL_HEARD
        }
        scene.addLogicalListener(listener)

        scene.collectLogicalSounds()
        assertTrue(heard)
    }

    @Test
    fun `collectLogicalSounds does not fire callback when out of range`() {
        val scene = SoundSceneClass()

        val sound = LogicalSoundClass().also {
            it.typeMask = 0x01
            it.setDropOffRadius(10f)
            it.setPosition(ccr.math.Vector3(0f, 0f, 0f))
        }
        scene.addLogicalSound(sound)

        var heard = false
        val listener = LogicalListenerClass().also {
            it.typeMask = 0x01
            it.setPosition(ccr.math.Vector3(50f, 0f, 0f))
            it.callback = object : AudioCallbackClass {
                override fun onLogicalHeard(listener: LogicalListenerClass, sound: LogicalSoundClass) {
                    heard = true
                }
            }
            it.registeredEvents = AudioCallbackClass.EVENT_LOGICAL_HEARD
        }
        scene.addLogicalListener(listener)

        scene.collectLogicalSounds()
        assertFalse(heard)
    }

    @Test
    fun `collectLogicalSounds does not fire when type masks do not overlap`() {
        val scene = SoundSceneClass()

        val sound = LogicalSoundClass().also {
            it.typeMask = 0x01
            it.setDropOffRadius(100f)
            it.setPosition(ccr.math.Vector3(0f, 0f, 0f))
        }
        scene.addLogicalSound(sound)

        var heard = false
        val listener = LogicalListenerClass().also {
            it.typeMask = 0x02   // different mask — no overlap
            it.setPosition(ccr.math.Vector3(0f, 0f, 0f))
            it.callback = object : AudioCallbackClass {
                override fun onLogicalHeard(listener: LogicalListenerClass, sound: LogicalSoundClass) {
                    heard = true
                }
            }
            it.registeredEvents = AudioCallbackClass.EVENT_LOGICAL_HEARD
        }
        scene.addLogicalListener(listener)

        scene.collectLogicalSounds()
        assertFalse(heard)
    }
}
