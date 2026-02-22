package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * DynamicAnimPhysDefClass definition.
 *
 * C++ source: wwphys/dynamicanimphys.cpp
 * Inherits: DecorationPhysDefClass -> DynamicPhysDefClass -> PhysDefClass -> DefinitionClass
 *
 * C++ defaults: CastsShadows(false), ShadowNearZ(-1.0f), ShadowFarZ(-1.0f)
 * AnimCollisionManagerDefClass defaults: CollisionMode(COLLIDE_PUSH=2), AnimationMode(ANIMATE_LOOP=0)
 */
class DynamicAnimPhysDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    // AnimCollisionManagerDefClass fields (sub-object in C++)
    val animCollisionMode: Int = 2,
    val animAnimationMode: Int = 0,
    val animAnimationName: String = "",
    // Own fields
    val castsShadows: Boolean = false,
    val shadowNearZ: Float = -1.0f,
    val shadowFarZ: Float = -1.0f,
) : DecorationPhysDefClass(name, id, chunkId, modelName, isPreLit) {

    companion object {
        const val CHUNK_ID: UInt = 0x0002050Fu  // PHYSICS_CHUNKID_DYNAMICANIMPHYSDEF
    }
}

// Chunk IDs for DynamicAnimPhysDefClass
// C++ uses octal literal 052600316 = 11206862 decimal = 0x00AB00CE
private const val CHUNK_DECOPHYSDEF = 0x00AB00CEu
private const val CHUNK_ANIMMANAGERDEF = 0x00AB00CFu
private const val CHUNK_VARIABLES = 0x00AB00D0u

// DecorationPhysDefClass parent chunk IDs
private const val DECORATIONPHYSDEF_CHUNK_DYNAMICPHYSDEF = 0x01070004u

// DynamicPhysDefClass parent chunk IDs
private const val DYNAMICPHYSDEF_CHUNK_PHYSDEF = 813001104u

// AnimCollisionManagerDefClass chunk
private const val ANIMCOLLISIONMANAGERDEF_CHUNK_VARIABLES = 525000306u // 0x1F4ADE72

// AnimCollisionManagerDefClass micro-chunk IDs
private const val ANIM_MCID_COLLISION_MODE = 0x00
private const val ANIM_MCID_ANIMATION_MODE = 0x01
private const val ANIM_MCID_ANIMATION_NAME = 0x02

// Own micro-chunk IDs
private const val MCID_CASTS_SHADOWS = 0x01
private const val MCID_SHADOW_NEAR_Z = 0x02
private const val MCID_SHADOW_FAR_Z = 0x03

fun parseDynamicAnimPhysDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): DynamicAnimPhysDefClass {
    // Navigate the parent chunk chain to reach PhysDefClass level:
    // CHUNK_DECOPHYSDEF -> DECORATIONPHYSDEF_CHUNK_DYNAMICPHYSDEF -> DYNAMICPHYSDEF_CHUNK_PHYSDEF
    val decoPhysChunk = objDataReader.findChunk(CHUNK_DECOPHYSDEF)
    val dynPhysChunk = decoPhysChunk?.findChunk(DECORATIONPHYSDEF_CHUNK_DYNAMICPHYSDEF)
    val physDefChunk = dynPhysChunk?.findChunk(DYNAMICPHYSDEF_CHUNK_PHYSDEF)

    // Parse PhysDefClass fields (modelName, isPreLit) at the correct chunk level
    val (modelName, isPreLit) = if (physDefChunk != null) {
        PhysDefClass.parseFields(physDefChunk)
    } else {
        "NULL" to false
    }

    // Parse AnimCollisionManagerDef variables
    val animManagerChunk = objDataReader.findChunk(CHUNK_ANIMMANAGERDEF)
    val animVars = animManagerChunk?.findChunk(ANIMCOLLISIONMANAGERDEF_CHUNK_VARIABLES)

    // Parse own variables
    val ownVars = objDataReader.findChunk(CHUNK_VARIABLES)

    return DynamicAnimPhysDefClass(
        name = name,
        id = id,
        chunkId = chunkId,
        modelName = modelName,
        isPreLit = isPreLit,
        // AnimCollisionManagerDefClass
        animCollisionMode = animVars?.readMicroInt(ANIM_MCID_COLLISION_MODE) ?: 2,
        animAnimationMode = animVars?.readMicroInt(ANIM_MCID_ANIMATION_MODE) ?: 0,
        animAnimationName = animVars?.readMicroString(ANIM_MCID_ANIMATION_NAME) ?: "",
        // Own fields
        castsShadows = ownVars?.readMicroBool(MCID_CASTS_SHADOWS) ?: false,
        shadowNearZ = ownVars?.readMicroFloat(MCID_SHADOW_NEAR_Z) ?: -1.0f,
        shadowFarZ = ownVars?.readMicroFloat(MCID_SHADOW_FAR_Z) ?: -1.0f,
    )
}
