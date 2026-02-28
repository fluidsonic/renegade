# Physics Module (ccr.physics)

- Gradle module: `kotlin-server/physics/`, depends on `:math`. Added to `settings.gradle.kts` and `server/build.gradle.kts`
- Package layout mirrors C++ wwphys: `ode/` (integrators), `collision/` (CollisionMath, CastResult, CollisionGroupMatrix), `spatial/` (StaticAABTree, PhysGrid), `scene/` (PhysicsScene), `static/`, `dynamic/`, `moveable/`, `rigidbody/`, `vehicle/`
- `PhysClass.scene: PhysicsScene?` — set automatically by `PhysicsScene.addDynamicObject()`; used by `timestep()` implementations for collision queries
- `PhysicsScene.update(dt)` sub-steps at MAX_TIMESTEP=1/15f; `castRay/castAABox` query both static tree and dynamic grid
- `CollisionMath` object — all geometry algorithms: Möller-Trumbore (ray/tri), slab method (ray/AABB), SAT (OBB/OBB, OBB/tri), swept AABB/OBB collide methods
- `Integrator.midpointIntegrate(sys, 0f, dt)` — default for `RigidBodyClass`; all 4 methods (Euler/Midpoint/RK4/RK5) in `ccr.physics.ode.Integrator` object
- `Vector3` is a data class with `var` fields — direct mutation works: `force.x += gravX`
- `ccr.physics.static` package — `static` is a Java reserved word but valid Kotlin package name
- `StaticPhysClass` is `open class` (not abstract) — directly instantiable; set `triangles` and `transform` then call `scene.addStaticObject()`
- Static collision geometry loaded by `PhysicsSceneBuilder.build(staticObjects, mapMix, alwaysMix)` → `PhysicsScene`; called inside `GameServer.loadLevel()` after `LevelLoader`; result stored in `GameServer.physicsScene`
- W3D collision flag: `(mesh.attributes and W3D_MESH_FLAG_COLLISION_TYPE_PHYSICAL) != 0u` — only meshes with bit 0x10 set should be added to the scene
- StaticPhysClass save format: `STATICPHYS_CHUNK_PHYS(0xDC2F94)` → `PHYS_CHUNK_VARIABLES(0x660055)` (micro 0x06=definitionId) + `PHYS_CHUNK_MODEL(0x660056)` → `WW3D_PERSIST_CHUNKID_RENDEROBJ(0x10000)` → `RENDOBJFACTORY_CHUNKID_VARIABLES(0x555040)` (micro 0x01=modelName string, 0x02=transform 48 bytes row-major Matrix3D)
- **`VehiclePhysClassStub` removed** — previously `CombatTypeStubs.kt` had `VehiclePhysClassStub : MoveablePhysClass()` as a placeholder; replaced with the real `VehiclePhysClass` from the physics module; future C++ physics class stubs belong in the physics module, not the server module
- **`orientation` computed property on `RigidBodyClass`** — `val orientation: Quaternion get() = state.orientation` (mirrors C++ `Get_Orientation()`)
- **`computeApproximateRideHeight()` on `VehiclePhysClass`** — returns `springLength` (mirrors C++ `Compute_Approximate_Ride_Height()` which uses suspension spring length to estimate ground clearance)
- **`PhysChunkIds` object** — `ccr.physics.PhysChunkIds` contains all chunk ID constants from `wwphysids.h`, `phys.cpp`, and `staticphys.cpp`; server-side loaders should reference these constants rather than hardcoding hex literals
- **Flag constants in `PhysClass.Companion`** — correct C++ bit positions: IMMOVABLE=0x100, DISABLED=0x200, ASLEEP=0x8000, etc.; old names `IMMOVABLE_BIT`/`ASLEEP_BIT` no longer exist
- **`As*()` methods on `PhysClass`** — `asDynamicPhysClass()`, `asVehiclePhysClass()`, etc. (23 methods total, all returning null by default); subclasses override to return `this`; use these instead of Kotlin `is`/`as` casts where C++ would use `As_VehiclePhysClass()`
