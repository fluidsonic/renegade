# Phase 8 Execution Plan: Explosion System

## Context

C4 and Beacon detonation already work for buildings, but C4 only damages its `stuckBuilding` (not
true AoE) and neither C4 nor Beacon applies damage falloff or hits soldiers/vehicles. This phase:
1. Gives `SoldierGameObj` and `VehicleGameObj` proper `applyDamage()` overrides that replicate health
2. Extracts the duplicated explosion logic from C4/Beacon into a shared `ExplosionHelper`
3. Refactors C4 and Beacon to use `ExplosionHelper` — giving C4 true AoE and both objects
   correct distance falloff + all-target damage

Master plan at `/plans/imperative-kindling-ripple.md`.

**Simplifications (matching existing codebase approach):**
- No ray occlusion (requires physics, Phase 11)
- Camera shake is purely client-side — no server changes needed
- `damageIsScaled` is already parsed in `ExplosionDefinitionClass`
- `DamageableGameObj.applyDamage()` already exists with shield + health logic

## Key Research Findings

### Existing Infrastructure
| Component | Status | File |
|-----------|--------|------|
| `ExplosionDefinitionClass` | Fully parsed (damageStrength, damageRadius, damageWarhead, damageIsScaled) | `server/defs/ExplosionDefinitionClass.kt` |
| `DamageableGameObj.applyDamage()` | Done (shield → health; calls `completelyDamaged()` at 0) | `server/net/DamageableGameObj.kt` |
| `BuildingGameObj.applyDamage()` | Override that marks `BIT_OCCASIONAL` dirty + calls `onDestroyed()` | `server/net/BuildingGameObj.kt` |
| `SoldierGameObj.applyDamage()` | NOT overridden — inherits base (no dirty bits set) | `server/net/SoldierGameObj.kt` |
| `VehicleGameObj.applyDamage()` | NOT overridden — inherits base (no dirty bits set) | `server/net/VehicleGameObj.kt` |
| `GameObjManager.getAllObjects()` | Returns all BaseGameObj (buildings + soldiers + vehicles + C4 + beacons) | `server/GameObjManager.kt` |
| `C4GameObj.detonate()` | Only damages `stuckBuilding`, no AoE, no falloff | `server/net/C4GameObj.kt` |
| `BeaconGameObj.detonate()` | Iterates buildings only, no falloff, no soldiers/vehicles | `server/net/BeaconGameObj.kt` |
| `ArmorWarheadManager.scaleDamage()` | Applies armor×warhead table | `server/combat/ArmorWarheadManager.kt` |
| `ScExplosionEvent` | classId=1014, defId+pos+ownerId | `server/net/ScExplosionEvent.kt` |

### C++ ExplosionManager::Create_Explosion_At()
- Iterates ALL `PhysicalGameObj` subclasses (soldiers, vehicles) + buildings
- Damage falloff: `scale = 1.0 - (distance / radius)` when `damageIsScaled = true`
- Ray occlusion reduces damage to 25% if blocked — out of scope (requires physics)

### Position Access Pattern
- `PhysicalGameObj` (soldiers, vehicles, C4, beacons): `obj.position: Vector3`
- `BuildingGameObj` (direct DamageableGameObj subclass): `obj.position: Vector3`
- In `ExplosionHelper`, use `when (obj) { is PhysicalGameObj -> obj.position; is BuildingGameObj -> obj.position; else -> null }` to get position

### Delete Order in detonate()
C4/Beacon must call `server.gameObjManager.remove(this)` and `setDeletePending()` BEFORE calling
`ExplosionHelper.applyExplosionDamage()` so the detonating object is not in the iteration list.

## Files to Modify / Create

| File | Changes |
|------|---------|
| `server/.../net/SoldierGameObj.kt` | Add `applyDamage()` override that marks `BIT_OCCASIONAL` dirty |
| `server/.../net/VehicleGameObj.kt` | Add `applyDamage()` override that marks `BIT_OCCASIONAL` dirty |
| `server/.../net/ExplosionHelper.kt` | **NEW** — shared AoE explosion logic |
| `server/.../net/C4GameObj.kt` | Refactor `detonate()` to use `ExplosionHelper` |
| `server/.../net/BeaconGameObj.kt` | Refactor `detonate()` to use `ExplosionHelper` |

## Tasks

### Task 1: applyDamage overrides for SoldierGameObj and VehicleGameObj

**Files:** `server/src/main/kotlin/ccr/server/net/SoldierGameObj.kt`,
`server/src/main/kotlin/ccr/server/net/VehicleGameObj.kt`

**Step 1: Write the failing test**

File: `server/src/test/kotlin/ccr/server/net/ExplosionDamageTest.kt`

```kotlin
package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExplosionDamageTest {

    companion object {
        @BeforeAll @JvmStatic fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_VEHICLE_VELOCITY, -90.0, 90.0, 0.01)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE, 0.0, 16.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_ANALOG, -1.0, 1.0, 0.01)
        }
    }

    @Test fun `SoldierGameObj applyDamage reduces health and sets BIT_OCCASIONAL dirty`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        soldier.applyDamage(30f)
        assertEquals(70f, soldier.health)
        assertTrue(soldier.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
    }

    @Test fun `VehicleGameObj applyDamage reduces health and sets BIT_OCCASIONAL dirty`() {
        val vehicle = VehicleGameObj(definitionId = 2, position = Vector3(0f, 0f, 0f), health = 200f, shieldStrength = 0f)
        vehicle.applyDamage(50f)
        assertEquals(150f, vehicle.health)
        assertTrue(vehicle.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
    }

    @Test fun `SoldierGameObj applyDamage zero damage does not dirty`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        soldier.applyDamage(0f)
        assertEquals(100f, soldier.health)
        // No damage — dirty bit should NOT be set
        assertEquals(false, soldier.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
    }
}
```

**Step 2: Run to verify it fails**

```bash
kotlin-server/gradlew -p kotlin-server :server:test --tests "ccr.server.net.ExplosionDamageTest"
```
Expected: FAIL — `SoldierGameObj applyDamage reduces health and sets BIT_OCCASIONAL dirty`
(health changes but dirty bit not set)

**Step 3: Add applyDamage override to SoldierGameObj**

In `SoldierGameObj.kt`, find the `override fun exportRare` (first override at ~line 47) and add before it:

```kotlin
override fun applyDamage(damage: Float) {
    super.applyDamage(damage)
    if (damage > 0f) setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
}
```

Add import `import ccr.net.replication.NetworkObject` if not already present.

**Step 4: Add applyDamage override to VehicleGameObj**

In `VehicleGameObj.kt`, find `override fun exportRare` (at ~line 138) and add before it:

```kotlin
override fun applyDamage(damage: Float) {
    super.applyDamage(damage)
    if (damage > 0f) setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
}
```

Add import `import ccr.net.replication.NetworkObject` if not already present.

**Step 5: Run test to verify it passes**

```bash
kotlin-server/gradlew -p kotlin-server :server:test --tests "ccr.server.net.ExplosionDamageTest"
```
Expected: 3/3 PASS

**Step 6: Run all tests to verify no regressions**

```bash
kotlin-server/gradlew -p kotlin-server test
```
Expected: all pass

**Step 7: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/net/SoldierGameObj.kt \
        kotlin-server/server/src/main/kotlin/ccr/server/net/VehicleGameObj.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/net/ExplosionDamageTest.kt
git commit -m "feat: mark BIT_OCCASIONAL dirty in SoldierGameObj/VehicleGameObj applyDamage"
```

---

### Task 2: Create ExplosionHelper

**File to create:** `server/src/main/kotlin/ccr/server/net/ExplosionHelper.kt`
**Test file:** `server/src/test/kotlin/ccr/server/net/ExplosionHelperTest.kt`

This object encapsulates all AoE explosion logic:
- Iterates all `DamageableGameObj` in `gameObjManager`
- Applies distance-based falloff (when `damageIsScaled`)
- Scales damage through `ArmorWarheadManager`
- Broadcasts `ScExplosionEvent`

**Step 1: Write the failing test**

```kotlin
package ccr.server.net

import ccr.math.Vector3
import ccr.server.GameObjManager
import ccr.server.combat.ArmorWarheadManager
import ccr.server.defs.DefinitionRegistry
import ccr.server.defs.ExplosionDefinitionClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExplosionHelperTest {

    private lateinit var gameObjManager: GameObjManager
    private lateinit var registry: DefinitionRegistry

    // Minimal fake server-like structure for testing:
    // ExplosionHelper needs server.gameObjManager, server.loadedLevel, server.god.playerInGame
    // We'll test via a TestableExplosionHelper that accepts components directly.
    // For simplicity, test the core logic via a standalone function that accepts the parameters.

    @BeforeEach fun setup() {
        gameObjManager = GameObjManager()
        registry = DefinitionRegistry()
    }

    private fun makeExplosionDef(
        radius: Float,
        strength: Float,
        warhead: Int = 0,
        scaled: Boolean = true,
    ): ExplosionDefinitionClass {
        return ExplosionDefinitionClass(
            name = "TestExplosion",
            id = 999u,
            chunkId = 0u,
            damageRadius = radius,
            damageStrength = strength,
            damageWarhead = warhead,
            damageIsScaled = scaled,
        )
    }

    @Test fun `object at explosion center takes full damage when scaled`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        gameObjManager.add(soldier)
        val explosionDef = makeExplosionDef(radius = 10f, strength = 50f, scaled = true)

        ExplosionHelper.applyDamageToObjects(
            explosionDef = explosionDef,
            posX = 0f, posY = 0f, posZ = 0f,
            objects = gameObjManager.getAllObjects(),
        )

        // At distance 0: scale = 1 - 0/10 = 1.0 → full 50 damage
        assertEquals(50f, soldier.health)
    }

    @Test fun `object at radius edge takes near-zero damage when scaled`() {
        // Place soldier exactly at radius
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(10f, 0f, 0f), health = 100f, shieldStrength = 0f)
        gameObjManager.add(soldier)
        val explosionDef = makeExplosionDef(radius = 10f, strength = 50f, scaled = true)

        ExplosionHelper.applyDamageToObjects(
            explosionDef = explosionDef,
            posX = 0f, posY = 0f, posZ = 0f,
            objects = gameObjManager.getAllObjects(),
        )

        // At distance 10, radius 10: scale = 1 - 10/10 = 0.0 → 0 damage
        assertEquals(100f, soldier.health)
    }

    @Test fun `object beyond radius takes no damage`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(20f, 0f, 0f), health = 100f, shieldStrength = 0f)
        gameObjManager.add(soldier)
        val explosionDef = makeExplosionDef(radius = 10f, strength = 50f, scaled = true)

        ExplosionHelper.applyDamageToObjects(
            explosionDef = explosionDef,
            posX = 0f, posY = 0f, posZ = 0f,
            objects = gameObjManager.getAllObjects(),
        )

        assertEquals(100f, soldier.health)
    }

    @Test fun `damageIsScaled false applies flat damage to all in radius`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(5f, 0f, 0f), health = 100f, shieldStrength = 0f)
        gameObjManager.add(soldier)
        val explosionDef = makeExplosionDef(radius = 10f, strength = 40f, scaled = false)

        ExplosionHelper.applyDamageToObjects(
            explosionDef = explosionDef,
            posX = 0f, posY = 0f, posZ = 0f,
            objects = gameObjManager.getAllObjects(),
        )

        // No scaling — full 40 damage regardless of distance within radius
        assertEquals(60f, soldier.health)
    }

    @Test fun `already dead object skipped`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 0f, shieldStrength = 0f)
        gameObjManager.add(soldier)
        val explosionDef = makeExplosionDef(radius = 10f, strength = 50f, scaled = false)

        // Should not throw
        ExplosionHelper.applyDamageToObjects(
            explosionDef = explosionDef,
            posX = 0f, posY = 0f, posZ = 0f,
            objects = gameObjManager.getAllObjects(),
        )

        assertEquals(0f, soldier.health)  // unchanged (already dead)
    }

    @Test fun `delete-pending object skipped`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        soldier.setDeletePending()
        gameObjManager.add(soldier)
        val explosionDef = makeExplosionDef(radius = 10f, strength = 50f, scaled = false)

        ExplosionHelper.applyDamageToObjects(
            explosionDef = explosionDef,
            posX = 0f, posY = 0f, posZ = 0f,
            objects = gameObjManager.getAllObjects(),
        )

        assertEquals(100f, soldier.health)  // unchanged (delete-pending)
    }

    @Test fun `multiple objects damaged proportionally by distance`() {
        val near   = SoldierGameObj(definitionId = 1, position = Vector3(2f, 0f, 0f), health = 100f, shieldStrength = 0f)
        val middle = SoldierGameObj(definitionId = 1, position = Vector3(5f, 0f, 0f), health = 100f, shieldStrength = 0f)
        gameObjManager.add(near)
        gameObjManager.add(middle)
        val explosionDef = makeExplosionDef(radius = 10f, strength = 100f, scaled = true)

        ExplosionHelper.applyDamageToObjects(
            explosionDef = explosionDef,
            posX = 0f, posY = 0f, posZ = 0f,
            objects = gameObjManager.getAllObjects(),
        )

        // near: scale = 1 - 2/10 = 0.8 → 80 damage → health = 20
        // middle: scale = 1 - 5/10 = 0.5 → 50 damage → health = 50
        assertEquals(20f, near.health,   absoluteTolerance = 0.1f)
        assertEquals(50f, middle.health, absoluteTolerance = 0.1f)
    }
}

private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
    assert(kotlin.math.abs(expected - actual) <= absoluteTolerance) {
        "Expected $expected ± $absoluteTolerance but got $actual"
    }
}
```

**Step 2: Run to verify it fails**

```bash
kotlin-server/gradlew -p kotlin-server :server:test --tests "ccr.server.net.ExplosionHelperTest"
```
Expected: FAIL — `ExplosionHelper` not found.

**Step 3: Create ExplosionHelper.kt**

```kotlin
package ccr.server.net

import ccr.math.Vector3
import ccr.net.replication.NetworkObjectManager
import ccr.server.GameServer
import ccr.server.combat.ArmorWarheadManager
import ccr.server.defs.ExplosionDefinitionClass
import kotlin.math.sqrt

/**
 * Shared explosion damage logic, ported from C++ ExplosionManager::Create_Explosion_At().
 *
 * Applies AoE damage with optional distance falloff to all damageable game objects
 * in range, then broadcasts ScExplosionEvent to all in-game clients.
 */
object ExplosionHelper {

    /**
     * Core damage loop — testable without a full GameServer.
     * Called by applyExplosionDamage after def lookup.
     */
    fun applyDamageToObjects(
        explosionDef: ExplosionDefinitionClass,
        posX: Float,
        posY: Float,
        posZ: Float,
        objects: List<*>,
    ) {
        val radiusSq = explosionDef.damageRadius * explosionDef.damageRadius
        if (radiusSq <= 0f) return

        for (obj in objects) {
            if (obj !is DamageableGameObj) continue
            if (obj.isDead || obj.isDeletePending) continue

            val objPos: Vector3 = when (obj) {
                is PhysicalGameObj -> obj.position
                is BuildingGameObj -> obj.position
                else -> continue
            }

            val dx = objPos.x - posX
            val dy = objPos.y - posY
            val dz = objPos.z - posZ
            val distSq = dx * dx + dy * dy + dz * dz

            if (distSq >= radiusSq) continue

            val dist = sqrt(distSq)
            val scale = if (explosionDef.damageIsScaled) {
                1f - (dist / explosionDef.damageRadius)
            } else {
                1f
            }

            val rawDamage = explosionDef.damageStrength * scale
            val armorType = obj.shieldType
            val finalDamage = ArmorWarheadManager.scaleDamage(rawDamage, explosionDef.damageWarhead, armorType)
            obj.applyDamage(finalDamage)
        }
    }

    /**
     * Full explosion: damage all objects in radius, then broadcast visual event.
     * Call after removing the exploding object from gameObjManager.
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
        )

        // Broadcast explosion visual + sound
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
```

**Step 4: Run test to verify it passes**

```bash
kotlin-server/gradlew -p kotlin-server :server:test --tests "ccr.server.net.ExplosionHelperTest"
```
Expected: 7/7 PASS

**Step 5: Run all tests**

```bash
kotlin-server/gradlew -p kotlin-server test
```
Expected: all pass

**Step 6: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/net/ExplosionHelper.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/net/ExplosionHelperTest.kt
git commit -m "feat: add ExplosionHelper with AoE damage and falloff"
```

---

### Task 3: Refactor C4GameObj and BeaconGameObj to use ExplosionHelper

**Files:** `server/src/main/kotlin/ccr/server/net/C4GameObj.kt`,
`server/src/main/kotlin/ccr/server/net/BeaconGameObj.kt`

**Step 1: Write the failing tests**

These are integration-style tests that verify the refactored `detonate()` calls `applyDamage` on
the right objects. Since we need `GameServer`, which is too heavy to instantiate in unit tests,
we verify via the public result (e.g., `isDeletePending` is set, objects are damaged).

Add to `ExplosionDamageTest.kt` (existing test file from Task 1):

```kotlin
@Test fun `C4GameObj detonate marks self as delete-pending`() {
    val c4 = C4GameObj(definitionId = 1, position = Vector3(0f, 0f, 0f))
    // detonate() with null serverRef should still call setDeletePending
    c4.detonate()
    assertTrue(c4.isDeletePending)
}

@Test fun `BeaconGameObj cancel marks self as delete-pending`() {
    val beacon = BeaconGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f))
    beacon.cancel()
    assertTrue(beacon.isDeletePending)
}
```

**Step 2: Run to verify they pass already** (sanity check — these should pass before the refactor)

```bash
kotlin-server/gradlew -p kotlin-server :server:test --tests "ccr.server.net.ExplosionDamageTest"
```

**Step 3: Refactor C4GameObj.detonate()**

Replace the current `detonate()` body in `C4GameObj.kt`:

```kotlin
fun detonate() {
    // Remove from manager and mark pending BEFORE AoE iteration
    // so this object is not damaged by its own explosion
    serverRef?.gameObjManager?.remove(this)
    setDeletePending()

    val server = serverRef ?: return
    val explosionDefId = ammoDefinition?.explosionDefId ?: 0
    if (explosionDefId == 0) return

    ExplosionHelper.applyExplosionDamage(
        explosionDefId = explosionDefId,
        posX           = stuckPosX,
        posY           = stuckPosY,
        posZ           = stuckPosZ,
        ownerId        = ownerId,
        server         = server,
    )
}
```

Add import `import ccr.server.net.ExplosionHelper` (already in the same package — no import needed).

Remove unused imports if any: `AmmoDefinitionClass.Companion.AMMO_TYPE_C4_REMOTE`,
`AmmoDefinitionClass.Companion.AMMO_TYPE_C4_TIMED` are still used in `think()`, keep them.
Remove unused import `ExplosionDefinitionClass` if it's no longer referenced directly in
`C4GameObj.kt`. Also remove import for `ArmorWarheadManager` if no longer directly used.

**Step 4: Refactor BeaconGameObj.detonate()**

Replace the `detonate()` body in `BeaconGameObj.kt`:

```kotlin
fun detonate() {
    val server = serverRef
    val def = beaconDef
    if (server == null || def == null) {
        cancel()
        return
    }

    // Remove from manager and mark pending BEFORE AoE iteration
    server.gameObjManager.remove(this)
    setDeletePending()

    val explosionDefId = def.explosionDefId
    if (explosionDefId == 0) return

    ExplosionHelper.applyExplosionDamage(
        explosionDefId = explosionDefId,
        posX           = position.x,
        posY           = position.y,
        posZ           = position.z,
        ownerId        = ownerId,
        server         = server,
    )
}
```

Remove unused imports from `BeaconGameObj.kt` if no longer directly referenced:
`ExplosionDefinitionClass`, `ArmorWarheadManager` — check after refactor.

**Step 5: Run tests**

```bash
kotlin-server/gradlew -p kotlin-server :server:test --tests "ccr.server.net.ExplosionDamageTest"
```
Expected: all PASS

**Step 6: Run all tests**

```bash
kotlin-server/gradlew -p kotlin-server test
```
Expected: all pass

**Step 7: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/net/C4GameObj.kt \
        kotlin-server/server/src/main/kotlin/ccr/server/net/BeaconGameObj.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/net/ExplosionDamageTest.kt
git commit -m "refactor: C4GameObj and BeaconGameObj use ExplosionHelper for AoE damage"
```

---

## Verification

```bash
kotlin-server/gradlew -p kotlin-server test
```

Expected: all tests pass. New tests cover:
- `SoldierGameObj`/`VehicleGameObj` dirty bit on damage
- `ExplosionHelper` falloff at center, edge, beyond radius
- `ExplosionHelper` flat damage (damageIsScaled=false)
- `ExplosionHelper` dead/delete-pending objects skipped
- `ExplosionHelper` multiple objects at different distances
- C4/Beacon delete-pending after `detonate()`/`cancel()`

## Manual Verification

1. Place timed C4 near NOD soldiers → after countdown, soldiers take damage (health bars change)
2. Place ion cannon beacon → after detonation, AoE hits buildings + nearby soldiers
3. C4 at center of group of buildings → all buildings in radius take damage (not just stuckBuilding)
4. Clients observe `ScExplosionEvent` effect + health bar updates for all damaged objects

## Later Phases

| Phase | Name | Key Deliverables |
|-------|------|-----------------|
| 9 | Map Rotation | End-of-round map switch, level reload |
| 10 | Bandwidth + Polish | Packet coalescing, rate limiting, connection quality |
| 11 | Physics Integration | Full collision, projectile flight, vehicle physics |
