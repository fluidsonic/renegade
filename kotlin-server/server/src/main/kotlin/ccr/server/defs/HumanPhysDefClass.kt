package ccr.server.defs

import ccr.physics.PhysClass
import ccr.physics.moveable.HumanPhysClass
import ccr.server.core.ChunkReader
import kotlin.math.cos
import kotlin.math.tan

/**
 * Kotlin representation of HumanPhysDefClass (wwphys/humanphys.h).
 *
 * Inheritance chain:
 *   HumanPhysDefClass → Phys3DefClass → MoveablePhysDefClass →
 *   DynamicPhysDefClass → PhysDefClass → DefinitionClass
 *
 * HumanPhysDefClass has no own saved fields — all fields come from parents.
 * C++ comment: "No variables for now..."
 *
 * Chunk layout inside OBJDATA:
 *   [HUMANPHYSDEF_CHUNK_PHYS3DEF = 0x00516000] → parent Phys3DefClass data
 */
class HumanPhysDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    mass: Float = 1f,
    gravScale: Float = 1f,
    elasticity: Float = 0.1f,
    cinematicCollisionMode: Int = MoveablePhysDefClass.CINEMATIC_COLLISION_PUSH,
    normSpeed: Float = 10f,
    slideAngle: Float = 0.7853982f, // DEG_TO_RADF(45)
    stepHeight: Float = 0.25f,
) : Phys3DefClass(name, id, chunkId, modelName, isPreLit, mass, gravScale, elasticity, cinematicCollisionMode, normSpeed, slideAngle, stepHeight) {

    /**
     * C++: HumanPhysDefClass::Create()
     * Creates a HumanPhysClass and initialises it via Init(*this).
     * C++ HumanPhysClass::Init calls Phys3Class::Init (which calls MoveablePhysClass::Init).
     */
    override fun create(): PhysClass {
        val obj = HumanPhysClass()
        // MoveablePhysClass::Init
        obj.mass = mass
        obj.gravScale = gravScale
        obj.elasticity = elasticity
        // Phys3Class::Init
        obj.normSpeed = normSpeed
        obj.slideAngle = slideAngle
        obj.slideNormalZ = cos(slideAngle)
        obj.slideAngleTan = tan(slideAngle)
        obj.stepHeight = stepHeight
        return obj
    }

    companion object {
        const val CHUNK_ID: UInt = 0x00020501u  // PHYSICS_CHUNKID_HUMANPHYSDEF

        // Chunk ID from humanphys.cpp: wraps Phys3DefClass data
        private const val HUMANPHYSDEF_CHUNK_PHYS3DEF = 0x00516000u

        /**
         * Loads a HumanPhysDefClass from the OBJDATA chunk.
         *
         * Chunk nesting:
         * ```
         * [HUMANPHYSDEF_CHUNK_PHYS3DEF]
         *   [PHYS3DEF_CHUNK_MOVEABLEPHYSDEF]
         *     [MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF]
         *       [DYNAMICPHYSDEF_CHUNK_PHYSDEF]
         *         [PHYSDEF_CHUNK_DEFINITION] → DefinitionClass
         *         [PHYSDEF_CHUNK_VARIABLES]  → modelName, isPreLit
         *     [MOVEABLEPHYSDEF_CHUNK_VARIABLES] → mass, gravScale, elasticity, cinematicCollisionMode
         *   [PHYS3DEF_CHUNK_VARIABLES] → normSpeed, slideAngle, stepHeight
         * ```
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, chunkId: UInt): HumanPhysDefClass {
            val phys3Chunk = objDataChunk.findChunk(HUMANPHYSDEF_CHUNK_PHYS3DEF)
            val fields = if (phys3Chunk != null) {
                Phys3DefClass.parseFields(phys3Chunk)
            } else {
                Phys3DefClass.ParsedFields(
                    "NULL", false, 1f, 1f, 0.1f,
                    MoveablePhysDefClass.CINEMATIC_COLLISION_PUSH, 10f, 0.7853982f, 0.25f,
                )
            }

            return HumanPhysDefClass(
                name = name,
                id = id,
                chunkId = chunkId,
                modelName = fields.modelName,
                isPreLit = fields.isPreLit,
                mass = fields.mass,
                gravScale = fields.gravScale,
                elasticity = fields.elasticity,
                cinematicCollisionMode = fields.cinematicCollisionMode,
                normSpeed = fields.normSpeed,
                slideAngle = fields.slideAngle,
                stepHeight = fields.stepHeight,
            )
        }
    }
}
