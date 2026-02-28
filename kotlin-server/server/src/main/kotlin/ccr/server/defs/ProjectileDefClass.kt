package ccr.server.defs

import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.moveable.ProjectileClass
import ccr.server.core.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of ProjectileDefClass (wwphys/projectile.h).
 * Inherits from MoveablePhysDefClass.
 *
 * C++ defaults:
 *   CollidesOnMove(true), OrientationMode(ORIENTATION_ALIGNED=0),
 *   TumbleAxis(1,2,1), TumbleRate(DEG_TO_RADF(10)), Lifetime(2.0f), BounceCount(0)
 */
class ProjectileDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    mass: Float = 1f,
    gravScale: Float = 1f,
    elasticity: Float = 0.1f,
    cinematicCollisionMode: Int = CINEMATIC_COLLISION_PUSH,
    val collidesOnMove: Boolean = true,
    val orientationMode: Int = ORIENTATION_ALIGNED,
    val tumbleAxisX: Float = 1f,
    val tumbleAxisY: Float = 2f,
    val tumbleAxisZ: Float = 1f,
    val tumbleRate: Float = 0.17453292f, // DEG_TO_RADF(10)
    val lifetime: Float = 2f,
    val bounceCount: Int = 0,
) : MoveablePhysDefClass(name, id, chunkId, modelName, isPreLit, mass, gravScale, elasticity, cinematicCollisionMode) {

    /**
     * C++: ProjectileDefClass::Create()
     * Creates a ProjectileClass and initialises it via Init(*this).
     * C++ ProjectileClass::Init calls MoveablePhysClass::Init then sets projectile-specific fields.
     * TumbleAxis is normalised in C++ Init.
     */
    override fun create(): PhysClass {
        val obj = ProjectileClass()
        // MoveablePhysClass::Init
        obj.mass = mass
        obj.gravScale = gravScale
        obj.elasticity = elasticity
        // ProjectileClass::Init
        obj.collidesOnMove = collidesOnMove
        obj.orientationMode = when (orientationMode) {
            ORIENTATION_ALIGNED -> ProjectileClass.OrientationMode.ALIGNED
            ORIENTATION_FIXED -> ProjectileClass.OrientationMode.FIXED
            ORIENTATION_TUMBLING -> ProjectileClass.OrientationMode.TUMBLING
            else -> ProjectileClass.OrientationMode.ALIGNED
        }
        val rawAxis = Vector3(tumbleAxisX, tumbleAxisY, tumbleAxisZ)
        val axisLen = rawAxis.length()
        obj.tumbleAxis = if (axisLen > 0f) rawAxis / axisLen else Vector3(1f, 0f, 0f)
        obj.tumbleRate = tumbleRate
        obj.lifetime = lifetime
        obj.maxBounces = bounceCount
        return obj
    }

    companion object {
        const val CHUNK_ID: UInt = 0x00020506u  // PHYSICS_CHUNKID_PROJECTILEDEF

        const val ORIENTATION_ALIGNED = 0
        const val ORIENTATION_FIXED = 1
        const val ORIENTATION_TUMBLING = 2

        // Chunk IDs from projectile.cpp local enum
        private const val PROJECTILEDEF_CHUNK_MOVEABLEPHYSDEF = 0x01210011u
        private const val PROJECTILEDEF_CHUNK_VARIABLES = 0x01210012u

        // Micro-chunk IDs (starting at 0x00)
        private const val PROJECTILEDEF_VARIABLE_COLLIDESONMOVE = 0x00
        private const val PROJECTILEDEF_VARIABLE_ORIENTATIONMODE = 0x01
        private const val PROJECTILEDEF_VARIABLE_TUMBLEAXIS = 0x02
        private const val PROJECTILEDEF_VARIABLE_TUMBLERATE = 0x03
        private const val PROJECTILEDEF_VARIABLE_LIFETIME = 0x04
        private const val PROJECTILEDEF_VARIABLE_BOUNCECOUNT = 0x05

        /**
         * Loads a ProjectileDefClass from the OBJDATA chunk.
         *
         * Chunk nesting:
         * ```
         * [PROJECTILEDEF_CHUNK_MOVEABLEPHYSDEF]
         *   [MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF]
         *     [DYNAMICPHYSDEF_CHUNK_PHYSDEF]
         *       [PHYSDEF_CHUNK_DEFINITION] → DefinitionClass
         *       [PHYSDEF_CHUNK_VARIABLES]  → modelName, isPreLit
         *   [MOVEABLEPHYSDEF_CHUNK_VARIABLES] → mass, gravScale, elasticity, cinematicCollisionMode
         * [PROJECTILEDEF_CHUNK_VARIABLES] → collidesOnMove, orientationMode, tumbleAxis, tumbleRate, lifetime, bounceCount
         * ```
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, chunkId: UInt): ProjectileDefClass {
            // Parse parent chain
            val moveableChunk = objDataChunk.findChunk(PROJECTILEDEF_CHUNK_MOVEABLEPHYSDEF)
            val parentFields = if (moveableChunk != null) {
                MoveablePhysDefClass.parseFields(moveableChunk)
            } else {
                MoveablePhysDefClass.ParsedFields("NULL", false, 1f, 1f, 0.1f, CINEMATIC_COLLISION_PUSH)
            }

            // Own fields
            val vars = objDataChunk.findChunk(PROJECTILEDEF_CHUNK_VARIABLES)

            val collidesOnMove = vars?.let { PhysDefClass.microChunkBool(it, PROJECTILEDEF_VARIABLE_COLLIDESONMOVE) } ?: true
            val orientationMode = vars?.let { PhysDefClass.microChunkInt(it, PROJECTILEDEF_VARIABLE_ORIENTATIONMODE) } ?: ORIENTATION_ALIGNED

            // TumbleAxis is a Vector3 (3 x float32 = 12 bytes)
            val tumbleBytes = vars?.findMicroChunk(PROJECTILEDEF_VARIABLE_TUMBLEAXIS)
            val tumbleAxisX: Float
            val tumbleAxisY: Float
            val tumbleAxisZ: Float
            if (tumbleBytes != null && tumbleBytes.size >= 12) {
                val bb = ByteBuffer.wrap(tumbleBytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
                tumbleAxisX = bb.getFloat(0)
                tumbleAxisY = bb.getFloat(4)
                tumbleAxisZ = bb.getFloat(8)
            } else {
                tumbleAxisX = 1f
                tumbleAxisY = 2f
                tumbleAxisZ = 1f
            }

            val tumbleRate = vars?.let { PhysDefClass.microChunkFloat(it, PROJECTILEDEF_VARIABLE_TUMBLERATE) } ?: 0.17453292f
            val lifetime = vars?.let { PhysDefClass.microChunkFloat(it, PROJECTILEDEF_VARIABLE_LIFETIME) } ?: 2f
            val bounceCount = vars?.let { PhysDefClass.microChunkInt(it, PROJECTILEDEF_VARIABLE_BOUNCECOUNT) } ?: 0

            return ProjectileDefClass(
                name = name,
                id = id,
                chunkId = chunkId,
                modelName = parentFields.modelName,
                isPreLit = parentFields.isPreLit,
                mass = parentFields.mass,
                gravScale = parentFields.gravScale,
                elasticity = parentFields.elasticity,
                cinematicCollisionMode = parentFields.cinematicCollisionMode,
                collidesOnMove = collidesOnMove,
                orientationMode = orientationMode,
                tumbleAxisX = tumbleAxisX,
                tumbleAxisY = tumbleAxisY,
                tumbleAxisZ = tumbleAxisZ,
                tumbleRate = tumbleRate,
                lifetime = lifetime,
                bounceCount = bounceCount,
            )
        }
    }
}
