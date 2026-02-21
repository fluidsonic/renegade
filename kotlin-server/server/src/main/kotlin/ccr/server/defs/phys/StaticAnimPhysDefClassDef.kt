package ccr.server.defs.phys

import ccr.server.defs.PhysDefClass
import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of AnimCollisionManagerDefClass (wwphys/animcollisionmanager.cpp).
 *
 * C++ defaults: CollisionMode(COLLIDE_PUSH=2), AnimationMode(ANIMATE_LOOP=0), AnimationName("")
 */
data class AnimCollisionManagerDef(
    val collisionMode: Int = 2,   // COLLIDE_PUSH
    val animationMode: Int = 0,   // ANIMATE_LOOP
    val animationName: String = "",
)

/**
 * Kotlin representation of ProjectorManagerDefClass (wwphys/projectormanager.cpp).
 *
 * C++ defaults match constructor initializer list.
 */
data class ProjectorManagerDef(
    val isEnabled: Boolean = false,
    val isPerspective: Boolean = false,
    val isAdditive: Boolean = false,
    val isAnimated: Boolean = false,
    val orthoWidth: Float = 10.0f,
    val orthoHeight: Float = 10.0f,
    val horizontalFOV: Float = 0.17453293f, // DEG_TO_RADF(10)
    val verticalFOV: Float = 0.17453293f,   // DEG_TO_RADF(10)
    val nearZ: Float = 5.0f,
    val farZ: Float = 20.0f,
    val intensity: Float = 1.0f,
    val textureName: String = "",
    val boneName: String = "",
)

/**
 * Kotlin representation of StaticAnimPhysDefClass (wwphys/staticanimphys.h).
 *
 * Inheritance: StaticAnimPhysDefClass -> StaticPhysDefClass -> PhysDefClass -> DefinitionClass
 *
 * C++ defaults match constructor initializer list.
 *
 * Chunk layout inside OBJDATA:
 *   [STATICANIMPHYSDEF_CHUNK_STATICPHYSDEF       = 0x55110100]  -> parent StaticPhysDefClass
 *     [STATICPHYSDEF_CHUNK_PHYSDEF               = 0x01070002]  -> parent PhysDefClass
 *       [PHYSDEF_CHUNK_DEFINITION                = 0x055FFE07]  -> DefinitionClass (name, id)
 *       [PHYSDEF_CHUNK_VARIABLES                 = 0x055FFE08]  -> ModelName, IsPreLit
 *     [STATICPHYSDEF_CHUNK_VARIABLES             = 0x01070003]  -> IsNonOccluder
 *   [STATICANIMPHYSDEF_CHUNK_PROJECTORMANAGERDEF = 0x55110101]  -> ProjectorManagerDef
 *     [PROJECTORMANAGERDEF_CHUNK_VARIABLES       = 0x01110004]  -> projector fields
 *   [STATICANIMPHYSDEF_CHUNK_VARIABLES           = 0x55110102]  -> own fields + legacy overrides
 *   [STATICANIMPHYSDEF_CHUNK_ANIMMANAGERDEF      = 0x55110103]  -> AnimCollisionManagerDef
 *     [ANIMCOLLISIONMANAGERDEF_CHUNK_VARIABLES   = 0x1F4ADE72]  -> anim collision fields
 */
class StaticAnimPhysDefClass(
    name: String,
    id: UInt,
    classId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    isNonOccluder: Boolean = true,
    val shadowDynamicObjs: Boolean = false,
    val shadowIsAdditive: Boolean = false,
    val shadowIgnoresZRotation: Boolean = true,
    val shadowNearZ: Float = 0.5f,
    val shadowFarZ: Float = 5.0f,
    val shadowIntensity: Float = 0.5f,
    val doesCollideInPathfind: Boolean = false,
    val isCosmetic: Boolean = false,
    val animManagerDef: AnimCollisionManagerDef = AnimCollisionManagerDef(),
    val projectorManagerDef: ProjectorManagerDef = ProjectorManagerDef(),
) : StaticPhysDefClass(name, id, classId, modelName, isPreLit, isNonOccluder) {

    companion object {
        /** CLASSID_STATICANIMPHYSDEF = CLASSID_PHYSICS(0x9000) + 8 */
        const val CLASS_ID: UInt = 0x9008u
    }
}

// -- Top-level chunk IDs (staticanimphys.cpp enum) ----------------------------
private const val STATICANIMPHYSDEF_CHUNK_STATICPHYSDEF = 0x55110100u
private const val STATICANIMPHYSDEF_CHUNK_PROJECTORMANAGERDEF = 0x55110101u
private const val STATICANIMPHYSDEF_CHUNK_VARIABLES = 0x55110102u
private const val STATICANIMPHYSDEF_CHUNK_ANIMMANAGERDEF = 0x55110103u

// Parent chunk IDs (from staticphys.cpp, needed to parse parent chain)
private const val STATICPHYSDEF_CHUNK_PHYSDEF = 0x01070002u
private const val STATICPHYSDEF_CHUNK_VARIABLES = 0x01070003u
private const val STATICPHYSDEF_MCID_ISNONOCCLUDER = 0x00

// -- Own micro-chunk IDs (staticanimphys.cpp enum, starting at 0x00) ----------
private const val MCID_COLLISIONMODE = 0x00          // legacy -> animManagerDef
private const val MCID_SHADOWDYNAMICOBJS = 0x01
private const val MCID_SHADOWISADDITIVE = 0x02
private const val MCID_SHADOWIGNORESZROTATION = 0x03
private const val MCID_SHADOWFARZ = 0x04
private const val MCID_SHADOWINTENSITY = 0x05
private const val MCID_SHADOWNEARZ = 0x06
private const val MCID_ANIMATIONNAME = 0x07           // legacy -> animManagerDef
private const val MCID_COLLIDEINPATHFIND = 0x08
private const val MCID_ISCOSMETIC = 0x09

// -- AnimCollisionManagerDef chunk IDs (animcollisionmanager.cpp) -------------
private const val ANIMCOLLISIONMANAGERDEF_CHUNK_VARIABLES = 0x1F4ADE72u // 525000306 decimal
private const val ANIMCOLLISIONMANAGERDEF_MCID_COLLISIONMODE = 0x00
private const val ANIMCOLLISIONMANAGERDEF_MCID_ANIMATIONMODE = 0x01
private const val ANIMCOLLISIONMANAGERDEF_MCID_ANIMATIONNAME = 0x02

// -- ProjectorManagerDef chunk IDs (projectormanager.cpp) ---------------------
private const val PROJECTORMANAGERDEF_CHUNK_VARIABLES = 0x01110004u
private const val PROJECTORMANAGERDEF_MCID_ISENABLED = 0x00
private const val PROJECTORMANAGERDEF_MCID_ISPERSPECTIVE = 0x01
private const val PROJECTORMANAGERDEF_MCID_ISADDITIVE = 0x02
private const val PROJECTORMANAGERDEF_MCID_ISANIMATED = 0x03
private const val PROJECTORMANAGERDEF_MCID_ORTHOWIDTH = 0x04
private const val PROJECTORMANAGERDEF_MCID_ORTHOHEIGHT = 0x05
private const val PROJECTORMANAGERDEF_MCID_HORIZONTALFOV = 0x06
private const val PROJECTORMANAGERDEF_MCID_VERTICALFOV = 0x07
private const val PROJECTORMANAGERDEF_MCID_NEARZ = 0x08
private const val PROJECTORMANAGERDEF_MCID_FARZ = 0x09
private const val PROJECTORMANAGERDEF_MCID_TEXTURENAME = 0x0A
private const val PROJECTORMANAGERDEF_MCID_BONENAME = 0x0B
private const val PROJECTORMANAGERDEF_MCID_INTENSITY = 0x0C

fun parseStaticAnimPhysDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): StaticAnimPhysDefClass {

    // -- Parse parent StaticPhysDefClass fields from nested parent chunk ------
    val staticPhysChunk = objDataReader.findChunk(STATICANIMPHYSDEF_CHUNK_STATICPHYSDEF)

    // PhysDefClass fields (nested inside StaticPhysDefClass -> PhysDefClass)
    val physDefChunk = staticPhysChunk?.findChunk(STATICPHYSDEF_CHUNK_PHYSDEF)
    val physDefVars = physDefChunk?.findChunk(PhysDefClass.CHUNK_VARIABLES)

    val modelName = physDefVars?.readMicroString(PhysDefClass.VARIABLE_MODELNAME) ?: "NULL"
    val isPreLit = physDefVars?.readMicroBool(PhysDefClass.VARIABLE_ISPRELIT) ?: false

    // StaticPhysDefClass fields
    val staticPhysVars = staticPhysChunk?.findChunk(STATICPHYSDEF_CHUNK_VARIABLES)
    val isNonOccluder = staticPhysVars?.readMicroBool(STATICPHYSDEF_MCID_ISNONOCCLUDER) ?: true

    // -- Parse ProjectorManagerDef --------------------------------------------
    val projectorManagerDef = objDataReader.findChunk(STATICANIMPHYSDEF_CHUNK_PROJECTORMANAGERDEF)
        ?.findChunk(PROJECTORMANAGERDEF_CHUNK_VARIABLES)
        ?.let { vars -> parseProjectorManagerDef(vars) }
        ?: ProjectorManagerDef()

    // -- Parse AnimCollisionManagerDef ----------------------------------------
    var animManagerDef = objDataReader.findChunk(STATICANIMPHYSDEF_CHUNK_ANIMMANAGERDEF)
        ?.findChunk(ANIMCOLLISIONMANAGERDEF_CHUNK_VARIABLES)
        ?.let { vars -> parseAnimCollisionManagerDef(vars) }
        ?: AnimCollisionManagerDef()

    // -- Parse own variables --------------------------------------------------
    val vars = objDataReader.findChunk(STATICANIMPHYSDEF_CHUNK_VARIABLES)

    val shadowDynamicObjs = vars?.readMicroBool(MCID_SHADOWDYNAMICOBJS) ?: false
    val shadowIsAdditive = vars?.readMicroBool(MCID_SHADOWISADDITIVE) ?: false
    val shadowIgnoresZRotation = vars?.readMicroBool(MCID_SHADOWIGNORESZROTATION) ?: true
    val shadowFarZ = vars?.readMicroFloat(MCID_SHADOWFARZ) ?: 5.0f
    val shadowIntensity = vars?.readMicroFloat(MCID_SHADOWINTENSITY) ?: 0.5f
    val shadowNearZ = vars?.readMicroFloat(MCID_SHADOWNEARZ) ?: 0.5f
    val doesCollideInPathfind = vars?.readMicroBool(MCID_COLLIDEINPATHFIND) ?: false
    val isCosmetic = vars?.readMicroBool(MCID_ISCOSMETIC) ?: false

    // Legacy overrides (moved to AnimManagerDef in later versions)
    val legacyCollisionMode = vars?.readMicroInt(MCID_COLLISIONMODE)
    val legacyAnimationName = vars?.readMicroString(MCID_ANIMATIONNAME)

    // Apply legacy overrides from VARIABLES chunk into animManagerDef
    if (legacyCollisionMode != null || legacyAnimationName != null) {
        animManagerDef = animManagerDef.copy(
            collisionMode = legacyCollisionMode ?: animManagerDef.collisionMode,
            animationName = legacyAnimationName ?: animManagerDef.animationName,
        )
    }

    return StaticAnimPhysDefClass(
        name = name,
        id = id,
        classId = classId,
        modelName = modelName,
        isPreLit = isPreLit,
        isNonOccluder = isNonOccluder,
        shadowDynamicObjs = shadowDynamicObjs,
        shadowIsAdditive = shadowIsAdditive,
        shadowIgnoresZRotation = shadowIgnoresZRotation,
        shadowNearZ = shadowNearZ,
        shadowFarZ = shadowFarZ,
        shadowIntensity = shadowIntensity,
        doesCollideInPathfind = doesCollideInPathfind,
        isCosmetic = isCosmetic,
        animManagerDef = animManagerDef,
        projectorManagerDef = projectorManagerDef,
    )
}

private fun parseAnimCollisionManagerDef(vars: ChunkReader): AnimCollisionManagerDef {
    return AnimCollisionManagerDef(
        collisionMode = vars.readMicroInt(ANIMCOLLISIONMANAGERDEF_MCID_COLLISIONMODE) ?: 2,
        animationMode = vars.readMicroInt(ANIMCOLLISIONMANAGERDEF_MCID_ANIMATIONMODE) ?: 0,
        animationName = vars.readMicroString(ANIMCOLLISIONMANAGERDEF_MCID_ANIMATIONNAME) ?: "",
    )
}

private fun parseProjectorManagerDef(vars: ChunkReader): ProjectorManagerDef {
    return ProjectorManagerDef(
        isEnabled = vars.readMicroBool(PROJECTORMANAGERDEF_MCID_ISENABLED) ?: false,
        isPerspective = vars.readMicroBool(PROJECTORMANAGERDEF_MCID_ISPERSPECTIVE) ?: false,
        isAdditive = vars.readMicroBool(PROJECTORMANAGERDEF_MCID_ISADDITIVE) ?: false,
        isAnimated = vars.readMicroBool(PROJECTORMANAGERDEF_MCID_ISANIMATED) ?: false,
        orthoWidth = vars.readMicroFloat(PROJECTORMANAGERDEF_MCID_ORTHOWIDTH) ?: 10.0f,
        orthoHeight = vars.readMicroFloat(PROJECTORMANAGERDEF_MCID_ORTHOHEIGHT) ?: 10.0f,
        horizontalFOV = vars.readMicroFloat(PROJECTORMANAGERDEF_MCID_HORIZONTALFOV) ?: 0.17453293f,
        verticalFOV = vars.readMicroFloat(PROJECTORMANAGERDEF_MCID_VERTICALFOV) ?: 0.17453293f,
        nearZ = vars.readMicroFloat(PROJECTORMANAGERDEF_MCID_NEARZ) ?: 5.0f,
        farZ = vars.readMicroFloat(PROJECTORMANAGERDEF_MCID_FARZ) ?: 20.0f,
        intensity = vars.readMicroFloat(PROJECTORMANAGERDEF_MCID_INTENSITY) ?: 1.0f,
        textureName = vars.readMicroString(PROJECTORMANAGERDEF_MCID_TEXTURENAME) ?: "",
        boneName = vars.readMicroString(PROJECTORMANAGERDEF_MCID_BONENAME) ?: "",
    )
}
