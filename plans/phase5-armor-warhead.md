# Phase 5: ArmorWarheadManager + MCT Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the `ArmorWarheadManager` 1.0× stub with a real armor/warhead damage-multiplier table loaded from `armor.ini`, and make C4 detonation apply the correct per-armor-type scaled damage (including the MCT alternate skin).

**Architecture:** `armor.ini` (inside `always.dbs` MIX) is a section-based INI listing armor-type names, warhead-type names, save IDs, and 2D scale tables. `ArmorWarheadManager` loads these into a flat `Float` array indexed by `armorIndex × numWarheads + warheadIndex`. Save IDs (integers stored in definitions) are mapped to array indices at lookup time. `C4GameObj.detonate()` and `GameServer.handleCsDamageEvent` already call `ArmorWarheadManager.scaleDamage(damage, warheadSaveId, armorSaveId)` — once the table is populated, they get correct multipliers automatically.

**MCT scope:** The MCT *damage path* (using `mctSkin` armor type when `stuckMct = true`) is fully implemented. MCT *detection* (setting `stuckMct = true`) remains `false` (Phase 11 adds physics collision). `BuildingGameObj` gains `armorSaveId` and `mctSkinSaveId` fields populated at construction from the building's defense state + definition.

**Tech Stack:** Kotlin, existing `MixReader`, `kotlin.test`, the game's `always.dbs` MIX archive.

**C++ Reference:**
- `original-untouched/Code/Combat/damage.cpp` — `ArmorWarheadManager::Init()` lines 114-240
- `original-untouched/Code/Combat/damage.h` — class declaration
- INI sections: `Armor_Types`, `Warhead_Types`, `Armor_Save_IDs`, `Warhead_Save_IDs`, `Scale_<ArmorName>`, `Shield_<ArmorName>`
- `BuildingGameObj::Apply_Damage_Building` (building.cpp) — selects `MCTSkin` when `mct_damage = true`

---

## Task 1: Simple INI file parser

**Files:**
- Create: `kotlin-server/server/src/main/kotlin/ccr/server/mix/IniFile.kt`
- Create: `kotlin-server/server/src/test/kotlin/ccr/server/mix/IniFileTest.kt`

The Renegade INI format:
```
[SectionName]
Key=Value
Key2=Value2

[OtherSection]
...
```
- Keys and sections are case-insensitive
- Comments start with `;` (skip)
- Values are trimmed strings
- Sections with no entries are valid

**Step 1: Write the failing test**

```kotlin
// kotlin-server/server/src/test/kotlin/ccr/server/mix/IniFileTest.kt
package ccr.server.mix

import kotlin.test.Test
import kotlin.test.assertEquals

class IniFileTest {
    private val sample = """
        [Armor_Types]
        Flesh=Flesh
        Metal=Metal

        [Armor_Save_IDs]
        Flesh=0
        Metal=1

        [Scale_Flesh]
        Bullet=1.0
        C4=1.5

        [Scale_Metal]
        Bullet=0.5
        C4=2.0
    """.trimIndent().toByteArray()

    @Test fun `getInt returns parsed value`() {
        val ini = IniFile(sample)
        assertEquals(0, ini.getInt("Armor_Save_IDs", "Flesh", -1))
        assertEquals(1, ini.getInt("Armor_Save_IDs", "Metal", -1))
    }

    @Test fun `getFloat returns parsed value`() {
        val ini = IniFile(sample)
        assertEquals(1.5f, ini.getFloat("Scale_Flesh", "C4", 1.0f))
        assertEquals(2.0f, ini.getFloat("Scale_Metal", "C4", 1.0f))
    }

    @Test fun `getString returns parsed value`() {
        val ini = IniFile(sample)
        assertEquals("Flesh", ini.getString("Armor_Types", "Flesh", ""))
    }

    @Test fun `missing key returns default`() {
        val ini = IniFile(sample)
        assertEquals(-1, ini.getInt("Armor_Save_IDs", "Missing", -1))
        assertEquals(99.0f, ini.getFloat("Scale_Flesh", "Missing", 99.0f))
    }

    @Test fun `entryCount returns number of keys in section`() {
        val ini = IniFile(sample)
        assertEquals(2, ini.entryCount("Armor_Types"))
    }

    @Test fun `getEntry returns key name at index`() {
        val ini = IniFile(sample)
        val keys = (0 until ini.entryCount("Armor_Types")).map { ini.getEntry("Armor_Types", it) }
        assertEquals(setOf("Flesh", "Metal"), keys.toSet())
    }

    @Test fun `lookups are case-insensitive`() {
        val ini = IniFile(sample)
        assertEquals(0, ini.getInt("armor_save_ids", "flesh", -1))
        assertEquals(1.5f, ini.getFloat("SCALE_FLESH", "c4", 1.0f))
    }
}
```

**Step 2: Run test to verify it fails**

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*IniFileTest*" 2>&1 | tail -5
```
Expected: FAIL (IniFile does not exist)

**Step 3: Implement IniFile**

```kotlin
// kotlin-server/server/src/main/kotlin/ccr/server/mix/IniFile.kt
package ccr.server.mix

// C++: INIClass (wwlib/ini.cpp) — section-based key=value config file.
// Used by ArmorWarheadManager to load armor.ini from always.dbs.
class IniFile(bytes: ByteArray) {

    // sections: lowercase-name → (ordered key list, lowercase-key → original-value map)
    private val sections = mutableMapOf<String, Pair<MutableList<String>, MutableMap<String, String>>>()

    init {
        var currentSection: Pair<MutableList<String>, MutableMap<String, String>>? = null
        for (raw in bytes.toString(Charsets.ISO_8859_1).lines()) {
            val line = raw.trim().let { if (';' in it) it.substringBefore(';').trim() else it }
            if (line.isEmpty()) continue
            if (line.startsWith('[') && line.endsWith(']')) {
                val name = line.substring(1, line.length - 1).lowercase()
                currentSection = sections.getOrPut(name) { mutableListOf<String>() to mutableMapOf() }
            } else if ('=' in line && currentSection != null) {
                val key = line.substringBefore('=').trim()
                val value = line.substringAfter('=').trim()
                if (key.isNotEmpty() && key.lowercase() !in currentSection.second) {
                    currentSection.first.add(key)
                    currentSection.second[key.lowercase()] = value
                }
            }
        }
    }

    fun getString(section: String, key: String, default: String): String =
        sections[section.lowercase()]?.second?.get(key.lowercase()) ?: default

    fun getInt(section: String, key: String, default: Int): Int =
        getString(section, key, "").toIntOrNull() ?: default

    fun getFloat(section: String, key: String, default: Float): Float =
        getString(section, key, "").toFloatOrNull() ?: default

    fun entryCount(section: String): Int =
        sections[section.lowercase()]?.first?.size ?: 0

    // Returns the original-case key name at [index] in [section].
    fun getEntry(section: String, index: Int): String =
        sections[section.lowercase()]?.first?.getOrNull(index) ?: ""
}
```

**Step 4: Run test to verify it passes**

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*IniFileTest*" 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/mix/IniFile.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/mix/IniFileTest.kt
git commit -m "feat: add IniFile parser for armor.ini loading"
```

---

## Task 2: ArmorWarheadManager — load table from armor.ini bytes

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/combat/ArmorWarheadManager.kt`
- Create: `kotlin-server/server/src/test/kotlin/ccr/server/combat/ArmorWarheadManagerTest.kt`

**Step 1: Write the failing tests**

```kotlin
// kotlin-server/server/src/test/kotlin/ccr/server/combat/ArmorWarheadManagerTest.kt
package ccr.server.combat

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArmorWarheadManagerTest {

    // Minimal armor.ini matching Renegade section format
    private val armorIni = """
        [Armor_Types]
        Flesh=Flesh
        Metal=Metal

        [Warhead_Types]
        Bullet=Bullet
        C4=C4

        [Armor_Save_IDs]
        Flesh=0
        Metal=3

        [Warhead_Save_IDs]
        Bullet=0
        C4=5

        [Scale_Flesh]
        Bullet=1.0
        C4=1.5

        [Scale_Metal]
        Bullet=0.5
        C4=2.0
    """.trimIndent().toByteArray()

    @BeforeTest fun setup() { ArmorWarheadManager.init(armorIni) }

    @Test fun `getDamageMultiplier Flesh vs Bullet`() =
        assertEquals(1.0f, ArmorWarheadManager.getDamageMultiplier(0, 0))

    @Test fun `getDamageMultiplier Flesh vs C4`() =
        assertEquals(1.5f, ArmorWarheadManager.getDamageMultiplier(5, 0))

    @Test fun `getDamageMultiplier Metal vs Bullet`() =
        assertEquals(0.5f, ArmorWarheadManager.getDamageMultiplier(0, 3))

    @Test fun `getDamageMultiplier Metal vs C4`() =
        assertEquals(2.0f, ArmorWarheadManager.getDamageMultiplier(5, 3))

    @Test fun `scaleDamage applies multiplier`() =
        assertEquals(300.0f, ArmorWarheadManager.scaleDamage(150.0f, 5, 3))  // 150 * 2.0

    @Test fun `unknown save ID returns 1f multiplier`() =
        assertEquals(1.0f, ArmorWarheadManager.getDamageMultiplier(99, 99))
}
```

**Step 2: Run to verify failure**

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*ArmorWarheadManagerTest*" 2>&1 | tail -5
```
Expected: FAIL

**Step 3: Implement ArmorWarheadManager**

```kotlin
// kotlin-server/server/src/main/kotlin/ccr/server/combat/ArmorWarheadManager.kt
package ccr.server.combat

import ccr.server.mix.IniFile

// C++: ArmorWarheadManager (damage.cpp) — manages the warhead-vs-armor damage multiplier matrix.
// Table is a flat Float array: multipliers[armorIndex * numWarheads + warheadIndex]
// Save IDs (integers stored in defs/packets) map to array indices via Armor_Save_IDs / Warhead_Save_IDs.
object ArmorWarheadManager {

    private var multipliers: FloatArray = FloatArray(0) { 1.0f }
    private var numWarheads: Int = 1
    // save ID → array index
    private var armorIdToIndex   = mutableMapOf<Int, Int>()
    private var warheadIdToIndex = mutableMapOf<Int, Int>()

    // C++: ArmorWarheadManager::Init — parse armor.ini bytes.
    fun init(bytes: ByteArray) {
        val ini = IniFile(bytes)

        val armorNames   = (0 until ini.entryCount("Armor_Types"))  .map { ini.getEntry("Armor_Types",   it) }
        val warheadNames = (0 until ini.entryCount("Warhead_Types")).map { ini.getEntry("Warhead_Types", it) }
        val na = armorNames.size
        val nw = warheadNames.size
        if (na == 0 || nw == 0) return

        numWarheads = nw
        multipliers = FloatArray(na * nw) { 1.0f }

        // Build save-ID → index maps
        armorIdToIndex.clear()
        for (name in armorNames) {
            val saveId = ini.getInt("Armor_Save_IDs", name, -1)
            if (saveId >= 0) armorIdToIndex[saveId] = armorNames.indexOf(name)
        }
        warheadIdToIndex.clear()
        for (name in warheadNames) {
            val saveId = ini.getInt("Warhead_Save_IDs", name, -1)
            if (saveId >= 0) warheadIdToIndex[saveId] = warheadNames.indexOf(name)
        }

        // Fill multiplier table from Scale_<ArmorName> sections
        for ((ai, armorName) in armorNames.withIndex()) {
            for ((wi, warheadName) in warheadNames.withIndex()) {
                multipliers[ai * nw + wi] = ini.getFloat("Scale_$armorName", warheadName, 1.0f)
            }
        }

        println("[ARMOR] loaded ${na} armor types × ${nw} warhead types")
    }

    // warheadSaveId / armorSaveId are the integer IDs stored in defs (AmmoDefinitionClass.warhead,
    // DamageableGameObj shieldType / skinSaveId). Returns 1.0 if either ID is unknown.
    fun getDamageMultiplier(warheadSaveId: Int, armorSaveId: Int): Float {
        val ai = armorIdToIndex[armorSaveId]   ?: return 1.0f
        val wi = warheadIdToIndex[warheadSaveId] ?: return 1.0f
        return multipliers[ai * numWarheads + wi]
    }

    fun scaleDamage(rawDamage: Float, warheadSaveId: Int, armorSaveId: Int): Float =
        rawDamage * getDamageMultiplier(warheadSaveId, armorSaveId)
}
```

**Step 4: Run tests**

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*ArmorWarheadManagerTest*" 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/combat/ArmorWarheadManager.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/combat/ArmorWarheadManagerTest.kt
git commit -m "feat: ArmorWarheadManager loads real multiplier table from armor.ini"
```

---

## Task 3: Load armor.ini in GameServer

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`

The `alwaysMix` MIX already opened by `loadLevel()` contains `armor.ini`. Extract and init.

**Step 1: In `loadLevel()`, after opening `alwaysMix`, add:**

Find the block (around line 1225) where `alwaysMix` is opened. Immediately before `val level = LevelLoader(...)`, add:

```kotlin
// Load armor multiplier table from armor.ini inside alwaysMix
alwaysMix?.readFile("armor.ini")?.also { armorIniBytes ->
    ArmorWarheadManager.init(armorIniBytes)
}
```

The full context (so Edit can match it):
```kotlin
        val level = LevelLoader(alwaysMix, mapMix, baseName).load()
        loadedLevel = level
```
becomes:
```kotlin
        alwaysMix?.readFile("armor.ini")?.also { ArmorWarheadManager.init(it) }

        val level = LevelLoader(alwaysMix, mapMix, baseName).load()
        loadedLevel = level
```

**Step 2: Also load in the `loadDefinitions()` fallback path**

Inside `loadDefinitions()`, after the `MixReader` is opened (around line 1285, where `val ddb = mix.readFile("objects.ddb")` lives), also extract armor.ini. Add right after the `objects.ddb` read:

```kotlin
mix.readFile("armor.ini")?.also { ArmorWarheadManager.init(it) }
```

Full context to match:
```kotlin
                val ddb = mix.readFile("objects.ddb")
                if (ddb != null) {
                    println("[SERVER] found objects.ddb in $fileName (${mix.fileCount()} files in archive)")
                    ddb
                } else null
```
No change needed here — add a separate try to extract armor.ini after the ddb load block. Actually the cleanest approach: in `loadDefinitions()` after line `val definitions = readDefinitions(ddbData)`, add:

```kotlin
        // Also load armor multiplier table if not already loaded via loadLevel()
        candidates.firstNotNullOfOrNull { fileName ->
            val file = File(dataDir, fileName)
            if (!file.exists()) return@firstNotNullOfOrNull null
            try { MixReader(file.readBytes()).readFile("armor.ini") } catch (e: Exception) { null }
        }?.also { ArmorWarheadManager.init(it) }
```

**Step 3: Add import at top of GameServer.kt**

Add to imports (if not already present):
```kotlin
import ccr.server.combat.ArmorWarheadManager
```
(Already present at line 39.)

**Step 4: Verify build**

```
kotlin-server/gradlew -p kotlin-server :server:compileKotlin 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

**Step 5: Run all tests**

```
kotlin-server/gradlew -p kotlin-server test 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt
git commit -m "feat: load armor.ini into ArmorWarheadManager during level/definition load"
```

---

## Task 4: BuildingGameObj gains armorSaveId + mctSkinSaveId; createBuilding populates them

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/net/BuildingGameObj.kt`
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`

`BuildingGameObj.shieldType` is the armor save ID for normal hits (populated from the defense object's `skinSaveId`). `mctSkinSaveId` is the MCT alternate armor save ID (from `BuildingGameObjDef.mctSkin`).

**Step 1: Add mctSkinSaveId field to BuildingGameObj**

In `BuildingGameObj.kt`, inside the class body (after `var baseController`), add:

```kotlin
    // MCT alternate armor type save ID (from BuildingGameObjDef.mctSkin).
    // Used by C4GameObj.detonate() when stuckMct = true.
    var mctSkinSaveId: Int = 0
```

`shieldType` (from `DamageableGameObj`) already serves as `armorSaveId` for normal hits — no new field needed for that. Building construction just needs to pass the correct `shieldType` value from `lb.defense.skinSaveId`.

**Step 2: Update createBuilding() to pass shieldType and mctSkinSaveId**

In `GameServer.createBuilding()`, look up the building def to get `mctSkin`. Also pass `shieldType = lb.defense.skinSaveId` to each building constructor.

Replace the current `createBuilding` with:

```kotlin
private fun createBuilding(lb: LoadedBuildingGameObj): BuildingGameObj? {
    val pos = Vector3(lb.transform.position.x, lb.transform.position.y, lb.transform.position.z)
    val sphereCenter = Vector3(lb.collectionSphere.center.x, lb.collectionSphere.center.y, lb.collectionSphere.center.z)
    val radius = lb.collectionSphere.radius
    if (!ChunkIds.isBuilding(lb.factoryChunkId)) return null

    val health = lb.defense.healthMax.takeIf { it > 0f } ?: 5000f
    val shieldType = lb.defense.skinSaveId

    val mctSkinSaveId = (loadedLevel?.definitions?.findById(lb.definitionId.toUInt())
        as? ccr.server.defs.BuildingGameObjDef)?.mctSkin ?: 0

    val building = when (lb.factoryChunkId) {
        ChunkIds.GAMEOBJ_BUILDING_POWERPLANT ->
            PowerPlantGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType,
                isPowerOn = lb.isPowerOn, playerType = lb.playerType)
        ChunkIds.GAMEOBJ_BUILDING_REFINERY ->
            RefineryGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType, playerType = lb.playerType)
        ChunkIds.GAMEOBJ_BUILDING_SOLDIERFACTORY ->
            SoldierFactoryGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType, playerType = lb.playerType)
        ChunkIds.GAMEOBJ_BUILDING_WARFACTORY ->
            WarFactoryGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType, playerType = lb.playerType)
        ChunkIds.GAMEOBJ_BUILDING_AIRSTRIP,
        ChunkIds.GAMEOBJ_BUILDING_VEHICLEFACTORY ->
            VehicleFactoryGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType, playerType = lb.playerType)
        ChunkIds.GAMEOBJ_BUILDING_COMCENTER ->
            ComCenterGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType, playerType = lb.playerType)
        else -> BuildingGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType, playerType = lb.playerType)
    }
    building.mctSkinSaveId = mctSkinSaveId
    return building
}
```

Note: all `BuildingGameObj` subclasses inherit the `shieldType` parameter via the DamageableGameObj chain — verify each subclass constructor forwards it.

**Step 3: Verify subclass constructors accept shieldType**

Run:
```
kotlin-server/gradlew -p kotlin-server :server:compileKotlin 2>&1 | grep -i error | head -20
```

If any subclass constructor doesn't forward `shieldType`, add `shieldType: Int = 0` to its parameter list and pass it to `super(...)`.

**Step 4: Run all tests**

```
kotlin-server/gradlew -p kotlin-server test 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/net/BuildingGameObj.kt \
        kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt
git commit -m "feat: BuildingGameObj carries armorSaveId (shieldType) and mctSkinSaveId from def"
```

---

## Task 5: C4 detonation uses ArmorWarheadManager

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/net/C4GameObj.kt`

Currently `detonate()` applies `explosionDef.damageStrength` flat. It should scale by `ArmorWarheadManager.getDamageMultiplier(warheadSaveId, effectiveArmorSaveId)`.

**Step 1: Update C4GameObj.detonate()**

Replace the damage application block in `detonate()`:

```kotlin
        if (server != null && stuckBuilding != null && explosionDefId != 0) {
            val explosionDef = server.loadedLevel?.definitions?.findById(explosionDefId.toUInt())
                as? ExplosionDefinitionClass
            if (explosionDef != null) {
                val damage = explosionDef.damageStrength * if (stuckMct) 2f else 1f
                stuckBuilding!!.applyDamage(damage)
            }
        }
```

with:

```kotlin
        if (server != null && stuckBuilding != null && explosionDefId != 0) {
            val explosionDef = server.loadedLevel?.definitions?.findById(explosionDefId.toUInt())
                as? ExplosionDefinitionClass
            if (explosionDef != null) {
                val warheadSaveId = ammoDefinition?.warhead ?: 0
                val building = stuckBuilding!!
                val effectiveArmorSaveId = if (stuckMct) building.mctSkinSaveId else building.shieldType
                val damage = ArmorWarheadManager.scaleDamage(
                    explosionDef.damageStrength, warheadSaveId, effectiveArmorSaveId)
                building.applyDamage(damage)
            }
        }
```

Add import at top of C4GameObj.kt:
```kotlin
import ccr.server.combat.ArmorWarheadManager
```

**Step 2: Verify build**

```
kotlin-server/gradlew -p kotlin-server :server:compileKotlin 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

**Step 3: Run all tests**

```
kotlin-server/gradlew -p kotlin-server test 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add kotlin-server/server/src/main/kotlin/ccr/server/net/C4GameObj.kt
git commit -m "feat: C4 detonation scales damage via ArmorWarheadManager (armor × warhead table)"
```

---

## Verification Checklist

1. `kotlin-server/gradlew -p kotlin-server test` — all tests pass (including new IniFile + ArmorWarheadManager tests)
2. Server startup log shows `[ARMOR] loaded N armor types × M warhead types` (with N=8, M=~7 for vanilla Renegade)
3. Server startup log shows building `shieldType` > 0 for buildings with a skin set in their def
4. C4 detonation applies scaled damage — MCT hit and non-MCT hit produce different health changes

## Later Phases

| Phase | Name |
|-------|------|
| 6 | SpawnManager Timers + PowerUp |
| 7 | Beacon System |
| 8 | Explosion System (full AoE radius) |
| 9 | Map Rotation |
| 10 | Bandwidth + Polish |
| 11 | Physics Integration (MCT detection via collision) |
