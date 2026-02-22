# Phase 12 Implementation Plan: Explosion Ray Occlusion

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Explosions can no longer deal damage through solid terrain — `physicsScene.castRay()` checks line-of-sight from explosion center to each target before applying damage.

**Architecture:** `ExplosionHelper.applyDamageToObjects()` gains an optional `physicsScene: PhysicsScene? = null` parameter. Before each `applyDamage()` call, if a scene is provided, a ray is cast from the explosion center to the target's position; if the ray hits geometry the target is skipped. `applyExplosionDamage()` — the only caller that has `server` access — passes `server.physicsScene`. When no level is loaded `physicsScene` is null and the existing flat-distance behaviour is preserved unchanged.

**Tech Stack:** Kotlin, `ccr.physics.scene.PhysicsScene`, `ccr.physics.collision.RayCollisionTest`, `ccr.math.LineSeg`, `ccr.server.net.ExplosionHelper`, kotlin.test

---

## Context

### What's already done
- `PhysicsScene.update(dt)` is ticked at 30 Hz (Phase 11)
- `PhysicsSceneBuilder.build()` populates the scene with static collision triangles during `loadLevel()`
- `GameServer.physicsScene: PhysicsScene?` holds the live scene
- `PhysicsScene.castRay(test: RayCollisionTest): Boolean` — returns `true` if the ray hits geometry (i.e. is **blocked**)
- `RayCollisionTest(ray: LineSeg, checkStatic, checkDynamic)` — standard collision test
- `LineSeg(p0: Vector3, p1: Vector3)` — line segment in `ccr.math`
- `ExplosionHelper.applyDamageToObjects()` already iterates objects and applies falloff damage; no ray check yet

### What's missing
- `applyDamageToObjects()` has no line-of-sight check — explosions pass through walls and solid buildings
- This was explicitly deferred from Phase 8: *"No ray occlusion (requires physics, Phase 11)"*

## Key Files

| File | Purpose |
|------|---------|
| `kotlin-server/server/src/main/kotlin/ccr/server/net/ExplosionHelper.kt` | `applyDamageToObjects()` and `applyExplosionDamage()` |
| `kotlin-server/server/src/test/kotlin/ccr/server/net/ExplosionHelperTest.kt` | Existing test file (or create it) — add ray occlusion tests here |
| `kotlin-server/physics/src/main/kotlin/ccr/physics/scene/PhysicsScene.kt` | `castRay(test)` — returns true if blocked |
| `kotlin-server/physics/src/main/kotlin/ccr/physics/collision/RayCollisionTest.kt` | Test type: `ray: LineSeg, checkStatic: Boolean, checkDynamic: Boolean` |
| `kotlin-server/math/src/main/kotlin/ccr/math/LineSeg.kt` | `LineSeg(p0: Vector3, p1: Vector3)` |

---

## Task 1: Add ray occlusion to ExplosionHelper

**Files to modify:**
- `kotlin-server/server/src/main/kotlin/ccr/server/net/ExplosionHelper.kt`
- `kotlin-server/server/src/test/kotlin/ccr/server/net/ExplosionHelperTest.kt` (create if absent)

### Step 1.1: Read the existing files

Read both files:
```
kotlin-server/server/src/main/kotlin/ccr/server/net/ExplosionHelper.kt
kotlin-server/physics/src/main/kotlin/ccr/physics/scene/PhysicsScene.kt
kotlin-server/physics/src/main/kotlin/ccr/physics/collision/RayCollisionTest.kt
kotlin-server/math/src/main/kotlin/ccr/math/LineSeg.kt
```

Also look at the existing PhysicsSceneTest to understand how to build a test scene with static geometry:
```
kotlin-server/physics/src/test/kotlin/ccr/physics/scene/PhysicsSceneTest.kt
```

### Step 1.2: Write failing tests

In `ExplosionHelperTest.kt`, add three tests for the ray occlusion behaviour.

**Test 1 — null scene: same behaviour as before (no regression)**
```kotlin
@Test
fun `null physicsScene applies damage as before`() {
    // One target 5m away, radius 10m
    val def = explosionDef(damageStrength = 100f, damageRadius = 10f, damageIsScaled = false)
    val target = FakeDamageable(position = Vector3(5f, 0f, 0f))
    applyDamageToObjects(def, 0f, 0f, 0f, listOf(target), physicsScene = null)
    assertEquals(100f, target.damageTaken, 0.01f)
}
```

**Test 2 — empty scene (no geometry): ray misses, damage applied**
```kotlin
@Test
fun `empty physicsScene (no walls) still applies damage`() {
    val scene = PhysicsScene()
    val def = explosionDef(damageStrength = 100f, damageRadius = 10f, damageIsScaled = false)
    val target = FakeDamageable(position = Vector3(5f, 0f, 0f))
    applyDamageToObjects(def, 0f, 0f, 0f, listOf(target), physicsScene = scene)
    assertEquals(100f, target.damageTaken, 0.01f)
}
```

**Test 3 — wall between explosion and target: ray blocked, no damage**

For this test you need a PhysicsScene with a triangle wall between (0,0,0) and (10,0,0).
Look at `PhysicsSceneTest.kt` to see how to create a `StaticPhysClass` with a triangle list and call `scene.addStaticObject()` (or the builder equivalent). Place a triangle that covers the x=5 plane so the ray (0,0,0)→(10,0,0) is blocked.

```kotlin
@Test
fun `target behind wall receives no damage`() {
    val scene = buildSceneWithWallAt(x = 5f)   // implement this helper using the PhysicsSceneTest pattern
    val def = explosionDef(damageStrength = 100f, damageRadius = 20f, damageIsScaled = false)
    val blocked = FakeDamageable(position = Vector3(10f, 0f, 0f))
    val clear   = FakeDamageable(position = Vector3( 3f, 0f, 0f))
    applyDamageToObjects(def, 0f, 0f, 0f, listOf(blocked, clear), physicsScene = scene)
    assertEquals(0f,    blocked.damageTaken, 0.01f)   // blocked by wall
    assertEquals(100f,  clear.damageTaken,   0.01f)   // in front of wall
}
```

Helper stubs to add at the bottom of the test file (implement properly using existing `FakeDamageable` or similar):
```kotlin
private fun explosionDef(damageStrength: Float, damageRadius: Float, damageIsScaled: Boolean) =
    ExplosionDefinitionClass(/* minimal fields */ ...)

// FakeDamageable: a DamageableGameObj subclass that records damageTaken
// Look at how existing tests create test doubles — may already exist
```

Run tests — expect **FAIL** (compilation error because `applyDamageToObjects` has no `physicsScene` param yet):
```bash
cd /Users/marc/Documents/ccr/.worktrees/phase12
kotlin-server/gradlew -p kotlin-server :server:test --tests "ccr.server.net.ExplosionHelperTest" 2>&1 | tail -20
```

### Step 1.3: Add `physicsScene` parameter and ray cast to `applyDamageToObjects`

In `ExplosionHelper.kt`:

**Add imports** at the top (if not present):
```kotlin
import ccr.math.LineSeg
import ccr.physics.collision.RayCollisionTest
import ccr.physics.scene.PhysicsScene
```

**Change the function signature** — add optional last parameter:
```kotlin
fun applyDamageToObjects(
    explosionDef: ExplosionDefinitionClass,
    posX: Float,
    posY: Float,
    posZ: Float,
    objects: List<*>,
    physicsScene: PhysicsScene? = null,       // ← add this
) {
```

**Add the ray cast** inside the per-object loop, after the distance check passes and before `applyDamage`. The insertion point is immediately before the `val dist = sqrt(distSq)` line:

```kotlin
        // Line-of-sight check: skip targets blocked by static terrain
        // C++: ExplosionClass::Damage_Objects casts a ray per target
        if (physicsScene != null && distSq > 0.0001f) {
            val ray = RayCollisionTest(
                ray = LineSeg(
                    p0 = Vector3(posX, posY, posZ),
                    p1 = Vector3(objPos.x, objPos.y, objPos.z),
                ),
                checkStatic  = true,
                checkDynamic = false,   // only terrain walls occlude; other entities don't
            )
            if (physicsScene.castRay(ray)) continue   // blocked → no damage
        }
```

Note on `distSq > 0.0001f`: skip the ray cast for direct hits (explosion centre coincides with target) — a zero-length `LineSeg` would produce a degenerate ray.

**Update `applyExplosionDamage()`** — the only call site — to pass the scene:

Find the call to `applyDamageToObjects(...)` inside `applyExplosionDamage()` and add `physicsScene = server.physicsScene`:
```kotlin
    applyDamageToObjects(
        explosionDef = explosionDef,
        posX         = posX,
        posY         = posY,
        posZ         = posZ,
        objects      = server.gameObjManager.getAllObjects(),
        physicsScene = server.physicsScene,    // ← add this
    )
```

### Step 1.4: Run tests — expect PASS

```bash
kotlin-server/gradlew -p kotlin-server :server:test 2>&1 | tail -15
```

Expected: BUILD SUCCESSFUL. All 17+ tests pass (including the 3 new ray occlusion tests).

If the wall test fails because the PhysicsScene API for adding static geometry is different from what you assumed in Step 1.2, look at `PhysicsSceneTest.kt` more carefully and adjust the `buildSceneWithWallAt()` helper. The implementation in `applyDamageToObjects` itself should not need changing — only the test helper.

### Step 1.5: Commit

```bash
cd /Users/marc/Documents/ccr/.worktrees/phase12
git add kotlin-server/server/src/main/kotlin/ccr/server/net/ExplosionHelper.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/net/ExplosionHelperTest.kt
git commit -m "feat: add ray occlusion to explosion damage — skip targets blocked by terrain"
```

---

## Verification

```bash
kotlin-server/gradlew -p kotlin-server test
```

All tests pass. Manual verification with a real client:
- C4 on one side of a thick building → soldier on far side does NOT take damage
- C4 in open space → all soldiers in radius take damage with falloff
- Nuke/ion beacon → same occlusion rules apply

## Note

Save this plan to `/plans/phase12-explosion-occlusion.md` immediately once approved.
