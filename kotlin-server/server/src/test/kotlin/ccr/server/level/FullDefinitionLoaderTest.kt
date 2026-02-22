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
     *       [chunkId]                                 <- container, one per definition
     *         [SIMPLEFACTORY_CHUNKID_OBJDATA = 0x100101]  <- container
     *           [CHUNKID_VARIABLES = 0x100]           <- leaf (micro-chunks: id + name)
     */
    private fun buildDdb(vararg defs: Triple<UInt, UInt, String>): ByteArray =
        buildDdbWithExtra(*defs.map { (a, b, c) -> Triple(a, b, c) to byteArrayOf() }.toTypedArray())

    /**
     * Like [buildDdb] but allows appending extra bytes inside each definition's OBJDATA chunk.
     * Each pair is (chunkId, defId, name) → extra bytes appended alongside the variables chunk.
     */
    private fun buildDdbWithExtra(vararg defs: Pair<Triple<UInt, UInt, String>, ByteArray>): ByteArray {
        val defsBytes = defs.map { (triple, extra) ->
            val (chunkId, defId, name) = triple
            val idMicroChunk = buildMicroChunk(0x01, intToLeBytes(defId.toInt()))
            val nameMicroChunk = buildMicroChunk(0x03, name.toByteArray(Charsets.ISO_8859_1) + 0.toByte())
            val variablesData = idMicroChunk + nameMicroChunk
            val variablesChunk = buildChunk(0x00000100u, variablesData, isContainer = false)

            val objDataChunk = buildChunk(0x00100101u, variablesChunk + extra, isContainer = true)
            buildChunk(chunkId, objDataChunk, isContainer = true)
        }.fold(byteArrayOf()) { acc, bytes -> acc + bytes }

        val objectsChunk = buildChunk(0x00000101u, defsBytes, isContainer = true)
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
    fun `loads single definition with unknown chunkId as base DefinitionClass`() {
        val ddb = buildDdb(Triple(0xFFFFu, 42u, "TestDef"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(42u)
        assertNotNull(def)
        assertEquals("TestDef", def.name)
        assertEquals(42u, def.id)
        assertEquals(0xFFFFu, def.chunkId)
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
    fun `known chunkId for buildings registers typed subclass`() {
        // 0x00040142 = WarFactoryGameObjDef (CHUNKID_GAME_OBJECT_DEF_WARFACTORY)
        val ddb = buildDdb(Triple(0x00040142u, 100u, "WarFactory"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(100u)
        assertNotNull(def)
        assertEquals("WarFactory", def.name)
        assertEquals(0x00040142u, def.chunkId)
    }

    @Test
    fun `known physics chunkId registers definition`() {
        // 0x00020508 = StaticPhysDefClass (PHYSICS_CHUNKID_STATICPHYSDEF)
        val ddb = buildDdb(Triple(0x00020508u, 200u, "StaticPhys"))
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
        assertEquals(0x3001u, def.chunkId)
        // Verify it's the base DefinitionClass (not a subclass)
        assertEquals(DefinitionClass::class, def::class)
    }

    @Test
    fun `SpawnerDefClass dispatches correctly`() {
        // 0x00040121 = CHUNKID_SPAWNER_DEF
        // parseSpawnerDefClass needs CHUNKID_DEF_VARIABLES=1013991543 inside OBJDATA
        val spawnerVars = buildChunk(1013991543u, byteArrayOf(), isContainer = false)
        val ddb = buildDdbWithExtra(Triple(0x00040121u, 400u, "SpawnPoint") to spawnerVars)
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(400u)
        assertNotNull(def)
        assertEquals("SpawnPoint", def.name)
        assertTrue(def is ccr.server.defs.combat.SpawnerDefClass)
    }

    @Test
    fun `WeaponDefinitionClass dispatches correctly`() {
        // 0x00040127 = CHUNKID_WEAPON_DEF
        // parseWeaponDefinitionClass needs CHUNKID_WEAPON_DEF_VARIABLES=1205091654 inside OBJDATA
        val weaponVars = buildChunk(1205091654u, byteArrayOf(), isContainer = false)
        val ddb = buildDdbWithExtra(Triple(0x00040127u, 500u, "Pistol") to weaponVars)
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(500u)
        assertNotNull(def)
        assertEquals("Pistol", def.name)
        assertTrue(def is ccr.server.defs.WeaponDefinitionClass)
    }

    @Test
    fun `AmmoDefinitionClass dispatches correctly`() {
        // 0x00040128 = CHUNKID_AMMO_DEF
        // parseAmmoDefinitionClass needs CHUNKID_AMMO_DEF_VARIABLES=1206091429 inside OBJDATA
        val ammoVars = buildChunk(1206091429u, byteArrayOf(), isContainer = false)
        val ddb = buildDdbWithExtra(Triple(0x00040128u, 600u, "Bullets") to ammoVars)
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(600u)
        assertNotNull(def)
        assertEquals("Bullets", def.name)
        assertTrue(def is ccr.server.defs.AmmoDefinitionClass)
    }

    @Test
    fun `building defs dispatch to typed subclasses`() {
        // 0x00040138 = CHUNKID_GAME_OBJECT_DEF_REFINERY
        val ddb = buildDdb(Triple(0x00040138u, 700u, "Refinery"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(700u)
        assertNotNull(def)
        assertEquals("Refinery", def.name)
        assertTrue(def is ccr.server.defs.combat.RefineryGameObjDef)
    }

    @Test
    fun `physics defs dispatch correctly`() {
        // 0x00020501 = PHYSICS_CHUNKID_HUMANPHYSDEF
        val ddb = buildDdb(Triple(0x00020501u, 800u, "HumanPhys"))
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(800u)
        assertNotNull(def)
        assertEquals("HumanPhys", def.name)
        assertTrue(def is ccr.server.defs.HumanPhysDefClass)
    }

    @Test
    fun `PowerUpGameObjDef dispatches to typed subclass`() {
        // 0x00040107 = CHUNKID_GAME_OBJECT_DEF_POWERUP
        // parsePowerUpGameObjDef needs CHUNKID_DEF_VARIABLES=909991657 inside OBJDATA
        val powerUpVars = buildChunk(909991657u, byteArrayOf(), isContainer = false)
        val ddb = buildDdbWithExtra(Triple(0x00040107u, 999u, "PowerUp_Health") to powerUpVars)
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(999u)
        assertNotNull(def)
        assertEquals("PowerUp_Health", def.name)
        assertTrue(def is ccr.server.defs.combat.PowerUpGameObjDef)
    }

    @Test
    fun `BeaconGameObjDef dispatches to typed subclass`() {
        // 0x00040136 = CHUNKID_GAME_OBJECT_DEF_BEACON
        // parseBeaconGameObjDef needs CHUNKID_DEF_VARIABLES=35193910 (0x02190436) inside OBJDATA
        val beaconVars = buildChunk(35193910u, byteArrayOf(), isContainer = false)
        val ddb = buildDdbWithExtra(Triple(0x00040136u, 1001u, "Beacon_Nuke") to beaconVars)
        val registry = FullDefinitionLoader.load(ddb)

        assertEquals(1, registry.size)
        val def = registry.findById(1001u)
        assertNotNull(def)
        assertEquals("Beacon_Nuke", def.name)
        assertTrue(def is ccr.server.defs.combat.BeaconGameObjDef)
    }
}
