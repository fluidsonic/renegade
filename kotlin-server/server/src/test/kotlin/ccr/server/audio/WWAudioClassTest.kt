package ccr.server.audio

import ccr.server.defs.AudibleSoundDefinitionClass
import ccr.server.level.DefinitionRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WWAudioClassTest {

    private fun buildRegistry(vararg defs: AudibleSoundDefinitionClass): DefinitionRegistry {
        val reg = DefinitionRegistry()
        defs.forEach { reg.register(it) }
        return reg
    }

    private fun makeAudibleDef(
        name: String = "test_sound",
        id: UInt = 1u,
        is3D: Boolean = false,
        loopCount: Int = 1,
        volume: Float = 1.0f,
        priority: Float = 0.5f,
    ) = AudibleSoundDefinitionClass(
        name = name,
        id = id,
        chunkId = AudibleSoundDefinitionClass.CHUNK_ID,
        volume = volume,
        priority = priority,
        loopCount = loopCount,
        is3D = is3D,
    )

    @Test
    fun `singleton lifecycle - init and shutdown`() {
        assertNull(WWAudioClass.getInstance())

        val reg = buildRegistry()
        WWAudioClass.init(reg)
        assertNotNull(WWAudioClass.getInstance())

        WWAudioClass.shutdown()
        assertNull(WWAudioClass.getInstance())
    }

    @Test
    fun `createSound by id returns null when def not found`() {
        val reg = buildRegistry()
        WWAudioClass.init(reg)
        try {
            assertNull(WWAudioClass.getInstance()!!.createSound(999))
        } finally {
            WWAudioClass.shutdown()
        }
    }

    @Test
    fun `createSound by name returns configured sound`() {
        val def = makeAudibleDef(name = "beep", id = 42u, priority = 0.8f, loopCount = 3)
        val reg = buildRegistry(def)
        WWAudioClass.init(reg)
        try {
            val sound = WWAudioClass.getInstance()!!.createSound("beep")
            assertNotNull(sound)
            assertEquals(0.8f, sound.priority)
            assertEquals(3, sound.loopCount)
        } finally {
            WWAudioClass.shutdown()
        }
    }

    @Test
    fun `createSound by id returns configured sound`() {
        val def = makeAudibleDef(name = "beep", id = 10u)
        val reg = buildRegistry(def)
        WWAudioClass.init(reg)
        try {
            val sound = WWAudioClass.getInstance()!!.createSound(10)
            assertNotNull(sound)
        } finally {
            WWAudioClass.shutdown()
        }
    }

    @Test
    fun `createSound returns Sound3DClass when is3D`() {
        val def = makeAudibleDef(name = "spatial", id = 20u, is3D = true)
        val reg = buildRegistry(def)
        WWAudioClass.init(reg)
        try {
            val sound = WWAudioClass.getInstance()!!.createSound(20)
            assertNotNull(sound)
            assertNotNull(sound!!.asSound3DClass())
        } finally {
            WWAudioClass.shutdown()
        }
    }

    @Test
    fun `createContinuousSound sets loopCount to -1`() {
        val def = makeAudibleDef(name = "loop_sound", id = 30u)
        val reg = buildRegistry(def)
        WWAudioClass.init(reg)
        try {
            val sound = WWAudioClass.getInstance()!!.createContinuousSound(30)
            assertNotNull(sound)
            assertEquals(-1, sound!!.loopCount)
        } finally {
            WWAudioClass.shutdown()
        }
    }

    @Test
    fun `createContinuousSound returns null when def not found`() {
        val reg = buildRegistry()
        WWAudioClass.init(reg)
        try {
            assertNull(WWAudioClass.getInstance()!!.createContinuousSound(999))
        } finally {
            WWAudioClass.shutdown()
        }
    }

    @Test
    fun `shutdown flushes sound scene`() {
        val def = makeAudibleDef(id = 50u)
        val reg = buildRegistry(def)
        WWAudioClass.init(reg)
        val audio = WWAudioClass.getInstance()!!

        // Create and add a sound to the scene
        val sound = audio.createSound(50)!!
        audio.soundScene.addSound(sound)
        assertEquals(1, audio.soundScene.dynamicSounds.size)

        WWAudioClass.shutdown()
        // After shutdown the scene is flushed
        assertEquals(0, audio.soundScene.dynamicSounds.size)
        assertNull(WWAudioClass.getInstance())
    }
}
