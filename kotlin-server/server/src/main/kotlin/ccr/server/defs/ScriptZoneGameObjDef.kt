package ccr.server.defs

import ccr.math.Vector3
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// C++: ScriptZoneGameObjDef : public ScriptableGameObjDef (scriptzone.h / scriptzone.cpp)
// C++ hierarchy: ScriptZoneGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
open class ScriptZoneGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    // C++: ZoneConstants::ZoneType ZoneType (initialized to TYPE_DEFAULT = 0)
    val zoneType: Int = ZoneType.TYPE_DEFAULT,
    // C++: Vector3 Color (initialized to (0, 0.7f, 0))
    // @JvmName avoids clash with fun getColor() below
    @get:JvmName("colorField")
    val color: Vector3 = Vector3(0f, 0.7f, 0f),
    // C++: bool IsCTFZone (not initialized in constructor — legacy field)
    val isCTFZone: Boolean = false,
    // C++: bool CheckStarsOnly (initialized to true)
    val checkStarsOnly: Boolean = true,
    // C++: bool IsEnvironmentZone (initialized to false)
    val isEnvironmentZone: Boolean = false,
    // ScriptableGameObjDef fields (forwarded)
    scriptNameList: List<String> = emptyList(),
    scriptParameterList: List<String> = emptyList(),
) : ScriptableGameObjDef(name, id, chunkId, scriptNameList, scriptParameterList) {

    // C++: ZoneConstants::ZoneType Get_Type() const { return ZoneType; }
    fun getType(): Int = zoneType

    // C++: const Vector3 & Get_Color() const { return Color; }
    fun getColor(): Vector3 = color

    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_SCRIPT_ZONE = 0x00040123 (combatchunkid.h)
        const val CHUNK_ID: UInt = 0x00040123u

        // C++: CLASSID_GAME_OBJECT_DEF_SCRIPT_ZONE = 0x300E (combatchunkid.h)
        const val CLASS_ID: UInt = 0x300Eu

        // Chunk IDs from scriptzone.cpp local enum (starting at XXXCHUNKID_DEF_PARENT_OLD = 1111991132)
        const val XXXCHUNKID_DEF_PARENT_OLD: Int = 1111991132  // legacy, not written in Save
        const val CHUNKID_DEF_VARIABLES: Int     = 1111991133
        const val CHUNKID_DEF_PARENT: Int        = 1111991134

        // Micro-chunk IDs inside CHUNKID_DEF_VARIABLES
        const val MICROCHUNKID_DEF_IS_CTF_ZONE:         Int = 1  // legacy — not written in Save
        const val MICROCHUNKID_DEF_ZONE_COLOR:           Int = 2
        const val MICROCHUNKID_DEF_CHECK_STARS_ONLY:     Int = 3
        const val MICROCHUNKID_DEF_ZONE_TYPE:            Int = 4
        const val MICROCHUNKID_DEF_IS_ENVIRONMENT_ZONE:  Int = 5

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): ScriptZoneGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES.toUInt())

            val colorBytes = vars?.findMicroChunk(MICROCHUNKID_DEF_ZONE_COLOR)
            val color = if (colorBytes != null && colorBytes.size >= 12) {
                val buf = ByteBuffer.wrap(colorBytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
                Vector3(buf.getFloat(0), buf.getFloat(4), buf.getFloat(8))
            } else {
                Vector3(0f, 0.7f, 0f)
            }

            val checkStarsOnly = vars?.findMicroChunk(MICROCHUNKID_DEF_CHECK_STARS_ONLY)?.let {
                if (it.isNotEmpty()) it[0] != 0.toByte() else null
            } ?: true

            val zoneType = vars?.findMicroChunk(MICROCHUNKID_DEF_ZONE_TYPE)?.let {
                if (it.size >= 4) ByteBuffer.wrap(it, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int else null
            } ?: 0

            val isEnvironmentZone = vars?.findMicroChunk(MICROCHUNKID_DEF_IS_ENVIRONMENT_ZONE)?.let {
                if (it.isNotEmpty()) it[0] != 0.toByte() else null
            } ?: false

            // Navigate ScriptZone parent → ScriptableGameObjDef::Save to load script lists
            val szParent = objDataReader.findChunk(CHUNKID_DEF_PARENT.toUInt())
            val (scriptableData, _) = szParent?.let { ScriptableGameObjDefData.load(it) }
                ?: (ScriptableGameObjDefData(emptyList()) to null)

            return ScriptZoneGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                color = color,
                checkStarsOnly = checkStarsOnly,
                zoneType = zoneType,
                isEnvironmentZone = isEnvironmentZone,
                // ScriptableGameObjDef fields
                scriptNameList = scriptableData.scripts.map { it.name },
                scriptParameterList = scriptableData.scripts.map { it.parameters },
            )
        }
    }
}

// C++: ZoneConstants namespace (scriptzone.h)
object ZoneType {
    const val TYPE_DEFAULT              = 0
    const val TYPE_CTF                  = 1
    const val TYPE_VEHICLE_CONSTRUCTION = 2
    const val TYPE_VEHICLE_REPAIR       = 3
    const val TYPE_TIBERIUM_FIELD       = 4
    const val TYPE_BEACON               = 5
    const val TYPE_GDI_TIB_FIELD        = 6
    const val TYPE_NOD_TIB_FIELD        = 7
    const val TYPE_COUNT                = 8
}
