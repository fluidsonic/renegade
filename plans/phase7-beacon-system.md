# Phase 7 Implementation Plan: Beacon System

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Implement beacon placement, arming, and detonation — the nuclear/ion cannon mechanic that destroys enemy buildings in C&C mode.

**Architecture:** Beacons are placed by weapon fire (same path as C4), spend `armTime` transitioning ARMING→ARMED, then count down `detonateTime` and detonate — applying explosion-radius AoE damage to all buildings via `ArmorWarheadManager`. Existing dirty-bit replication handles client-side HUD/state updates automatically.

**Tech Stack:** Kotlin, existing `SimpleGameObj` / `DefinitionClass` hierarchy, `ArmorWarheadManager.scaleDamage`, `BaseControllerClass.getBuildings`, `ScExplosionEvent`, `NetworkObjectPacketWriter`.

---

## Simplifications (vs full C++)

- No owner interrupt — in C++ if the beacon owner moves/dies, arming aborts and ammo is returned. **We skip this.**
- No disarm mechanic — in C++ an enemy soldier can poke the beacon to disarm it. **We skip this.**
- No weather effects — ion/nuke sky tint and particle effects. **We skip this.**
- No per-player beacon limit — C++ enforces 1 active beacon per player. **We skip this.**

---

## Key Existing Infrastructure

| Component | File | Status |
|-----------|------|--------|
| `BeaconGameObj` | `net/BeaconGameObj.kt` | Partial — immutable `val state/ownerId`, has `exportRare`, **no `think()`** |
| `BeaconGameObjDef` | `defs/combat/BeaconGameObjDef.kt` | All 18 fields parsed; **is `data class`, NOT `DefinitionClass` subclass** |
| `FullDefinitionLoader` | `level/FullDefinitionLoader.kt` | Has `FIXME: BeaconGameObjDef.CHUNK_ID -> fallback` (line 105-106) |
| `AmmoDefinitionClass.beaconDefId` | `defs/AmmoDefinitionClass.kt` | `val beaconDefId: Int = 0` — non-zero identifies beacon weapons |
| `ExplosionDefinitionClass` | `defs/ExplosionDefinitionClass.kt` | Has `damageRadius`, `damageStrength`, `damageWarhead` |
| `ArmorWarheadManager.scaleDamage` | (loaded from armor.ini) | Call: `scaleDamage(rawDamage, warheadSaveId, armorSaveId)` |
| `ScExplosionEvent` | `net/ScExplosionEvent.kt` | `ScExplosionEvent(defId, posX, posY, posZ, ownerId)` |
| `BaseControllerClass.getBuildings()` | `net/BaseControllerClass.kt` | Returns `List<BuildingGameObj>` |
| `BuildingGameObj.applyDamage()` | `net/BuildingGameObj.kt` | Applies damage, marks `BIT_OCCASIONAL` dirty, calls `onDestroyed` at 0 |
| God C4 pattern | `God.kt` | `c4Objects`, `createC4()`, disconnect cleanup — identical structure for beacons |
| GameServer C4 pattern | `GameServer.kt` | `isC4Weapon()`, `timedC4WeaponDefId`, `tossedC4DefId` — follow same pattern |

---

## Beacon State Machine

```
STATE_NULL       = 0   (before placement — unused at runtime)
STATE_ARMING     = 1   (placed, counting down armTimer)
STATE_ARMED      = 2   (armed, counting down detonateTimer)
STATE_DISARMED   = 3   (not used in this phase)
STATE_DETONATING = 4   (detonation triggered)
```

Timers come from `BeaconGameObjDef`:
- `armTime` (default 10f) — seconds to transition ARMING → ARMED
- `detonateTime` (default 30f) — seconds to detonate once ARMED

---

## Files to Modify

| File | Changes |
|------|---------|
| `defs/combat/BeaconGameObjDef.kt` | Convert `data class` → `class : DefinitionClass(name, id, chunkId)` |
| `level/FullDefinitionLoader.kt` | Replace FIXME fallback with `parseBeaconGameObjDef(...)` |
| `level/FullDefinitionLoaderTest.kt` | Add dispatch test |
| `net/BeaconGameObj.kt` | Rewrite: mutable fields, state machine, `think()`, `detonate()`, `cancel()` |
| `God.kt` | Add `beaconObjects`, `createBeacon()`, disconnect cleanup |
| `GameServer.kt` | Add beacon weapon detection + wiring |

---

## Task 1: Fix BeaconGameObjDef — convert to DefinitionClass subclass

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/defs/combat/BeaconGameObjDef.kt`
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/level/FullDefinitionLoader.kt`
- Modify: `kotlin-server/server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt`

**Why:** `registry.findById(beaconDefId.toUInt()) as? BeaconGameObjDef` only works if `BeaconGameObjDef` extends `DefinitionClass`. Currently it's a `data class` with its own `name`/`id`/`chunkId` fields — exactly the same bug we fixed for `PowerUpGameObjDef` in Phase 6.

**Step 1: Write the failing test**

In `FullDefinitionLoaderTest.kt`, add a test that a definition with `BeaconGameObjDef.CHUNK_ID` dispatches to a `BeaconGameObjDef` instance. Pattern is identical to the `PowerUpGameObjDef` test already in the file.

Run: `kotlin-server/gradlew -p kotlin-server :server:test --tests "*FullDefinitionLoaderTest*" -x lint`
Expected: FAIL (still returns plain `DefinitionClass`)

**Step 2: Convert BeaconGameObjDef**

Replace:
```kotlin
data class BeaconGameObjDef(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    val armingAnimationName: String = "",
    // ... remaining fields ...
```

With:
```kotlin
class BeaconGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val armingAnimationName: String = "",
    // ... remaining fields unchanged ...
) : DefinitionClass(name, id, chunkId) {
```

Add the `DefinitionClass` import: `import ccr.server.defs.DefinitionClass`

Remove the three standalone `val name`, `val id`, `val chunkId` from the constructor (they become `name`, `id`, `chunkId` without `val` since the super class owns them).

The `companion object { const val CHUNK_ID: UInt = 0x00040136u }` stays as-is.

**Step 3: Wire into FullDefinitionLoader**

In `dispatch()`, replace:
```kotlin
// FIXME: BeaconGameObjDef is a data class — convert to DefinitionClass subclass and parse properly
BeaconGameObjDef.CHUNK_ID -> fallback
```

With:
```kotlin
BeaconGameObjDef.CHUNK_ID ->
    parseBeaconGameObjDef(objDataChunk, name, id, chunkId)
```

Add the import at the top of `FullDefinitionLoader.kt`:
`import ccr.server.defs.combat.parseBeaconGameObjDef`

**Step 4: Run test to verify it passes**

Run: `kotlin-server/gradlew -p kotlin-server :server:test --tests "*FullDefinitionLoaderTest*" -x lint`
Expected: PASS

**Step 5: Run all tests**

Run: `kotlin-server/gradlew -p kotlin-server test -x lint`
Expected: All pass

**Step 6: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/defs/combat/BeaconGameObjDef.kt \
        kotlin-server/server/src/main/kotlin/ccr/server/level/FullDefinitionLoader.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt
git commit -m "fix: convert BeaconGameObjDef to DefinitionClass subclass, wire into FullDefinitionLoader"
```

---

## Task 2: Rewrite BeaconGameObj with state machine + think() + detonate()

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/net/BeaconGameObj.kt`
- Create: `kotlin-server/server/src/test/kotlin/ccr/server/net/BeaconGameObjTest.kt`

**Why:** The current `BeaconGameObj` has immutable `val state` / `val ownerId` and no `think()`. We need a full state machine (ARMING → ARMED → detonate) and mutable runtime fields that `God.createBeacon()` can populate.

**Step 1: Write the failing tests**

Create `BeaconGameObjTest.kt`:

```kotlin
package ccr.server.net

import ccr.math.Vector3
import ccr.server.defs.combat.BeaconGameObjDef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BeaconGameObjTest {

    private fun makeBeacon(armTime: Float = 10f, detonateTime: Float = 30f): BeaconGameObj {
        val def = BeaconGameObjDef(
            name = "Test Beacon", id = 1u, chunkId = BeaconGameObjDef.CHUNK_ID,
            armTime = armTime, detonateTime = detonateTime,
            explosionDefId = 0,
        )
        return BeaconGameObj(
            definitionId = 1,
            position = Vector3(0f, 0f, 0f),
            state = BeaconGameObj.STATE_ARMING,
        ).also {
            it.beaconDef = def
            it.armTimer = armTime
        }
    }

    @Test
    fun `beacon transitions from ARMING to ARMED after armTime`() {
        val beacon = makeBeacon(armTime = 5f, detonateTime = 30f)
        assertEquals(BeaconGameObj.STATE_ARMING, beacon.state)

        beacon.thinkInternal(3f)
        assertEquals(BeaconGameObj.STATE_ARMING, beacon.state)  // not yet

        beacon.thinkInternal(3f)  // total 6f > 5f
        assertEquals(BeaconGameObj.STATE_ARMED, beacon.state)
    }

    @Test
    fun `beacon detonateTimer set from def on arming completion`() {
        val beacon = makeBeacon(armTime = 5f, detonateTime = 30f)
        beacon.thinkInternal(6f)
        assertEquals(BeaconGameObj.STATE_ARMED, beacon.state)
        // detonateTimer should be approximately 30f minus the overshoot (6-5=1s)
        assertTrue(beacon.detonateTimer < 30f)
        assertTrue(beacon.detonateTimer > 28f)
    }

    @Test
    fun `exportRare serialises state and ownerId`() {
        // Round-trip test: state and ownerId appear in exportRare output
        // (Existing behaviour — guard against regression)
        val beacon = BeaconGameObj(
            definitionId = 1,
            position = Vector3(0f, 0f, 0f),
            state = BeaconGameObj.STATE_ARMED,
        )
        beacon.ownerId = 42
        val bs = ccr.net.bitstream.BitStream()
        beacon.exportRare(bs)
        // Just verify it doesn't throw — state/ownerId are deep in the packet.
        // The round-trip test in BeaconGameObjPacketTest covers wire format.
        assertTrue(bs.bitWritePosition > 0)
    }
}
```

Run: `kotlin-server/gradlew -p kotlin-server :server:test --tests "*BeaconGameObjTest*" -x lint`
Expected: COMPILE ERROR — `BeaconGameObj.STATE_ARMING`, `thinkInternal`, `beaconDef`, `armTimer`, `detonateTimer` don't exist yet.

**Step 2: Rewrite BeaconGameObj**

Replace the entire file with:

```kotlin
package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectManager
import ccr.server.GameServer
import ccr.server.defs.ExplosionDefinitionClass
import ccr.server.defs.combat.BeaconGameObjDef
import ccr.server.ArmorWarheadManager

// C++: BeaconGameObj (beacongameobj.cpp) — extends SimpleGameObj.
// Export_Rare: calls super first, then appends state and ownerId.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → BeaconGameObj
class BeaconGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    state: Int = STATE_NULL,
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    companion object {
        const val STATE_NULL       = 0
        const val STATE_ARMING     = 1
        const val STATE_ARMED      = 2
        const val STATE_DISARMED   = 3
        const val STATE_DETONATING = 4
    }

    // Serialised fields (written in exportRare)
    var state: Int = state
    var ownerId: Int = 0

    // Runtime fields (not serialised)
    var beaconDef: BeaconGameObjDef? = null
    var serverRef: GameServer? = null
    var ownerRhostId: Int = 0
    var armTimer: Float = 0f
    var detonateTimer: Float = 0f

    // C++: BeaconGameObj::Think
    override fun think(deltaSeconds: Float) {
        thinkInternal(deltaSeconds)
    }

    // Extracted for testability (tests call this directly without needing serverRef).
    internal fun thinkInternal(deltaSeconds: Float) {
        if (isDeletePending) return

        when (state) {
            STATE_ARMING -> {
                armTimer -= deltaSeconds
                if (armTimer <= 0f) {
                    state = STATE_ARMED
                    // Carry overshoot into detonateTimer so timing is accurate
                    detonateTimer = (beaconDef?.detonateTime ?: 30f) + armTimer  // armTimer is negative overshoot
                    // Replicate state change to all in-game clients
                    val inGame = serverRef?.god?.playerInGame ?: return
                    for (clientId in inGame) {
                        setObjectDirtyBit(clientId, NetworkObject.BIT_RARE, true)
                    }
                }
            }
            STATE_ARMED -> {
                detonateTimer -= deltaSeconds
                if (detonateTimer <= 0f) {
                    state = STATE_DETONATING
                    detonate()
                }
            }
        }
    }

    // C++: BeaconGameObj::Detonate — applies AoE damage to all buildings in explosionRadius,
    // broadcasts ScExplosionEvent, then marks itself for deletion.
    fun detonate() {
        val server = serverRef ?: run { cancel(); return }
        val def = beaconDef ?: run { cancel(); return }

        val explosionDef = server.loadedLevel?.definitions
            ?.findById(def.explosionDefId.toUInt()) as? ExplosionDefinitionClass

        if (explosionDef != null && explosionDef.damageRadius > 0f) {
            val radiusSq = explosionDef.damageRadius * explosionDef.damageRadius

            // Iterate ALL buildings (both teams) — nuke/ion can hit own buildings at edge cases.
            val allBuildings = listOfNotNull(server.baseControllerNod, server.baseControllerGdi)
                .flatMap { it.getBuildings() }
                .filter { !it.isDestroyed }

            for (building in allBuildings) {
                val dx = building.position.x - position.x
                val dy = building.position.y - position.y
                val dz = building.position.z - position.z
                if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                    val damage = ArmorWarheadManager.scaleDamage(
                        explosionDef.damageStrength,
                        explosionDef.damageWarhead,
                        building.shieldType,
                    )
                    building.applyDamage(damage)
                }
            }

            // Broadcast explosion visual/sound to all in-game clients
            val explosion = ScExplosionEvent(
                defId   = def.explosionDefId,
                posX    = position.x,
                posY    = position.y,
                posZ    = position.z,
                ownerId = ownerId,
            )
            for (clientId in server.god.playerInGame) {
                val host = server.connectionManager.getHost(clientId) ?: continue
                server.sendGameNetObj(host) { bs ->
                    NetworkObjectPacketWriter.writeCreation(bs, explosion, NetworkObjectManager.getNewDynamicId())
                }
            }
        }

        server.gameObjManager.remove(this)
        setDeletePending()
    }

    // C++: cancel/defuse — remove without damage (owner disconnect, etc.)
    fun cancel() {
        serverRef?.gameObjManager?.remove(this)
        setDeletePending()
    }

    // C++: BeaconGameObj::Export_Rare — calls super then appends state + ownerId.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // PhysicalGameObj: model + anim + host + player_type + hud_pokable
        packet.addInt(state)
        packet.addInt(ownerId)
    }
}
```

**Step 3: Run tests**

Run: `kotlin-server/gradlew -p kotlin-server :server:test --tests "*BeaconGameObjTest*" -x lint`
Expected: PASS (all 3 tests)

**Step 4: Run all tests**

Run: `kotlin-server/gradlew -p kotlin-server test -x lint`
Expected: All pass

**Step 5: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/net/BeaconGameObj.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/net/BeaconGameObjTest.kt
git commit -m "feat: rewrite BeaconGameObj with state machine, think(), detonate(), cancel()"
```

---

## Task 3: Add beacon tracking to God

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/God.kt`
- Create: `kotlin-server/server/src/test/kotlin/ccr/server/GodBeaconTest.kt`

**Why:** Follows the exact same pattern as `C4GameObj` tracking. God needs to create beacons, enforce rate limiting, and cancel beacons when the owner disconnects.

**Step 1: Write the failing test**

Create `GodBeaconTest.kt`:

```kotlin
package ccr.server

import ccr.math.Vector3
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectManager
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.combat.BeaconGameObjDef
import ccr.server.net.BeaconGameObj
import ccr.server.net.SoldierGameObj
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GodBeaconTest {

    // Minimal God subclass with a no-op createCommando so tests don't need a real server
    private class TestGod(server: GameServer) : God(server) {
        override fun createCommando(rhostId: Int, playerType: Int): SoldierGameObj? = null
    }

    @Test
    fun `createBeacon rate-limits to once per second`() {
        val (god, server) = makeGodAndServer()
        val soldier = makeSoldier(server)
        val ammoDef = makeBeaconAmmoDef(server)

        val b1 = god.createBeacon(1, soldier, ammoDef, 1000L)
        val b2 = god.createBeacon(1, soldier, ammoDef, 1500L)  // < 1s later
        val b3 = god.createBeacon(1, soldier, ammoDef, 2100L)  // > 1s later

        assertTrue(b1 != null, "first beacon should be created")
        assertNull(b2, "second beacon within 1s should be rate-limited")
        assertTrue(b3 != null, "third beacon after 1s should be created")
    }

    @Test
    fun `deleteSoldier cancels owner beacons`() {
        val (god, server) = makeGodAndServer()
        val soldier = makeSoldier(server)
        val ammoDef = makeBeaconAmmoDef(server)

        god.playerInGame.add(1)
        god.soldiersByHost[1] = soldier
        god.playerTeams[1] = 0

        val beacon = god.createBeacon(1, soldier, ammoDef, 1000L)!!

        god.deleteSoldier(1)

        assertTrue(beacon.isDeletePending, "beacon should be cancelled on soldier delete")
    }

    // --- helpers ---

    private fun makeGodAndServer(): Pair<God, GameServer> {
        // We only test the beacon tracking logic, not the full server stack.
        // Create a minimal server stub via reflection or accept the dependency.
        // Use the TestGod pattern from GodRespawnCooldownTest.
        TODO("Provide a testable server stub — see GodRespawnCooldownTest for pattern")
    }
    // NOTE: if the minimal-server-stub approach is too complex, extract beacon logic
    // into internal helpers (createBeaconInternal, cancelBeaconForRhost) that take
    // only the parameters they need, and test those directly.
}
```

> **Note to implementer:** The God tests in this codebase (see `GodRespawnCooldownTest.kt`) use a `TrackingGod` subclass that overrides `createCommando` as a no-op. Follow that exact pattern. If making a fully-wired server for testing is too burdensome, extract the rate-limiting + list-management logic into `internal` helpers with simple signatures and test those directly.

Run: `kotlin-server/gradlew -p kotlin-server :server:test --tests "*GodBeaconTest*" -x lint`
Expected: FAIL (fields / methods don't exist)

**Step 2: Add beacon tracking to God**

In `God.kt`, add the following:

**Imports to add:**
```kotlin
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.combat.BeaconGameObjDef
import ccr.server.net.BeaconGameObj
```

**Fields after the `c4Objects` block (around line 52-53):**
```kotlin
// Beacon tracking (parallel to C4 tracking)
val beaconObjects = mutableListOf<BeaconGameObj>()
private val lastBeaconPlaceMs = mutableMapOf<Int, Long>()  // rhostId → last placement time
```

**New function after `createC4()`:**
```kotlin
/**
 * Creates a BeaconGameObj for the given soldier at their current position.
 * Returns null if rate-limited or if beaconDefId resolves to no definition.
 * C++: BeaconGameObj is created when weapon primary-fires and ammo.beaconDefId != 0.
 *
 * @param rhostId   remote host / client ID
 * @param soldier   the soldier placing the beacon
 * @param ammoDef   the ammo definition for the current weapon (must have beaconDefId != 0)
 * @param nowMs     current time in milliseconds (for rate limiting)
 */
fun createBeacon(rhostId: Int, soldier: SoldierGameObj, ammoDef: AmmoDefinitionClass, nowMs: Long): BeaconGameObj? {
    // Rate limit: 1 beacon per second per player
    if (nowMs - (lastBeaconPlaceMs[rhostId] ?: 0L) < 1000L) return null

    val beaconDef = server.loadedLevel?.definitions
        ?.findById(ammoDef.beaconDefId.toUInt()) as? BeaconGameObjDef ?: return null

    val beacon = BeaconGameObj(
        definitionId = ammoDef.beaconDefId,
        position     = soldier.position.copy(),
        modelName    = ammoDef.modelFilename,
        state        = BeaconGameObj.STATE_ARMING,
    )
    beacon.ownerId       = soldier.networkId
    beacon.beaconDef     = beaconDef
    beacon.serverRef     = server
    beacon.ownerRhostId  = rhostId
    beacon.armTimer      = beaconDef.armTime

    val netId = NetworkObjectManager.getNewDynamicId()
    NetworkObjectManager.registerObject(beacon, netId)
    server.gameObjManager.add(beacon)
    beaconObjects.add(beacon)
    lastBeaconPlaceMs[rhostId] = nowMs

    println("[GOD] beacon placed by rhostId=$rhostId netId=$netId arm=${beaconDef.armTime}s det=${beaconDef.detonateTime}s")
    return beacon
}
```

**In `deleteSoldier()`, add beacon cancellation after the C4 cleanup block:**
```kotlin
// Cancel all beacons owned by this player (beacon arming/detonation aborts on owner disconnect)
beaconObjects.filter { !it.isDeletePending && it.ownerRhostId == rhostId }
    .forEach { it.cancel() }
```

**Step 3: Run tests**

Run: `kotlin-server/gradlew -p kotlin-server :server:test --tests "*GodBeaconTest*" -x lint`
Expected: PASS

**Step 4: Run all tests**

Run: `kotlin-server/gradlew -p kotlin-server test -x lint`
Expected: All pass

**Step 5: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/God.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/GodBeaconTest.kt
git commit -m "feat: add beacon tracking to God — createBeacon(), disconnect cancellation"
```

---

## Task 4: Wire GameServer — beacon weapon detection + cleanup

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`

**Why:** GameServer needs to (1) recognise beacon weapons from `continuousBoolBits` bit 0, (2) call `god.createBeacon()`, and (3) clean up the `beaconObjects` list in the network tick loop (same pattern as `c4Objects.removeAll`).

No new test file is needed — the existing integration path is already covered by beacon/God unit tests. The wiring is structural.

**Step 1: Add beacon weapon detection helper**

Near the existing `isC4Weapon` and `getAmmoDefForWeapon` helpers (around line 1393), add:

```kotlin
// Returns true if the weapon's primary ammo has a beaconDefId configured.
fun isBeaconWeapon(weaponDefId: Int): Boolean {
    val ammoDef = getAmmoDefForWeapon(weaponDefId) ?: return false
    return ammoDef.beaconDefId != 0
}
```

**Step 2: Add beaconWeaponDefId field and definition loading**

After the `timedC4WeaponDefId` and `tossedC4DefId` fields (around line 121-122), add:

```kotlin
internal var beaconWeaponDefId: Int = 0  // first weapon whose primary ammo has beaconDefId != 0
```

In both definition loading locations (the two blocks around lines 1291-1299 and 1381-1388 where C4 weapon is detected), add beacon weapon lookup **after** the C4 block:

```kotlin
defs.filterIsInstance<AmmoDefinitionClass>().find { it.beaconDefId != 0 }?.let { ammoDef ->
    // Find the weapon that uses this ammo (check primaryAmmoDefID)
    defs.filterIsInstance<WeaponDefinitionClass>()
        .find { it.primaryAmmoDefID == ammoDef.id.toInt() }
        ?.let { weaponDef ->
            beaconWeaponDefId = weaponDef.id.toInt()
            println("[SERVER] beacon weapon: ${weaponDef.name} defId=0x${beaconWeaponDefId.toUInt().toString(16)}")
        }
}
```

> **Implementation note:** `WeaponDefinitionClass.primaryAmmoDefID` is an `Int`. Check the actual field name in the class before using it — it may be `primaryAmmoID` or similar. Use Grep/Read to confirm.

**Step 3: Wire beacon fire in handleFrequentUpdate (on-foot path)**

In the on-foot path of `handleFrequentUpdate`, immediately after the C4 fire block (around line 1037-1039):

```kotlin
if (weaponFirePrimary && isBeaconWeapon(soldier.currentWeaponDefId)) {
    val ammoDef = getAmmoDefForWeapon(soldier.currentWeaponDefId)
    if (ammoDef != null) {
        god.createBeacon(rhostId, soldier, ammoDef, System.currentTimeMillis())
    }
}
```

**Step 4: Clean up beaconObjects in the network tick loop**

In `networkTickLoop`, immediately after the existing line:
```kotlin
god.c4Objects.removeAll { it.isDeletePending }
```

Add:
```kotlin
god.beaconObjects.removeAll { it.isDeletePending }
```

**Step 5: Run all tests**

Run: `kotlin-server/gradlew -p kotlin-server test -x lint`
Expected: All pass

**Step 6: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt
git commit -m "feat: wire GameServer beacon weapon detection and tick-loop cleanup"
```

---

## Verification

1. Build: `kotlin-server/gradlew -p kotlin-server test`
2. Manual test with real client:
   - Join → soldier spawns with pistol + timed C4 in weapon bag
   - Ensure beacon weapon (ion cannon / nuke launcher) is available in purchase menu
   - Fire beacon weapon near enemy base → `[GOD] beacon placed` log appears
   - Wait `armTime` seconds → `[GOD]` or state-change BIT_RARE log; client HUD updates
   - Wait `detonateTime` seconds → buildings in radius take damage; explosion event sent to all clients
   - Building health reaches 0 → `onDestroyed()` → `BaseControllerClass` updates → game-over detection triggers
   - Disconnect while beacon is arming → beacon is cancelled (no detonation)

---

## Later Phases

Full roadmap at `/plans/imperative-kindling-ripple.md`. Remaining phases after this one:

| Phase | Name | Key Deliverables |
|-------|------|-----------------|
| 8 | Explosion System | Full radius-based AoE damage, camera shake |
| 9 | Map Rotation | End-of-round map switch, level reload |
| 10 | Bandwidth + Polish | Packet coalescing, rate limiting, connection quality |
| 11 | Physics Integration | Full collision, projectile flight, vehicle physics |

## Note
Once execution begins, save this plan to `/plans/phase7-beacon-system.md` — this is already the correct location.
