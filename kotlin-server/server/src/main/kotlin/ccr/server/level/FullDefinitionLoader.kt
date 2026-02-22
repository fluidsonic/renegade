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
 * definition to its typed parser by chunkId. Definitions whose parser returns a
 * [DefinitionClass] subclass are stored as-is; all others (data-class parsers,
 * unknown chunkIds) are stored as a base [DefinitionClass] with name/id/chunkId.
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

        objectsChunk.forEachChunk { chunkId, _, defChunkReader ->
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

            val def = dispatch(chunkId, name, id, objDataChunk)
            registry.register(def)
        }

        return registry
    }

    /**
     * Dispatches to the typed parser for [chunkId] and returns a [DefinitionClass]
     * (or subclass). Falls back to a plain [DefinitionClass] for unknown or
     * non-DefinitionClass-based parsers.
     */
    private fun dispatch(
        chunkId: UInt,
        name: String,
        id: UInt,
        objDataChunk: ChunkReader,
    ): DefinitionClass {
        val fallback = DefinitionClass(name, id, chunkId)

        return when (chunkId) {

            // ── Game Object Defs (0x3000 range) ─────────────────────────────────────

            // SAMSiteGameObjDef → DefinitionClass
            SAMSiteGameObjDef.CHUNK_ID ->
                parseSAMSiteGameObjDef(objDataChunk, name, id, chunkId)

            // SpawnerDefClass → DefinitionClass
            SpawnerDefClass.CHUNK_ID ->
                parseSpawnerDefClass(objDataChunk, name, id, chunkId) ?: fallback

            // FIXME: C4GameObjDef is a data class — convert to DefinitionClass subclass and parse properly
            C4GameObjDef.CHUNK_ID -> fallback

            // FIXME: SoldierGameObjDef is a data class — convert to DefinitionClass subclass and parse properly
            SoldierGameObjDef.CHUNK_ID -> fallback

            // FIXME: SimpleGameObjDef is a data class — convert to DefinitionClass subclass and parse properly
            SimpleGameObjDef.CHUNK_ID -> fallback

            PowerUpGameObjDef.CHUNK_ID ->
                parsePowerUpGameObjDef(objDataChunk, name, id, chunkId)

            // TransitionGameObjDef → DefinitionClass
            TransitionGameObjDef.CHUNK_ID ->
                TransitionGameObjDef.load(chunkId, objDataChunk) ?: fallback

            // FIXME: VehicleGameObjDef is a data class — convert to DefinitionClass subclass and parse properly
            VehicleGameObjDef.CHUNK_ID -> fallback

            // FIXME: CinematicGameObjDef is a data class — convert to DefinitionClass subclass and parse properly
            CinematicGameObjDef.CHUNK_ID -> fallback

            BeaconGameObjDef.CHUNK_ID ->
                parseBeaconGameObjDef(objDataChunk, name, id, chunkId)

            // ScriptZoneGameObjDef → DefinitionClass
            ScriptZoneGameObjDef.CHUNK_ID ->
                parseScriptZoneGameObjDef(objDataChunk, name, id, chunkId)

            // DamageZoneGameObjDef → DefinitionClass
            DamageZoneGameObjDef.CHUNK_ID ->
                parseDamageZoneGameObjDef(objDataChunk, name, id, chunkId)

            // SpecialEffectsGameObjDef → DefinitionClass
            SpecialEffectsGameObjDef.CHUNK_ID ->
                parseSpecialEffectsGameObjDef(objDataChunk, name, id, chunkId)

            // SakuraBossGameObjDef → DefinitionClass
            SakuraBossGameObjDef.CHUNK_ID ->
                parseSakuraBossGameObjDef(objDataChunk, name, id, chunkId) ?: fallback

            // MendozaBossGameObjDefClass → DefinitionClass
            MendozaBossGameObjDefClass.CHUNK_ID ->
                parseMendozaBossGameObjDefClass(objDataChunk, name, id, chunkId)

            // RaveshawBossGameObjDefClass → DefinitionClass
            RaveshawBossGameObjDefClass.CHUNK_ID ->
                parseRaveshawBossGameObjDefClass(objDataChunk, name, id, chunkId)

            // ── Munitions (0xB000 range) ────────────────────────────────────────────

            WeaponDefinitionClass.CHUNK_ID ->
                parseWeaponDefinitionClass(objDataChunk, name, id, chunkId) ?: fallback

            AmmoDefinitionClass.CHUNK_ID ->
                parseAmmoDefinitionClass(objDataChunk, name, id, chunkId) ?: fallback

            ExplosionDefinitionClass.CHUNK_ID ->
                parseExplosionDefinitionClass(objDataChunk, name, id, chunkId) ?: fallback

            // ── Sound (0x5000 range) ────────────────────────────────────────────────

            AudibleSoundDefinitionClass.CHUNK_ID ->
                parseAudibleSoundDefinitionClass(objDataChunk, name, id, chunkId) ?: fallback

            // ── Physics (0x9000 range) ──────────────────────────────────────────────

            DecorationPhysDefClass.CHUNK_ID ->
                parseDecorationPhysDefClass(objDataChunk, name, id, chunkId)

            HumanPhysDefClass.CHUNK_ID ->
                HumanPhysDefClass.load(objDataChunk, name, id, chunkId)

            MotorcycleDefClass.CHUNK_ID ->
                MotorcycleDefClass.load(objDataChunk, name, id, chunkId)

            Phys3DefClass.CHUNK_ID ->
                Phys3DefClass.load(objDataChunk, name, id, chunkId)

            RigidBodyDefClass.CHUNK_ID ->
                RigidBodyDefClass.load(objDataChunk, name, id, chunkId)

            WheeledVehicleDefClass.CHUNK_ID ->
                WheeledVehicleDefClass.load(objDataChunk, name, id, chunkId)

            StaticPhysDefClass.CHUNK_ID ->
                parseStaticPhysDefClass(objDataChunk, name, id, chunkId)

            StaticAnimPhysDefClass.CHUNK_ID ->
                parseStaticAnimPhysDefClass(objDataChunk, name, id, chunkId)

            ProjectileDefClass.CHUNK_ID ->
                ProjectileDefClass.load(objDataChunk, name, id, chunkId)

            VehiclePhysDefClass.CHUNK_ID ->
                VehiclePhysDefClass.load(objDataChunk, name, id, chunkId)

            TrackedVehicleDefClass.CHUNK_ID ->
                TrackedVehicleDefClass.load(objDataChunk, name, id, chunkId)

            VTOLVehicleDefClass.CHUNK_ID ->
                VTOLVehicleDefClass.load(objDataChunk, name, id, chunkId) ?: fallback

            DynamicAnimPhysDefClass.CHUNK_ID ->
                parseDynamicAnimPhysDefClass(objDataChunk, name, id, chunkId)

            DoorPhysDefClass.CHUNK_ID ->
                parseDoorPhysDefClass(objDataChunk, name, id, chunkId)

            ElevatorPhysDefClass.CHUNK_ID ->
                parseElevatorPhysDefClass(objDataChunk, name, id, chunkId) ?: fallback

            // ── Buildings (0xD000 range) ────────────────────────────────────────────

            RefineryGameObjDef.CHUNK_ID ->
                parseRefineryGameObjDef(objDataChunk, name, id, chunkId)

            PowerPlantGameObjDef.CHUNK_ID ->
                parsePowerPlantGameObjDef(objDataChunk, name, id, chunkId)

            SoldierFactoryGameObjDef.CHUNK_ID ->
                parseSoldierFactoryGameObjDef(objDataChunk, name, id, chunkId)

            VehicleFactoryGameObjDef.CHUNK_ID ->
                parseVehicleFactoryGameObjDef(objDataChunk, name, id, chunkId)

            AirStripGameObjDef.CHUNK_ID ->
                parseAirStripGameObjDef(objDataChunk, name, id, chunkId)

            WarFactoryGameObjDef.CHUNK_ID ->
                parseWarFactoryGameObjDef(objDataChunk, name, id, chunkId)

            ComCenterGameObjDef.CHUNK_ID ->
                parseComCenterGameObjDef(objDataChunk, name, id, chunkId)

            RepairBayGameObjDef.CHUNK_ID ->
                parseRepairBayGameObjDef(objDataChunk, name, id, chunkId)

            // BuildingGameObjDef → DefinitionClass (generic building not covered by a subtype)
            BuildingGameObjDef.CHUNK_ID ->
                BuildingGameObjDef.load(objDataChunk, chunkId) ?: fallback

            // ── Purchase Settings ────────────────────────────────────────────────────
            PurchaseSettingsDefClass.CHUNK_ID ->             // 0x40607
                PurchaseSettingsDefClass.load(objDataChunk) ?: fallback

            // ── Global Settings (0x40600 range) ─────────────────────────────────────
            // FIXME: remaining are data classes — convert to DefinitionClass subclasses and parse properly
            0x00040600u,                                    // CHUNKID_GLOBAL_SETTINGS_DEF (wrapper)
            HumanLoiterGlobalSettingsDef.CHUNK_ID,          // 0x40601
            GlobalSettingsGeneralDef.CHUNK_ID,              // 0x40602
            HUDGlobalSettingsDef.CHUNK_ID,                  // 0x40603
            EvaSettingsDefClass.CHUNK_ID,                   // 0x40604
            CharacterClassSettingsDefClass.CHUNK_ID,        // 0x40605
            HumanAnimOverrideDef.CHUNK_ID,                  // 0x40606
            TeamPurchaseSettingsDefClass.CHUNK_ID,          // 0x40608
            CNCModeSettingsDef.CHUNK_ID,                    // 0x40609
            -> fallback

            // ── Editor-only objects (CHUNKID_COMMANDO_EDITOR_BEGIN range 0x50000) ────
            // These appear in Objects.DDB but have no gameplay role on the server.
            0x00050003u,  // CHUNKID_WAYPATH_DEF
            0x00050016u,  // CHUNKID_VIS_POINT_DEF
            0x00050018u,  // CHUNKID_PATHFIND_START_DEF
            -> fallback

            // ── Unknown chunkId — log error and fall back ────────────────────────────
            else -> {
                System.err.println("[ERROR] FullDefinitionLoader: unknown chunkId=0x${chunkId.toString(16)} name=\"$name\" id=$id")
                fallback
            }
        }
    }
}
