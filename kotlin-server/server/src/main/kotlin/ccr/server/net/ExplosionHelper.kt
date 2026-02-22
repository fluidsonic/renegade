package ccr.server.net

import ccr.math.LineSeg
import ccr.math.Vector3
import ccr.net.replication.NetworkObjectManager
import ccr.physics.collision.RayCollisionTest
import ccr.physics.scene.PhysicsScene
import ccr.server.GameServer
import ccr.server.combat.ArmorWarheadManager
import ccr.server.defs.ExplosionDefinitionClass
import kotlin.math.sqrt

/**
 * Shared AoE explosion damage logic. Port of C++ ExplosionManager::Create_Explosion_At().
 *
 * Applies radius-based falloff damage to all DamageableGameObj in range,
 * then broadcasts ScExplosionEvent to all in-game clients.
 */
object ExplosionHelper {

    /**
     * Core damage loop — accepts Any list for testability (no GameServer needed in tests).
     * Objects that are not DamageableGameObj, are dead, or are delete-pending are skipped.
     */
    fun applyDamageToObjects(
        explosionDef: ExplosionDefinitionClass,
        posX: Float,
        posY: Float,
        posZ: Float,
        objects: List<*>,
        physicsScene: PhysicsScene? = null,
    ) {
        val radius = explosionDef.damageRadius
        if (radius <= 0f) return
        val radiusSq = radius * radius

        for (obj in objects) {
            if (obj !is DamageableGameObj) continue
            if (obj.isDead || obj.isDeletePending) continue

            val objPos = when (obj) {
                is PhysicalGameObj -> obj.position
                is BuildingGameObj -> obj.position
                else -> continue
            }

            val dx = objPos.x - posX
            val dy = objPos.y - posY
            val dz = objPos.z - posZ
            val distSq = dx * dx + dy * dy + dz * dz

            // Objects at or beyond the radius take no damage
            if (distSq >= radiusSq) continue

            // Line-of-sight check: blocked targets take 25% damage
            // C++: ExplosionManager::Create_Explosion_At casts a ray per target
            var scale = if (explosionDef.damageIsScaled) 1f - (sqrt(distSq) / radius) else 1f
            if (physicsScene != null && distSq > 0.0001f) {
                val ray = RayCollisionTest(
                    ray = LineSeg(
                        p0 = Vector3(posX, posY, posZ),
                        p1 = Vector3(objPos.x, objPos.y, objPos.z),
                    ),
                    checkStatic  = true,
                    checkDynamic = false,   // only terrain walls occlude; other entities don't
                )
                if (physicsScene.castRay(ray)) scale *= 0.25f   // C++: blocked targets take 25% damage
            }

            val rawDamage = explosionDef.damageStrength * scale
            val finalDamage = ArmorWarheadManager.scaleDamage(
                rawDamage, explosionDef.damageWarhead, obj.shieldType)
            obj.applyDamage(finalDamage)
        }
    }

    /**
     * Full explosion: damage objects, broadcast visual/sound event.
     * The exploding object itself must be removed from gameObjManager BEFORE calling this,
     * so it does not damage itself.
     */
    fun applyExplosionDamage(
        explosionDefId: Int,
        posX: Float,
        posY: Float,
        posZ: Float,
        ownerId: Int,
        server: GameServer,
    ) {
        val explosionDef = server.loadedLevel?.definitions?.findById(explosionDefId.toUInt())
            as? ExplosionDefinitionClass ?: return

        applyDamageToObjects(
            explosionDef = explosionDef,
            posX = posX,
            posY = posY,
            posZ = posZ,
            objects = server.gameObjManager.getAllObjects(),
            physicsScene = server.physicsScene,
        )

        val explosion = ScExplosionEvent(
            defId   = explosionDefId,
            posX    = posX,
            posY    = posY,
            posZ    = posZ,
            ownerId = ownerId,
        )
        for (clientId in server.god.playerInGame) {
            val host = server.connectionManager.getHost(clientId) ?: continue
            server.sendGameNetObj(host) { bs ->
                NetworkObjectPacketWriter.writeCreation(bs, explosion, NetworkObjectManager.getNewDynamicId())
            }
        }
    }
}
