package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of WarFactoryGameObjDef (Combat/warfactorygameobj.h/.cpp).
 *
 * C++ hierarchy: WarFactoryGameObjDef : VehicleFactoryGameObjDef : BuildingGameObjDef :
 *   DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * WarFactoryGameObjDef adds NO persisted fields of its own. Its Save() wraps
 * VehicleFactoryGameObjDef::Save() in CHUNKID_DEF_PARENT (0x02200638) and writes an
 * empty CHUNKID_DEF_VARIABLES (0x02200639).
 */
// C++: class WarFactoryGameObjDef : public VehicleFactoryGameObjDef
class WarFactoryGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    padClearingWarhead: Int = 25,
    totalBuildingTime: Float = 12f,
) : VehicleFactoryGameObjDef(
    name = name,
    id = id,
    chunkId = chunkId,
    padClearingWarhead = padClearingWarhead,
    totalBuildingTime = totalBuildingTime,
) {

    companion object {
        /** CHUNKID_GAME_OBJECT_DEF_WARFACTORY = 0x00040142 (combatchunkid.h) */
        const val CHUNK_ID: UInt = 0x00040142u

        // C++: CLASSID_GAME_OBJECT_DEF_WARFACTORY = 0xD007 (combatchunkid.h)
        const val CLASS_ID: UInt = 0xD007u

        // C++: local enum in warfactorygameobj.cpp
        // enum { CHUNKID_DEF_PARENT = 0x02200638, CHUNKID_DEF_VARIABLES, MICROCHUNKID_DEF_UNUSED = 1 }
        private const val CHUNKID_DEF_PARENT    = 0x02200638u
        private const val CHUNKID_DEF_VARIABLES = 0x02200639u

        // Micro-chunk IDs from VehicleFactoryGameObjDef (vehiclefactorygameobj.cpp)
        // enum { ..., MICROCHUNKID_DEF_PADCLEARINGWARHEAD = 2, MICROCHUNKID_DEF_TOTALBUILDINGTIME = 3 }
        private const val MICROCHUNKID_DEF_PADCLEARINGWARHEAD = 2
        private const val MICROCHUNKID_DEF_TOTALBUILDINGTIME  = 3

        /**
         * Parses a WarFactoryGameObjDef from the OBJDATA chunk.
         * No definition-specific fields to extract beyond what VehicleFactoryGameObjDef provides.
         *
         * Save layout:
         *   CHUNKID_DEF_PARENT(0x02200638) → VehicleFactoryGameObjDef::Save
         *     CHUNKID_DEF_PARENT(0x02200638) → BuildingGameObjDef::Save (and parents)
         *     CHUNKID_DEF_VARIABLES(0x02200639) → micro 2=padClearingWarhead, 3=totalBuildingTime
         *   CHUNKID_DEF_VARIABLES(0x02200639) → (empty — no WarFactory-specific fields)
         *
         * [name], [id], and [chunkId] are already extracted by the definition DB reader.
         */
        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): WarFactoryGameObjDef {
            // Navigate into CHUNKID_DEF_PARENT to access VehicleFactoryDef's CHUNKID_DEF_VARIABLES
            val vehicleFactoryParent = objDataReader.findChunk(CHUNKID_DEF_PARENT)
            val vehicleFactoryVars = vehicleFactoryParent?.findChunk(CHUNKID_DEF_VARIABLES)

            val padClearingWarhead = vehicleFactoryVars?.readMicroInt(MICROCHUNKID_DEF_PADCLEARINGWARHEAD) ?: 25
            val totalBuildingTime  = vehicleFactoryVars?.readMicroFloat(MICROCHUNKID_DEF_TOTALBUILDINGTIME) ?: 12f

            return WarFactoryGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                padClearingWarhead = padClearingWarhead,
                totalBuildingTime = totalBuildingTime,
            )
        }
    }
}
