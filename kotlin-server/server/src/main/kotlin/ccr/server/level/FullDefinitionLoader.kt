package ccr.server.level

import ccr.server.defs.*
import ccr.server.defs.combat.*
import ccr.server.defs.phys.*
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Loads all definitions from an Objects.DDB binary blob into a [DefinitionRegistry].
 *
 * Walks the same DDB chunk structure as [readDefinitions] but dispatches each
 * definition to its typed parser by classId. Definitions whose parser returns a
 * [DefinitionClass] subclass are stored as-is; all others (data-class parsers,
 * unknown classIds) are stored as a base [DefinitionClass] with name/id/classId.
 */
object FullDefinitionLoader {

    // ── DDB structure constants (from wwsaveload/definitionmgr.cpp) ──
    private const val CHUNKID_SAVELOAD_DEFMGR = 0x00000101u
    private const val CHUNKID_OBJECTS = 0x00000101u
    private const val CHUNKID_VARIABLES = 0x00000100u
    private const val VARID_INSTANCEID = 0x01
    private const val VARID_NAME = 0x03

    fun load(ddbData: ByteArray): DefinitionRegistry {
        val registry = DefinitionRegistry()
        val outerReader = ChunkReader(ddbData)

        val defMgrChunk = outerReader.findChunk(CHUNKID_SAVELOAD_DEFMGR) ?: return registry
        val objectsChunk = defMgrChunk.findChunk(CHUNKID_OBJECTS) ?: return registry

        objectsChunk.forEachChunk { classId, _, defChunkReader ->
            val objDataChunk = defChunkReader.findChunk(ChunkIds.SIMPLEFACTORY_CHUNKID_OBJDATA)
                ?: return@forEachChunk

            // Extract base DefinitionClass fields (name + id) from the deeply-nested variables chunk.
            val variablesChunk = objDataChunk.findChunkRecursive(CHUNKID_VARIABLES)
                ?: return@forEachChunk

            val idBytes = variablesChunk.findMicroChunk(VARID_INSTANCEID) ?: return@forEachChunk
            if (idBytes.size < 4) return@forEachChunk
            val id = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

            val nameBytes = variablesChunk.findMicroChunk(VARID_NAME) ?: return@forEachChunk
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val name = String(nameBytes, 0, if (nullIdx < 0) nameBytes.size else nullIdx, Charsets.ISO_8859_1)
            if (name.isEmpty()) return@forEachChunk

            val def = dispatch(classId, name, id, objDataChunk)
            registry.register(def)
        }

        return registry
    }

    /**
     * Dispatches to the typed parser for [classId] and returns a [DefinitionClass]
     * (or subclass). Falls back to a plain [DefinitionClass] for unknown or
     * non-DefinitionClass-based parsers.
     */
    private fun dispatch(
        classId: UInt,
        name: String,
        id: UInt,
        objDataChunk: ChunkReader,
    ): DefinitionClass {
        val fallback = DefinitionClass(name, id, classId)

        return when (classId) {

            // ── Game Object Defs (0x3000 range) ─────────────────────────────────────

            // SAMSiteGameObjDef → DefinitionClass
            SAMSiteGameObjDef.CLASS_ID ->
                parseSAMSiteGameObjDef(objDataChunk, name, id, classId)

            // SpawnerDefClass → DefinitionClass
            SpawnerDefClass.CLASS_ID ->
                parseSpawnerDefClass(objDataChunk, name, id, classId) ?: fallback

            // ScriptZoneGameObjDef → DefinitionClass
            ScriptZoneGameObjDef.CLASS_ID ->
                parseScriptZoneGameObjDef(objDataChunk, name, id, classId)

            // DamageZoneGameObjDef → DefinitionClass
            DamageZoneGameObjDef.CLASS_ID ->
                parseDamageZoneGameObjDef(objDataChunk, name, id, classId)

            // SpecialEffectsGameObjDef → DefinitionClass
            SpecialEffectsGameObjDef.CLASS_ID ->
                parseSpecialEffectsGameObjDef(objDataChunk, name, id, classId)

            // SakuraBossGameObjDef → DefinitionClass
            SakuraBossGameObjDef.CLASS_ID ->
                parseSakuraBossGameObjDef(objDataChunk, name, id, classId) ?: fallback

            // MendozaBossGameObjDefClass → DefinitionClass
            MendozaBossGameObjDefClass.CLASS_ID ->
                parseMendozaBossGameObjDefClass(objDataChunk, name, id, classId)

            // RaveshawBossGameObjDefClass → DefinitionClass
            RaveshawBossGameObjDefClass.CLASS_ID ->
                parseRaveshawBossGameObjDefClass(objDataChunk, name, id, classId)

            // ── Munitions (0xB000 range) ────────────────────────────────────────────

            WeaponDefinitionClass.CLASS_ID ->
                parseWeaponDefinitionClass(objDataChunk, name, id, classId) ?: fallback

            AmmoDefinitionClass.CLASS_ID ->
                parseAmmoDefinitionClass(objDataChunk, name, id, classId) ?: fallback

            ExplosionDefinitionClass.CLASS_ID ->
                parseExplosionDefinitionClass(objDataChunk, name, id, classId) ?: fallback

            // ── Sound (0x5000 range) ────────────────────────────────────────────────

            AudibleSoundDefinitionClass.CLASS_ID ->
                parseAudibleSoundDefinitionClass(objDataChunk, name, id, classId) ?: fallback

            // ── Physics (0x9000 range) ──────────────────────────────────────────────

            DecorationPhysDefClass.CLASS_ID ->
                parseDecorationPhysDefClass(objDataChunk, name, id, classId)

            HumanPhysDefClass.CLASS_ID ->
                HumanPhysDefClass.load(objDataChunk, name, id, classId)

            MotorcycleDefClass.CLASS_ID ->
                MotorcycleDefClass.load(objDataChunk, name, id, classId)

            Phys3DefClass.CLASS_ID ->
                Phys3DefClass.load(objDataChunk, name, id, classId)

            RigidBodyDefClass.CLASS_ID ->
                RigidBodyDefClass.load(objDataChunk, name, id, classId)

            WheeledVehicleDefClass.CLASS_ID ->
                WheeledVehicleDefClass.load(objDataChunk, name, id, classId)

            StaticPhysDefClass.CLASS_ID ->
                parseStaticPhysDefClass(objDataChunk, name, id, classId)

            StaticAnimPhysDefClass.CLASS_ID ->
                parseStaticAnimPhysDefClass(objDataChunk, name, id, classId)

            ProjectileDefClass.CLASS_ID ->
                ProjectileDefClass.load(objDataChunk, name, id, classId)

            VehiclePhysDefClass.CLASS_ID ->
                VehiclePhysDefClass.load(objDataChunk, name, id, classId)

            TrackedVehicleDefClass.CLASS_ID ->
                TrackedVehicleDefClass.load(objDataChunk, name, id, classId)

            VTOLVehicleDefClass.CLASS_ID ->
                VTOLVehicleDefClass.load(objDataChunk, name, id, classId) ?: fallback

            DynamicAnimPhysDefClass.CLASS_ID ->
                parseDynamicAnimPhysDefClass(objDataChunk, name, id, classId)

            DoorPhysDefClass.CLASS_ID ->
                parseDoorPhysDefClass(objDataChunk, name, id, classId)

            ElevatorPhysDefClass.CLASS_ID ->
                parseElevatorPhysDefClass(objDataChunk, name, id, classId) ?: fallback

            // ── Buildings (0xD000 range) ────────────────────────────────────────────

            RefineryGameObjDef.CLASS_ID ->
                parseRefineryGameObjDef(objDataChunk, name, id, classId)

            PowerPlantGameObjDef.CLASS_ID ->
                parsePowerPlantGameObjDef(objDataChunk, name, id, classId)

            SoldierFactoryGameObjDef.CLASS_ID ->
                parseSoldierFactoryGameObjDef(objDataChunk, name, id, classId)

            VehicleFactoryGameObjDef.CLASS_ID ->
                parseVehicleFactoryGameObjDef(objDataChunk, name, id, classId)

            AirStripGameObjDef.CLASS_ID ->
                parseAirStripGameObjDef(objDataChunk, name, id, classId)

            WarFactoryGameObjDef.CLASS_ID ->
                parseWarFactoryGameObjDef(objDataChunk, name, id, classId)

            ComCenterGameObjDef.CLASS_ID ->
                parseComCenterGameObjDef(objDataChunk, name, id, classId)

            RepairBayGameObjDef.CLASS_ID ->
                parseRepairBayGameObjDef(objDataChunk, name, id, classId)

            // ── Fallback for all other classIds ─────────────────────────────────────
            // Includes: SoldierGameObjDef (0x3001), VehicleGameObjDef (0x3010),
            // SimpleGameObjDef (0x3004), PowerUpGameObjDef (0x3003),
            // BeaconGameObjDef (0x3016), C4GameObjDef (0x3006),
            // CinematicGameObjDef (0x3011), global settings (0xF00x range),
            // TwiddlerClass, BuildingAggregateDefClass, MotorVehicleDefClass,
            // TimedDecorationPhysDefClass, ShakeableStaticPhysDefClass,
            // AccessiblePhysDefClass, DamageableStaticPhysDefClass, etc.
            else -> fallback
        }
    }
}
