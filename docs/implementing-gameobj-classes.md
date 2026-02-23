# Implementing NetworkObject Subclasses — Strict C++ Compliance Rules

## Golden Rules

1. **One item at a time.** Present C++ vs Kotlin comparison for each function/field before implementing.
2. **No deviations from C++** unless explicitly noted below.
3. **Assume all missing classes exist.** Never stub out calls — write the code as if `AnimControlClass`, `PhysClass`, `CombatManager`, `TimeManager`, etc. are all available.
4. **Never add features, helpers, or abstractions** that don't exist in C++.
5. **Never change files other than the one being worked on** without explicit approval.
6. **One file at a time — fix errors only at the very end.** Transform each file as if all dependencies in other files already exist with their correct final API. Do NOT fix cross-file compilation errors mid-stream. Only when ALL files in the hierarchy are ported do you go back and resolve any remaining type mismatches, missing methods, or broken call sites across the whole codebase.

## What Must Be Ported — The Critical Rule

**Port EVERYTHING unless the code is explicitly non-server in the C++ source itself.**

Do NOT skip code just because it looks like it relates to:
- Sound / audio
- Animation
- Visual effects
- Radar blips
- Conversations
- HUD indicators
- Physics simulation

The server needs all of this. Sound events trigger game logic. Animation completion drives observer callbacks. Radar blips affect gameplay state. Conversations affect AI behavior. Physics drives position and collision. **The server is not a headless stub — it is a full simulation.**

Only skip code that is **explicitly guarded** in C++ with one of:
- `#ifndef PARAM_EDITING_ON` / editor-only blocks
- `CombatManager::I_Am_Only_Client()` — client-only rendering paths
- Direct D3D / GL / Win32 / platform rendering calls with no game logic consequence
- `Debug_Say(...)` / `WWPROFILE(...)` — debug/profiling only

When in doubt: **port it.** Add a FIXME if a dependency isn't available yet, but write the call.

---

## Class Declaration

- C++ multiple inheritance → Kotlin: primary parent as superclass, remaining as interfaces
- `CombatPhysObserverClass`, `AudioCallbackClass` etc. → Kotlin `interface`
- Omit `ReferenceableGameObj` mixin — embed its fields/methods directly into the class (see ScriptableGameObj pattern)
- Comment: `// C++: ClassName : public Parent1, public Parent2`

## Fields

- Declare all C++ member fields as Kotlin `var` properties (not constructor params)
- Match C++ initialization values exactly:
  - `NULL` pointer → `null`
  - `true`/`false` → same
  - numeric → same
- `BYTE` → `UByte`, initialized to `0u` unless constructor sets it otherwise
- **Never use `Long` or `ULong`** — C++ is 32-bit based; `unsigned long` / `long` map to `UInt` / `Int` (32-bit). These will become `uint32_t` / `int32_t` in the C++ port. Use `UInt` / `Int` everywhere.
- `GameObjReference` → `val hostGameObj: GameObjReference = GameObjReference()`
- If C++ constructor body sets a field via a method (e.g. `Reset_Server_Skips(255)`), initialize the field to `0` and call the method in an `init` block
- `protected` in C++ → `protected` in Kotlin
- Comment every field: `// C++: Type FieldName (initialized to value)`

## Constructor / Destructor

- C++ constructor initializer list → Kotlin field defaults
- C++ constructor body calls → Kotlin `init { }` block
- C++ destructor → `override fun destruct()`, always ends with `super.destruct()`
- No manual ref counting (`Release_Ref`, `REF_PTR_RELEASE`) — GC handles it; just null the field
- `COMBAT_SCENE` = `CombatManager::Get_Scene()` → `CombatManager.getScene()`
- `COMBAT_STAR` = `CombatManager::Get_The_Star()` → FIXME comment when used

## Methods — General

- C++ virtual → Kotlin `open fun`
- C++ pure virtual → Kotlin `abstract fun`
- C++ non-virtual → Kotlin `fun` (no `open`)
- C++ `WWINLINE` → Kotlin single-expression function
- `DamageableGameObj::Method(definition)` etc. → `super.method(definition)`
- Never use explicit casts to call super — use `super.method()`
- `WWASSERT(x)` → `check(x)` for state assertions; `require(x)` for argument validation
- `check` is redundant alongside `!!` — use `!!` alone, remove `check`
- `As_VehicleGameObj() != NULL` → `this is VehicleGameObj`
- `As_SmartGameObj()` → `this as? SmartGameObj`
- `CombatManager::Are_Observers_Active()` → omit guard (always true on server); add FIXME if behavior differs
- `CombatManager::I_Am_Server()` → omit guard (always server); add FIXME if behavior differs
- `TimeManager::Get_Frame_Seconds()` → `TimeManager.getFrameSeconds()` — wire directly, no FIXME
- Pointer remap system (`REQUEST_POINTER_REMAP`, `Register_Pointer`, `Register_Post_Load_Callback`) → FIXME comment at end of `load()`

## Save / Load

- Chunk IDs: always verify against C++ enum — sequential values starting from a base; missing entries in the Kotlin enum shift all subsequent IDs
- Include LEGACY chunk IDs in `load()` even if not written in `save()`
- Include ALL micro chunk IDs — LEGACY ones too; wire them in `load()`
- `CHUNKID_REFERENCEABLE` → delegate to `referenceableGameObj.save/load(csave/cload)`
- Pointer-based micro chunks (`PHYS_OBSERVER_PTR`, `PHYSICAL_OBJECT`, etc.) → FIXME comment, skip in load
- `load()` starts with `check(fieldThatShouldBeNull == null)` mirroring `WWASSERT`
- Save order must match C++ exactly
- `else -> error("Unrecognized ... chunk ID: ${cload.curChunkId}")` in every `when`

## Network Export/Import

- `Export_Creation` / `Import_Creation` — **do not write `definitionId`** here; that's written by the factory `Prep_Packet` layer upstream
- `Export_Rare` / `Import_Rare` — use `!!` for non-null fields (physObj, model); no nullable fallbacks
- `Export_Frequent` / `Import_Frequent` — keep tight; check host via host's model not own model
- VehicleGameObj hidden bool in `Export_Rare`/`Import_Rare` → `if (this is VehicleGameObj)`

## Property Overrides

- If a superclass has `open var foo`, override it with a custom setter rather than a separate `setFoo()` method:
  ```kotlin
  override var foo: Int
      get() = super.foo
      set(value) { super.foo = value; sideEffect() }
  ```

## Concurrent Modification

- Observer/timer lists that may be modified during callbacks → iterate `observers.toList()`
- Add FIXME: explore `CopyOnWriteArrayList` for identical C++ behavior
- Remove timer **after** firing callback (same as C++), capture reference before loop body

## Ref-Counted Objects

- No manual ref counting in Kotlin — GC handles lifecycle
- Just null the field; don't call `releaseRef()` or `addRef()`

## FIXMEs

Add a FIXME comment (not a stub) when:
- Pointer remap not ported
- `CombatManager::Get_The_Star()` / `Star_Killed()` not ported
- `CombatMaterialEffectManager` / visual-only systems not ported
- Physics pipeline creation (`PhysDefID → PhysClass`) not yet wired
- `Is_First_Load()` / save-game system not ported
- `EncyclopediaMgrClass` not ported

Do NOT add FIXMEs for things that can be implemented immediately (TimeManager, scene access, etc.).

## Naming Conventions

| C++ | Kotlin |
|-----|--------|
| `Get_Foo()` | `getFoo()` or `val foo` |
| `Set_Foo(v)` | `setFoo(v)` or `var foo` setter |
| `Is_Foo()` | `isFoo()` |
| `Peek_Foo()` | `peekFoo()` |
| `COMBAT_SCENE` | `CombatManager.getScene()` |
| `COMBAT_STAR` | `CombatManager.getTheStar()` |
| `PLAYERTYPE_NOD` etc. | top-level constants in `DamageableGameObj.kt` |
| `DEFAULT_COLLISION_GROUP` | companion object constant |
| `HIBERNATION_DELAY` | companion object constant |

## Server-Port Direct Fields

Some C++ fields are derived from `PhysObj` at runtime (position, facing, modelName).
In the Kotlin server port, keep direct `var` fields as fallbacks **only if needed for the server**.
Always prefer delegating to `physObj!!` when physObj is available.
Never use `?: fallbackField` — use `!!` and let it fail if physObj is null.

## Constructor Chain — No Params

All classes from `DamageableGameObj` upward have **no constructor params**. Fields are initialized as class body `var` properties. The full chain:

```
NetworkObject() → BaseGameObj() → ScriptableGameObj() → DamageableGameObj()
→ PhysicalGameObj() → ArmedGameObj() → SmartGameObj() → SoldierGameObj / VehicleGameObj
```

- Never pass health/shield/position/modelName etc. as constructor params
- PhysicalGameObj sets `defenseObject` fields in `copySettings()` via `defenseObject.init(def.defenseObjectDef, this)`
- `definitionId` is accessed via `definition!!.id.toInt()` — stored in `BaseGameObj.definition`

## DefenseObjectClass / OffenseObjectClass

- `DamageableGameObj.defenseObject: DefenseObjectClass` — embedded, not nullable
- Kotlin properties `health`, `healthMax`, `shieldStrength` etc. delegate to `defenseObject`
- `applyDamage(OffenseObjectClass, scale, alternateSkin)` is the C++ signature — must match exactly
- Convenience `applyDamage(Float)` wrapper exists for call sites that don't have an `OffenseObjectClass`
- `completelyDamaged(OffenseObjectClass)` takes the damager — required signature

## ControlClass

- `SmartGameObj.control: ControlClass` — embedded, not nullable
- Delegate methods: `clearControl()`, `setBooleanControl()`, `setAnalogControl()`, `importControlCs()`, etc. all delegate to `control.*`
- `control.clearOneTimeBoolean()` called in `postThink()` — C++: `Control.Clear_One_Time_Boolean()`
- `#if 01` (truthy) branch is always active — port that branch only

## PhysControllerClass

- `SmartGameObj.controller: PhysController` — uses `ccr.physics.PhysController`, not a server-side class
- Save/load inline in SmartGameObj (not via `controller.save()`): chunk `PHYSCONTROLLER_CHUNK_VARIABLES=0x100`, micro 0=MoveVector, 1=TurnLeft
- `MoveablePhysClass.controller = controller` wires it to physics (in `copySettings()`)

## PlayerType

- Constants `PLAYERTYPE_NOD`, `PLAYERTYPE_GDI`, etc. are **top-level package-level constants** in `DamageableGameObj.kt`
- `playerTypesAreEnemies(t1, t2)` is a top-level function in `DamageableGameObj.kt`
- `getColorForTeam(team)` is a top-level function in `DamageableGameObj.kt`
- `DamageableGameObj.playerType` is `open var` with setter that sets `BIT_RARE` dirty
- `SmartGameObj` does NOT have a `team` field — `playerType` IS the team for SmartGameObj/VehicleGameObj

## Stealth System

- `SmartGameObj.stealthEffect: StealthEffectClass?` — nullable, created on demand via `allocStealthEffect()`
- `allocStealthEffect()` creates the effect and sets `fadeDistance = getStealthFadeDistance()`
- Stealth logic runs in `think()`: updates `stealthPowerupTimer`, `stealthFiringTimer`, calls `enableStealth()` / `setBroken()`
- `applyDamage()` → `stealthEffect?.damageOccurred()` when `damage > 0`
- `stealthEffect = null` on `reInit()` — GC handles cleanup (no `REF_PTR_RELEASE`)

## Logical Sound / Combat Sound

- `LogicalListenerClass` — server stub; `addToScene()` / `removeFromScene()` are no-ops
- `registerListener()` has FIXMEs for `setScale()` and `attachToObject()` (audio-only)
- `CombatSound` — plain data class with `position`, `type`, `creator`
- `LogicalSoundClass` — minimal port with `typeMask`, `position`, `userObj`
- `onLogicalHeard()` builds `CombatSound` from sound object and calls `observer.soundHeard()`

## TimeManager

- Global `object TimeManager` in `ccr.server.net` — `setFrameSeconds()` called by game loop each tick
- Use `TimeManager.getFrameSeconds()` directly everywhere — no FIXME needed

## GameObjObserverClass

- Observers assumed to exist with these methods: `damaged()`, `killed()`, `destroyed()`, `created()`, `enemySeen()`, `soundHeard()`, `animationComplete()`, `timerExpired()`, `custom()`
- Pass `this` as sender, `null` for missing references that C++ would pass NULL for

## Checklist Per Class

1. Read C++ `.h` and `.cpp` fully before writing any Kotlin
2. Fields: declare all, match init values, correct types
3. Constructor/destructor: `init {}` + `override fun destruct()`
4. `Init` / `Copy_Settings` / `Re_Init` / `Get_Definition`
5. `Save` / `Load` / `On_Post_Load`
6. `Think` / `Post_Think` (if overridden)
7. Network: `Export/Import_Creation`, `Export/Import_Rare`, `Export/Import_Frequent`
8. All remaining methods from the header, in order
9. Chunk ID constants — verify every value against C++ enum
10. Micro chunk ID constants — verify every value, include LEGACY IDs
11. `companion object` at bottom of class with all chunk/micro IDs + class-level constants
12. All stealth/control/hibernation/listener wiring in correct methods
13. `SmartGameObj`: verify `think()` has full enemy-seen loop + stealth update + super call at end
14. `SmartGameObj`: verify `postThink()` calls `control.clearOneTimeBoolean()`
15. `SmartGameObj`: verify `onLogicalHeard()` builds `CombatSound` and fires `soundHeard()`
