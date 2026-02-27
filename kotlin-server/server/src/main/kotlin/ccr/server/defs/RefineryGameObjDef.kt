package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of RefineryGameObjDef (Combat/refinerygameobj.h / refinerygameobj.cpp).
 *
 * C++ hierarchy: RefineryGameObjDef : BuildingGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (refinerygameobj.cpp constructor):
 *   UnloadTime(0), FundsGathered(0), FundsDistributedPerSec(0), HarvesterDefID(0)
 */
open class RefineryGameObjDef(
    // BuildingGameObjDef base params
    name: String,
    id: UInt,
    chunkId: UInt,
    defenseObjectDef: DefenseObjectDefClass = DefenseObjectDefClass(),
    infoIconTextureFilename: String = "",
    translatedNameId: Int = 0,
    notTargetable: Boolean = false,
    defaultPlayerType: Int = BuildingGameObjDef.PLAYERTYPE_NEUTRAL,
    meshPrefix: String = "",
    mctSkin: Int = 0,
    buildingType: Int = BuildingGameObjDef.BUILDING_TYPE_NONE,
    gdiDamageReportId: Int = 0,
    nodDamageReportId: Int = 0,
    gdiDestroyReportId: Int = 0,
    nodDestroyReportId: Int = 0,

    // C++: float UnloadTime (initialized to 0)
    val unloadTime: Float = 0f,

    // C++: float FundsGathered (initialized to 0)
    val fundsGathered: Float = 0f,

    // C++: float FundsDistributedPerSec (initialized to 0)
    val fundsDistributedPerSec: Float = 0f,

    // C++: int HarvesterDefID (initialized to 0)
    val harvesterDefId: Int = 0,
) : BuildingGameObjDef(
    name = name,
    id = id,
    chunkId = chunkId,
    defenseObjectDef = defenseObjectDef,
    infoIconTextureFilename = infoIconTextureFilename,
    translatedNameId = translatedNameId,
    notTargetable = notTargetable,
    defaultPlayerType = defaultPlayerType,
    meshPrefix = meshPrefix,
    mctSkin = mctSkin,
    buildingType = buildingType,
    gdiDamageReportId = gdiDamageReportId,
    nodDamageReportId = nodDamageReportId,
    gdiDestroyReportId = gdiDestroyReportId,
    nodDestroyReportId = nodDestroyReportId,
) {

    companion object {
        /** Persist factory chunk ID for RefineryGameObjDef in the DDB. */
        const val CHUNK_ID: UInt = 0x00040138u  // CHUNKID_GAME_OBJECT_DEF_REFINERY

        // --- Chunk IDs from refinerygameobj.cpp local enum ---
        // enum { CHUNKID_DEF_PARENT = 0x02200638, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT    = 0x02200638u
        private const val CHUNKID_DEF_VARIABLES = 0x02200639u

        // --- Micro-chunk IDs for CHUNKID_DEF_VARIABLES ---
        private const val MICROCHUNKID_DEF_UNLOAD_TIME    = 1
        private const val MICROCHUNKID_DEF_FUNDS_GATHERED = 2
        private const val MICROCHUNKID_DEF_HARVESTER_DEFID= 3
        private const val MICROCHUNKID_DEF_FUNDS_PER_SEC  = 4

        /**
         * Loads a RefineryGameObjDef from the OBJDATA chunk of a definition entry.
         * Delegates base class loading to BuildingGameObjDef.load(), then reads
         * the refinery-specific variables chunk.
         * Returns null if required base fields (id, name) are missing.
         */
        fun load(objDataChunk: ChunkReader, chunkId: UInt): RefineryGameObjDef? {
            // Delegate to BuildingGameObjDef to load all base fields
            val base = BuildingGameObjDef.load(objDataChunk, chunkId) ?: return null

            // Read the refinery-specific variables chunk
            val vars = objDataChunk.findChunk(CHUNKID_DEF_VARIABLES)

            val unloadTime          = vars?.mcFloat(MICROCHUNKID_DEF_UNLOAD_TIME)    ?: 0f
            val fundsGathered       = vars?.mcFloat(MICROCHUNKID_DEF_FUNDS_GATHERED) ?: 0f
            val fundsDistributedPerSec = vars?.mcFloat(MICROCHUNKID_DEF_FUNDS_PER_SEC) ?: 0f
            val harvesterDefId      = vars?.mcInt(MICROCHUNKID_DEF_HARVESTER_DEFID)  ?: 0

            return RefineryGameObjDef(
                name                   = base.name,
                id                     = base.id,
                chunkId                = base.chunkId,
                defenseObjectDef       = base.defenseObjectDef,
                infoIconTextureFilename= base.infoIconTextureFilename,
                translatedNameId       = base.translatedNameId,
                notTargetable          = base.notTargetable,
                defaultPlayerType      = base.defaultPlayerType,
                meshPrefix             = base.meshPrefix,
                mctSkin                = base.mctSkin,
                buildingType           = base.buildingType,
                gdiDamageReportId      = base.gdiDamageReportId,
                nodDamageReportId      = base.nodDamageReportId,
                gdiDestroyReportId     = base.gdiDestroyReportId,
                nodDestroyReportId     = base.nodDestroyReportId,
                unloadTime             = unloadTime,
                fundsGathered          = fundsGathered,
                fundsDistributedPerSec = fundsDistributedPerSec,
                harvesterDefId         = harvesterDefId,
            )
        }

        /** Convenience overload — name/id already extracted by caller; delegates to full load(). */
        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): RefineryGameObjDef {
            return load(objDataReader, chunkId) ?: RefineryGameObjDef(name = name, id = id, chunkId = chunkId)
        }
    }

    override fun toString(): String =
        "RefineryGameObjDef(id=$id, name='$name', harvesterDefId=$harvesterDefId, " +
        "unloadTime=$unloadTime, fundsGathered=$fundsGathered, fundsDistributedPerSec=$fundsDistributedPerSec)"
}
