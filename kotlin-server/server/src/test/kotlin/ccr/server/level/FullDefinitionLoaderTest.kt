package ccr.server.level

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FullDefinitionLoaderTest {

    /**
     * Builds a minimal Objects.DDB binary blob containing the given definitions.
     *
     * DDB structure:
     *   [CHUNKID_SAVELOAD_DEFMGR = 0x101]           <- container
     *     [CHUNKID_OBJECTS = 0x101]                   <- container
     *       [classId]                                 <- container, one per definition
     *         [SIMPLEFACTORY_CHUNKID_OBJDATA = 0x100101]  <- container
     *           [CHUNKID_VARIABLES = 0x100]           <- leaf (micro-chunks: id + name)
     */
    private fun buildDdb(vararg defs: Triple<UInt, UInt, String>): ByteArray {
        // Build the inner definitions
        val defsBytes = defs.map { (classId, defId, name) ->
            // Build the variables leaf chunk (micro-chunks for id + name)
            val idMicroChunk = buildMicroChunk(0x01, intToLeBytes(defId.toInt()))
            val nameMicroChunk = buildMicroChunk(0x03, name.toByteArray(Charsets.ISO_8859_1) + 0.toByte())
            val variablesData = idMicroChunk + nameMicroChunk
            val variablesChunk = buildChunk(0x00000100u, variablesData, isContainer = false)

            // Wrap in OBJDATA container
            val objDataChunk = buildChunk(0x00100101u, variablesChunk, isContainer = true)

            // Wrap in classId container
            buildChunk(classId, objDataChunk, isContainer = true)
        }.fold(byteArrayOf()) { acc, bytes -> acc + bytes }

        // Wrap in CHUNKID_OBJECTS container
        val objectsChunk = buildChunk(0x00000101u, defsBytes, isContainer = true)

        // Wrap in CHUNKID_SAVELOAD_DEFMGR container
        return buildChunk(0x00000101u, objectsChunk, isContainer = true)
    }

    private fun buildChunk(id: UInt, data: ByteArray, isContainer: Boolean): ByteArray {
        val buf = ByteBuffer.allocate(8 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(id.toInt())
        val sizeField = data.size or (if (isContainer) 0x80000000.toInt() else 0)
        buf.putInt(sizeField)
        buf.put(data)
        return buf.array()
    }

    private fun buildMicroChunk(id: Int, data: ByteArray): ByteArray {
        return byteArrayOf(id.toByte(), data.size.toByte()) + data
    }

    private fun intToLeBytes(value: Int): ByteArray {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(value)
        return buf.array()
    }

    @Test
    fun `empty DDB returns empty registry`() {
        val registry = FullDefinitionLoader.load(byteArrayOf())
        assertTrue(registry.isEmpty)
    }

    @Test
    fun `loads single definition with unknown classId as base DefinitionClass`() {
        val ddb = buildDdb(Triple(0xFFFFu, 42u, "TestDef"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(42u)
        assertNotNull(def)
        assertEquals("TestDef", def.name)
        assertEquals(42u, def.id)
        assertEquals(0xFFFFu, def.classId)
    }

    @Test
    fun `loads multiple definitions`() {
        val ddb = buildDdb(
            Triple(0xFFFFu, 1u, "Alpha"),
            Triple(0xFFFEu, 2u, "Bravo"),
            Triple(0xFFFDu, 3u, "Charlie"),
        )
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(3, registry.size)
        assertNotNull(registry.findByName("Alpha"))
        assertNotNull(registry.findByName("Bravo"))
        assertNotNull(registry.findByName("Charlie"))
    }

    @Test
    fun `skips definition with empty name`() {
        val ddb = buildDdb(
            Triple(0xFFFFu, 1u, "Good"),
            Triple(0xFFFEu, 2u, ""),
        )
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        assertNotNull(registry.findByName("Good"))
    }

    @Test
    fun `known classId for buildings registers typed subclass`() {
        // 0xD007 = WarFactoryGameObjDef — the parse function wraps in DefinitionClass subclass
        val ddb = buildDdb(Triple(0xD007u, 100u, "WarFactory"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(100u)
        assertNotNull(def)
        assertEquals("WarFactory", def.name)
        assertEquals(0xD007u, def.classId)
    }

    @Test
    fun `known physics classId registers definition`() {
        // 0x9007 = StaticPhysDefClass
        val ddb = buildDdb(Triple(0x9007u, 200u, "StaticPhys"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(200u)
        assertNotNull(def)
        assertEquals("StaticPhys", def.name)
    }

    @Test
    fun `data class definitions fall through to base DefinitionClass`() {
        // 0x3001 = SoldierGameObjDef (data class, not DefinitionClass subclass)
        // Should fall through to the else branch → base DefinitionClass
        val ddb = buildDdb(Triple(0x3001u, 300u, "Soldier"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(300u)
        assertNotNull(def)
        assertEquals("Soldier", def.name)
        assertEquals(0x3001u, def.classId)
        // Verify it's the base DefinitionClass (not a subclass)
        assertEquals(DefinitionClass::class, def::class)
    }
}
