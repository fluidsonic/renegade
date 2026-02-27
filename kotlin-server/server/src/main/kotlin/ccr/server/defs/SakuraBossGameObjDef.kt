package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of SakuraBossGameObjDef (Combat/sakurabossgameobj.h).
 *
 * C++ hierarchy: SakuraBossGameObjDef : VehicleGameObjDef : SmartGameObjDef :
 *   ArmedGameObjDef : PhysicalGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * Chunk layout inside OBJDATA:
 *   [CHUNKID_DEF_PARENT = 0x09070458]              -> VehicleGameObjDef::Save (parent chain)
 *   [CHUNKID_DEF_VARIABLES = 0x09070459]            -> sakura-specific micro-chunks
 *   [CHUNKID_DEF_ROCKET_DEFENSEOBJ_DEF = 0x0907045A] -> DefenseObjectDefClass for rockets
 */
class SakuraBossGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    vehicleDef: VehicleGameObjDef,
    // SakuraBoss-specific fields — defaults match C++ constructor
    val gattlingGunDefId: Int = 0,
    val rocketLauncherDefId: Int = 0,
    val gattlingGunRevSoundDefId: Int = 0,
    val rocketDoorOpenSoundId: Int = 0,
    val rocketDestroyedExplosionId: Int = 0,
    val rocketsDefense: DefenseObjectDefClass = DefenseObjectDefClass(),
) : VehicleGameObjDefWrapper(name, id, chunkId, vehicleDef) {

    companion object {
        const val CHUNK_ID: UInt = 0x00040132u  // CHUNKID_GAME_OBJECT_DEF_SAKURA_BOSS

        // Chunk IDs from sakurabossgameobj.cpp local enum (starting at 0x09070458)
        private const val CHUNKID_DEF_PARENT = 0x09070458u
        private const val CHUNKID_DEF_VARIABLES = 0x09070459u
        private const val CHUNKID_DEF_ROCKET_DEFENSEOBJ_DEF = 0x0907045Au

        // Micro-chunk IDs (from sakurabossgameobj.cpp, starting at 1)
        private const val VARID_DEF_GATLING_DEF_ID = 1
        private const val VARID_DEF_ROCKET_DEF_ID = 2
        private const val VARID_DEF_GATLING_REV_SOUND_ID = 3
        private const val VARID_DEF_ROCKET_DOOR_SOUND_ID = 4
        private const val VARID_DEF_ROCKET_DESTROYED_EXPLOSION_ID = 5

        // DefenseObjectDefClass chunk/micro-chunk IDs (damage.cpp)
        private const val DEFENSEOBJECTDEF_CHUNK_VARIABLES = 7311607u
        private const val MCID_DEF_HEALTH = 0x00
        private const val MCID_DEF_HEALTH_MAX = 0x01
        private const val MCID_DEF_SKIN = 0x02
        private const val MCID_DEF_SHIELD_STRENGTH = 0x03
        private const val MCID_DEF_SHIELD_STRENGTH_MAX = 0x04
        private const val MCID_DEF_SHIELD_TYPE = 0x05
        private const val MCID_DEF_DAMAGE_POINTS = 0x06
        private const val MCID_DEF_DEATH_POINTS = 0x07

        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): SakuraBossGameObjDef? {
            // --- VehicleGameObjDef parent chain ---
            val vehicleParentChunk = objDataReader.findChunk(CHUNKID_DEF_PARENT) ?: return null
            val vehicleDef = VehicleGameObjDef.load(vehicleParentChunk) ?: return null

            // --- SakuraBoss-specific variables ---
            val varsReader = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
            val gattlingGunDefId = varsReader?.readMicroInt(VARID_DEF_GATLING_DEF_ID) ?: 0
            val rocketLauncherDefId = varsReader?.readMicroInt(VARID_DEF_ROCKET_DEF_ID) ?: 0
            val gattlingGunRevSoundDefId = varsReader?.readMicroInt(VARID_DEF_GATLING_REV_SOUND_ID) ?: 0
            val rocketDoorOpenSoundId = varsReader?.readMicroInt(VARID_DEF_ROCKET_DOOR_SOUND_ID) ?: 0
            val rocketDestroyedExplosionId = varsReader?.readMicroInt(VARID_DEF_ROCKET_DESTROYED_EXPLOSION_ID) ?: 0

            // --- Rockets defense object def ---
            val defenseChunk = objDataReader.findChunk(CHUNKID_DEF_ROCKET_DEFENSEOBJ_DEF)
            val rocketsDefense = if (defenseChunk != null) loadDefenseObjectDef(defenseChunk) else DefenseObjectDefClass()

            return SakuraBossGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                vehicleDef = vehicleDef,
                gattlingGunDefId = gattlingGunDefId,
                rocketLauncherDefId = rocketLauncherDefId,
                gattlingGunRevSoundDefId = gattlingGunRevSoundDefId,
                rocketDoorOpenSoundId = rocketDoorOpenSoundId,
                rocketDestroyedExplosionId = rocketDestroyedExplosionId,
                rocketsDefense = rocketsDefense,
            )
        }

        private fun loadDefenseObjectDef(defenseChunk: ChunkReader): DefenseObjectDefClass {
            val vars = defenseChunk.findChunk(DEFENSEOBJECTDEF_CHUNK_VARIABLES)
                ?: return DefenseObjectDefClass()
            return DefenseObjectDefClass(
                health              = vars.readMicroFloat(MCID_DEF_HEALTH) ?: 100f,
                healthMax           = vars.readMicroFloat(MCID_DEF_HEALTH_MAX) ?: 100f,
                skin                = vars.readMicroInt(MCID_DEF_SKIN) ?: 0,
                shieldStrength      = vars.readMicroFloat(MCID_DEF_SHIELD_STRENGTH) ?: 0f,
                shieldStrengthMax   = vars.readMicroFloat(MCID_DEF_SHIELD_STRENGTH_MAX) ?: 0f,
                shieldType          = vars.readMicroInt(MCID_DEF_SHIELD_TYPE) ?: 0,
                damagePoints        = vars.readMicroFloat(MCID_DEF_DAMAGE_POINTS) ?: 0f,
                deathPoints         = vars.readMicroFloat(MCID_DEF_DEATH_POINTS) ?: 0f,
            )
        }
    }
}
