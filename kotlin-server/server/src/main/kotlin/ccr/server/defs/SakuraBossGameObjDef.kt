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
    // SakuraBoss-specific fields — defaults match C++ constructor
    val gattlingGunDefId: Int = 0,
    val rocketLauncherDefId: Int = 0,
    val gattlingGunRevSoundDefId: Int = 0,
    val rocketDoorOpenSoundId: Int = 0,
    val rocketDestroyedExplosionId: Int = 0,
    val rocketsDefense: DefenseObjectDefClass = DefenseObjectDefClass(),
) : BaseGameObjDef(name, id, chunkId) {

    companion object {
        const val CHUNK_ID: UInt = 0x00040132u  // CHUNKID_GAME_OBJECT_DEF_SAKURA_BOSS

        // Chunk IDs from sakurabossgameobj.cpp local enum
        private const val CHUNKID_DEF_VARIABLES = 0x09070459u

        // Micro-chunk IDs (from sakurabossgameobj.cpp, starting at 1)
        private const val VARID_DEF_GATLING_DEF_ID = 1
        private const val VARID_DEF_ROCKET_DEF_ID = 2
        private const val VARID_DEF_GATLING_REV_SOUND_ID = 3
        private const val VARID_DEF_ROCKET_DOOR_SOUND_ID = 4
        private const val VARID_DEF_ROCKET_DESTROYED_EXPLOSION_ID = 5

        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): SakuraBossGameObjDef? {
            val varsReader = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)

            val gattlingGunDefId = varsReader?.readMicroInt(VARID_DEF_GATLING_DEF_ID) ?: 0
            val rocketLauncherDefId = varsReader?.readMicroInt(VARID_DEF_ROCKET_DEF_ID) ?: 0
            val gattlingGunRevSoundDefId = varsReader?.readMicroInt(VARID_DEF_GATLING_REV_SOUND_ID) ?: 0
            val rocketDoorOpenSoundId = varsReader?.readMicroInt(VARID_DEF_ROCKET_DOOR_SOUND_ID) ?: 0
            val rocketDestroyedExplosionId = varsReader?.readMicroInt(VARID_DEF_ROCKET_DESTROYED_EXPLOSION_ID) ?: 0

            return SakuraBossGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                gattlingGunDefId = gattlingGunDefId,
                rocketLauncherDefId = rocketLauncherDefId,
                gattlingGunRevSoundDefId = gattlingGunRevSoundDefId,
                rocketDoorOpenSoundId = rocketDoorOpenSoundId,
                rocketDestroyedExplosionId = rocketDestroyedExplosionId,
                rocketsDefense = DefenseObjectDefClass(),
            )
        }
    }
}
