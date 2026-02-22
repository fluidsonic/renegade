# Physics System Implementation Plan

## Context

The Kotlin server currently has all physics **definition classes** parsed from binary data (defs/phys/), but `physicsTickLoop()` in GameServer.kt is an empty stub. We need the full runtime physics simulation — matching the C++ wwphys library — so the server can simulate soldier movement, vehicle dynamics, projectile trajectories, and collision detection. This is the foundation for server-authoritative gameplay.

## Scope

Full physics system: all C++ wwphys class types, mesh-level collision detection, ODE integration (all 4 integrators), spatial partitioning. New `physics` Gradle module mirroring the C++ wwphys library.

## Execution Strategy

Use a **git worktree** for isolation. Execute via **parallel agents** organized into work streams. The plan is structured as independent tasks that can be parallelized.

---

## Work Stream 1: Math Module Extensions

**Files to modify:**
- `math/src/main/kotlin/ccr/math/Vector3.kt` — add `normalized()`, `lengthSquared()`, `unaryMinus()`, `div(scalar)`, `lerp()`
- `math/src/main/kotlin/ccr/math/Quaternion.kt` — add `times(scalar)`, `slerp()`, `plus(other)`, `dot(other)`
- `math/src/main/kotlin/ccr/math/Matrix3D.kt` — add `rotateVector()` (3x3 part only, no translation), `withTranslation()`, `getZRotation()`

**Files to create:**
- `math/src/main/kotlin/ccr/math/Matrix3.kt` — 3x3 matrix for inertia tensors. `fromQuaternion()`, `inverse()`, `transpose()`, `operator times(Matrix3)`, `operator times(Vector3)`, `operator times(Float)`, `IDENTITY`
- `math/src/main/kotlin/ccr/math/LineSeg.kt` — line segment: `p0`, `p1`, precomputed `dp`, `dir`, `length`. `pointAt(t)`, `transform(Matrix3D)`
- `math/src/main/kotlin/ccr/math/AABox.kt` — axis-aligned box: `center`, `extent` (mutable). `min`/`max`, `contains(point/box)`, `overlaps(box)`, `addBox()`, `addPoint()`, `translate()`, `fromMinMax()`, `fromLineSeg()`
- `math/src/main/kotlin/ccr/math/OBBox.kt` — oriented box: `center`, `extent`, `basis` (Matrix3D rotation). `computeAxisAlignedExtent()`, `projectToAxis()`
- `math/src/main/kotlin/ccr/math/Plane.kt` — plane: `normal`, `dist`. `distanceTo(point)`, `intersect(p0, p1)`
- `math/src/main/kotlin/ccr/math/Triangle.kt` — triangle: `v0`, `v1`, `v2`, precomputed `normal`

**Tests to create** (each type gets its own test file):
- `math/src/test/kotlin/ccr/math/Matrix3Test.kt`
- `math/src/test/kotlin/ccr/math/LineSegTest.kt`
- `math/src/test/kotlin/ccr/math/AABoxTest.kt`
- `math/src/test/kotlin/ccr/math/OBBoxTest.kt`
- `math/src/test/kotlin/ccr/math/PlaneTest.kt`
- `math/src/test/kotlin/ccr/math/TriangleTest.kt`
- Extend existing `Vector3Test.kt`, `QuaternionTest.kt` with new method tests
- New `Matrix3DTest.kt` for rotateVector, withTranslation

**C++ references:**
- `Code/WWMath/vector3.h`, `Code/WWMath/matrix3.h`, `Code/WWMath/matrix3d.h`
- `Code/WWMath/quat.h`, `Code/WWMath/lineseg.h`, `Code/WWMath/aabox.h`
- `Code/WWMath/obbox.h`, `Code/WWMath/plane.h`, `Code/WWMath/tri.h`

---

## Work Stream 2: Physics Module Setup + ODE Integration

**Files to create:**

Module setup:
- `physics/build.gradle.kts` — `dependencies { implementation(project(":math")) }`

**Modify** `settings.gradle.kts`: add `:physics` → `include(":math", ":net", ":physics", ":server")`
**Modify** `server/build.gradle.kts`: add `implementation(project(":physics"))` to dependencies

ODE system (`physics/src/main/kotlin/ccr/physics/ode/`):
- `StateVector.kt` — resizable float array. `add(Float)`, `get/set(index)`, `reset()`, `ensureCapacity()`. Needs internal `count` setter for integrator.
- `OdeSystem.kt` — interface: `getState(StateVector)`, `setState(StateVector, startIndex): Int`, `computeDerivatives(t, testState?, dydt, startIndex): Int`
- `Integrator.kt` — object with 4 static methods: `eulerIntegrate()`, `midpointIntegrate()`, `rungeKuttaIntegrate()` (RK4), `rungeKutta5Integrate()` (RK5). Pre-allocated scratch StateVectors.

Physics constants (`physics/src/main/kotlin/ccr/physics/`):
- `PhysicsConstants.kt` — `gravityAcceleration = Vector3(0,0,-9.8)`, `linearDamping = 0.01`, `angularDamping = 0.05`, `restingContactVelocity = 0.1`, `minFrictionVelocity = 0.05`, `defaultContactFriction = 0.5`, `defaultContactDrag = 0.0`. Friction/drag lookup table: `[DynamicObjType][SurfaceType]`
- `SurfaceType.kt` — constants 0..31 matching W3D surface types
- `DynamicObjType.kt` — `TIRE=0`, `TRACK=1`, `GENERIC=2`
- `PhysController.kt` — `moveForward`, `moveLeft`, `moveUp`, `turnLeft` (all Float). `moveVector: Vector3`, `isInactive: Boolean`, `reset()`

**Tests:**
- `physics/src/test/kotlin/ccr/physics/ode/StateVectorTest.kt`
- `physics/src/test/kotlin/ccr/physics/ode/IntegratorTest.kt` — verify against known solutions: free-fall (y = 0.5*g*t^2), simple harmonic oscillator (compare all 4 integrators)
- `physics/src/test/kotlin/ccr/physics/PhysicsConstantsTest.kt`
- `physics/src/test/kotlin/ccr/physics/PhysControllerTest.kt`

**C++ references:**
- `Code/WWMath/ode.h` and `Code/WWMath/ode.cpp` — exact integrator implementations
- `Code/wwphys/physcon.h` and `physcon.cpp` — constants and friction tables
- `Code/wwphys/physcontrol.h` — controller class

---

## Work Stream 3: Collision System

**Files to create** (`physics/src/main/kotlin/ccr/physics/collision/`):
- `CastResult.kt` — mutable result: `startBad`, `fraction` (init 1.0), `normal`, `surfaceType`, `computeContactPoint`, `contactPoint`. `reset()`
- `CollisionType.kt` — bitmask constants: `ALL=0x01`, `PHYSICAL=0x02`, `PROJECTILE=0x04`, `VIS=0x08`, `CAMERA=0x10`, `VEHICLE=0x20`
- `CollisionGroup.kt` — group constants 0-15: `DEFAULT=0`, `UNCOLLIDEABLE=1`, `TERRAIN_ONLY=2`, `BULLET=3`, `TERRAIN_AND_BULLET=4`, `BULLET_ONLY=5`, `SOLDIER=6`, `SOLDIER_GHOST=7`, `TERRAIN=15`
- `CollisionGroupMatrix.kt` — 16x16 symmetric boolean matrix stored as `BooleanArray(256)`. Index = `group0 or (group1 shl 4)`. `doGroupsCollide()`, `enableCollision()`, `disableCollision()`, `enableAll()`, `disableAll()`
- `OverlapType.kt` — enum: `OUTSIDE`, `INSIDE`, `ON`, `OVERLAPPED`
- `CollisionEvent.kt` — data for observer callbacks
- `CollisionReactionType.kt` — enum: `DEFAULT`, `STOP_MOTION`, `NO_BOUNCE`
- `RayCollisionTest.kt` — ray + result + collisionGroup + collisionType + collidedPhysObj + checkStatic/Dynamic flags. `cull(AABox)`
- `AABoxCollisionTest.kt` — box + move + result + precomputed sweepMin/sweepMax. `cull()`
- `OBBoxCollisionTest.kt` — OBBox + move + result + precomputed sweepMin/sweepMax. `cull()`
- `CollisionMath.kt` — static utility object. Key methods:
  - Overlap: `overlapTest(Plane, Vector3)`, `overlapTest(AABox, LineSeg)`, `overlapTest(AABox, AABox)`, `overlapTest(Plane, AABox)`
  - Intersection: `intersectionTest(AABox, AABox)`, `intersectionTest(AABox, Triangle)`, `intersectionTest(OBBox, Triangle)`, `intersectionTest(OBBox, OBBox)`
  - Collide (swept): `collide(LineSeg, Triangle, CastResult)` (Moller-Trumbore), `collide(LineSeg, AABox, CastResult)` (slab method), `collide(LineSeg, OBBox, CastResult)`, `collide(AABox, move, Triangle, CastResult)`, `collide(AABox, move, AABox, CastResult)`, `collide(OBBox, move, Triangle, triMove, CastResult)` (SAT with temporal intervals), `collide(OBBox, move, AABox, aaboxMove, CastResult)`, `collide(OBBox, move, OBBox, otherMove, CastResult)`

**Tests:**
- `physics/src/test/kotlin/ccr/physics/collision/CollisionGroupMatrixTest.kt`
- `physics/src/test/kotlin/ccr/physics/collision/CollisionMathTest.kt` — extensive: known geometric configurations for every method. Ray hits/misses triangle, box sweep hits/misses, SAT separating axis tests
- `physics/src/test/kotlin/ccr/physics/collision/CastResultTest.kt`
- `physics/src/test/kotlin/ccr/physics/collision/RayCollisionTestTest.kt`
- `physics/src/test/kotlin/ccr/physics/collision/AABoxCollisionTestTest.kt`

**C++ references:**
- `Code/ww3d2/coltest.h`, `Code/wwphys/physcoltest.h`
- `Code/WWMath/colmath.h`, `Code/WWMath/colmathaabtri.cpp`, `Code/WWMath/colmathobbobb.cpp`
- `Code/WWMath/castres.h`
- `Code/Combat/combat.h` (collision group initialization)

---

## Work Stream 4: Spatial Partitioning + Physics Scene

**Files to create** (`physics/src/main/kotlin/ccr/physics/spatial/`):
- `AABTreeNode.kt` — `bounds: AABox`, `splitAxis`, `splitDist`, `front`/`back` children, `objectIndices: IntArray?`
- `StaticAABTree.kt` — static geometry culling. `castRay()`, `castAABox()`, `castOBBox()`, `collectObjects()` — all recursive tree traversal with broadphase culling
- `PhysGrid.kt` — uniform grid for dynamic objects. `rePartition(worldMin, worldMax, cellSize)`, `addObject()`, `removeObject()`, `updateObject()`, `castRay()`, `castAABox()`, `castOBBox()`, `collectObjects()`

PhysObserver + PhysClass base (`physics/src/main/kotlin/ccr/physics/`):
- `PhysObserver.kt` — interface: `collisionOccurred(CollisionEvent): CollisionReactionType`, `objectExpired()`, `objectRemovedFromScene()`
- `PhysClass.kt` — abstract base. `flags: Int`, `observer: PhysObserver?`, `transform` (abstract get/set). Flag accessors: `collisionGroup` (bits 0-3), `isImmovable`, `isAsleep`, `isDisabled`, `isFrictionDisabled`, `isSimulationDisabled`, `ignoreCount`. Virtual: `timestep(dt)`, `postTimestepProcess()`, `needsTimestep()`, `castRay()`, `castAABox()`, `castOBBox()`. Flag constants matching C++ bit positions.

Scene (`physics/src/main/kotlin/ccr/physics/scene/`):
- `PhysicsScene.kt` — owns StaticAABTree + PhysGrid + CollisionGroupMatrix + object lists. `addDynamicObject()`, `addStaticObject()`, `removeObject()`. `update(dt)` with sub-step loop (MAX_TIMESTEP = 1/15f). `castRay()`, `castAABox()`, `castOBBox()`, `collectObjects()`. `initDefaultCollisionGroups()` matching C++ CombatManager::Scene_Init.

**Tests:**
- `physics/src/test/kotlin/ccr/physics/PhysClassTest.kt` — flag manipulation, collision group bits
- `physics/src/test/kotlin/ccr/physics/spatial/PhysGridTest.kt`
- `physics/src/test/kotlin/ccr/physics/spatial/StaticAABTreeTest.kt`
- `physics/src/test/kotlin/ccr/physics/scene/PhysicsSceneTest.kt` — add/remove objects, collision group matrix, sub-step loop timing, cast queries

**C++ references:**
- `Code/wwphys/phys.h` — PhysClass flags, interface
- `Code/wwphys/pscene.h`, `Code/wwphys/pscene.cpp`, `Code/wwphys/pscene_collision.cpp`
- `Code/wwphys/physobserver.h`

---

## Work Stream 5: Static + Dynamic Physics Classes

**Files to create** (`physics/src/main/kotlin/ccr/physics/`):

Static (`static/`):
- `StaticPhysClass.kt` — extends PhysClass. Holds collision mesh. `castRay/castAABox/castOBBox` delegate to mesh triangles. `needsTimestep() = false`
- `StaticAnimPhysClass.kt` — extends StaticPhysClass. Animation state, `needsTimestep() = true`
- `ShakeableStaticPhysClass.kt` — extends StaticAnimPhysClass. Shake state
- `AccessiblePhysClass.kt` — extends StaticAnimPhysClass. Base for doors/elevators. Access zone

Dynamic (`dynamic/`):
- `DynamicPhysClass.kt` — extends PhysClass. Visibility frame tracking
- `DecorationPhysClass.kt` — extends DynamicPhysClass. Inert, `timestep` is no-op
- `TimedDecorationPhysClass.kt` — extends DecorationPhysClass. Lifetime timer
- `DynamicAnimPhysClass.kt` — extends DecorationPhysClass. Animation playback
- `LightPhysClass.kt` — extends DecorationPhysClass. Light source params
- `RenderObjPhysClass.kt` — extends DynamicPhysClass. Render object wrapper

**Tests:**
- `physics/src/test/kotlin/ccr/physics/static/StaticPhysClassTest.kt` — collision delegation
- `physics/src/test/kotlin/ccr/physics/static/StaticAnimPhysClassTest.kt`
- `physics/src/test/kotlin/ccr/physics/dynamic/DecorationPhysClassTest.kt`
- `physics/src/test/kotlin/ccr/physics/dynamic/TimedDecorationPhysClassTest.kt`

**C++ references:**
- `Code/wwphys/staticphys.h`, `Code/wwphys/staticanimphys.h`, `Code/wwphys/shakeablestaticphys.h`
- `Code/wwphys/accessiblephys.h`
- `Code/wwphys/dynamicphys.h`, `Code/wwphys/decophys.h`, `Code/wwphys/timeddecophys.h`
- `Code/wwphys/dynamicanimphys.h`, `Code/wwphys/lightphys.h`, `Code/wwphys/renderobjphys.h`

---

## Work Stream 6: Moveable Physics (Soldiers + Projectiles)

**Files to create** (`physics/src/main/kotlin/ccr/physics/moveable/`):
- `MoveablePhysClass.kt` — extends DynamicPhysClass. `mass`, `massInv`, `gravScale`, `elasticity`, `controller: PhysController?`, `carrier: PhysClass?`. `needsTimestep() = true`
- `Phys3State.kt` — data class: `position: Vector3`, `velocity: Vector3`
- `GroundState.kt` — data class: `onGround`, `surfaceType`, `height`, `normal`, `downSlope`
- `Phys3Class.kt` — extends MoveablePhysClass. `collisionBoxExtent: Vector3`. Movement modes enum: `USER_OVERRIDE`, `BALLISTIC`, `SLIDE`, `NORMAL`, `COLLIDE`. `timestep()` dispatches by mode. `checkGround()` casts AABox downward. `ballisticMove()` applies gravity. `slideMove()` slides on steep slopes. `normalMove()` applies controller input rotated by heading. `applyMove()` sweeps collision box with collision response, step-over, wall sliding. `normSpeed`, `slideAngle` (cos threshold), `stepHeight`
- `HumanPhysClass.kt` — extends Phys3Class. Jump logic, crouch speed modifier, `computeDesiredMoveVector()`. Jump speed ~6 m/s upward
- `ProjectileClass.kt` — extends MoveablePhysClass. Point-based ray collision. `lifetime`, `bounceCount`, `gravity`, `tumbleRate`, `tumbleAxis`. Simple Euler integration. `orientationMode` enum

**Tests:**
- `physics/src/test/kotlin/ccr/physics/moveable/MoveablePhysClassTest.kt`
- `physics/src/test/kotlin/ccr/physics/moveable/Phys3ClassTest.kt` — gravity free-fall, ground snap, slope slide threshold, step-over, wall slide
- `physics/src/test/kotlin/ccr/physics/moveable/HumanPhysClassTest.kt` — jump arc, crouch, ground state transitions
- `physics/src/test/kotlin/ccr/physics/moveable/ProjectileClassTest.kt` — ballistic trajectory matches analytical

**C++ references:**
- `Code/wwphys/movephys.h` — MoveablePhysClass
- `Code/wwphys/phys3.h`, `Code/wwphys/phys3.cpp` — Phys3Class movement modes and collision
- `Code/wwphys/humanphys.h`, `Code/wwphys/humanphys.cpp` — HumanPhysClass
- `Code/wwphys/projectile.h`, `Code/wwphys/projectile.cpp` — ProjectileClass

---

## Work Stream 7: Rigid Body + Vehicle Physics

**Files to create** (`physics/src/main/kotlin/ccr/physics/rigidbody/`):
- `RigidBodyState.kt` — data class: `position`, `orientation` (Quaternion), `linearMomentum`, `angularMomentum`. `toVector(StateVector)`, `fromVector(StateVector, index)`. `lerp()` with slerp for orientation. 13 floats total.
- `RigidBodyClass.kt` — extends MoveablePhysClass, implements OdeSystem. State + derived quantities (`rotation: Matrix3`, `inertiaInv`, `velocity`, `angularVelocity`). Body-space inertia: `inertiaBody`, `inertiaBodyInv`. `updateAuxiliaryState()` recomputes derived from state. `computeForceAndTorque(force, torque)`: gravity + aero drag + angular damping + contact forces. `computeDerivatives()`: position dot = velocity, orientation dot = 0.5 * omega * q, momentum dots = force/torque. `integrate(dt)` uses Midpoint by default. `applyImpulse(impulse)` and `applyImpulse(impulse, worldPos)`. `computePointVelocity(worldPos)`. Sleep system: `goToSleepTimer`, `SLEEP_DELAY = 0.5s`, sleep when velocity < threshold and >=3 contacts for delay duration. `computeInertia(dx, dy, dz)` for box shape. `contactPoints[MAX=10]`, `contactNormal`, `contactCount`.
- `OctBoxClass.kt` — contact detection box (used by rigid body for multi-point contact)

Suspension (`physics/src/main/kotlin/ccr/physics/vehicle/`):
- `SuspensionElement.kt` — base strut. Flags bitmask: `FAKE`, `STEERING`, `INV_STEERING`, `TILT_STEERING`, `ENGINE`, `LEFT_TRACK`, `RIGHT_TRACK`, `DISABLED`, `IN_CONTACT`, `BRAKING`. Spring properties. `computeForceAndTorque(force, torque)` abstract
- `Wheel.kt` — extends SuspensionElement. `radius`, `rotation`, `axleTorque`, `slipFactor`. `computeSuspensionForce()`, `computeTractionForces()` abstract, `applyForces()`
- `WVWheel.kt` — wheeled vehicle wheel. Tire friction model with friction circle
- `TrackWheel.kt` — tracked vehicle wheel. Differential steering
- `VTOLWheel.kt` — VTOL landing gear. High friction all directions

Vehicle classes:
- `VehiclePhysClass.kt` — extends RigidBodyClass. `wheels: MutableList<SuspensionElement>`. `springConstant`, `dampingConstant`, `springLength`, `tractionMultiplier`, `lateralMomentArm`, `tractiveMomentArm`. `isFake` (skip all physics). `isEngineOn`, `lastGoodPosition`. `allocSuspensionElement()` abstract. `timestep()` calls super then `updateWheels()`. `computeForceAndTorque()` adds wheel forces before super
- `MotorVehicleClass.kt` — extends VehiclePhysClass. Engine model: `maxEngineTorque`, `torqueCurve`, `engineRPM`. Transmission: `gearCount`, `gearRatios[]`, `finalDriveGearRatio`, `driveTrainInertia`, `shiftUpRpm`, `shiftDownRpm`. `timestep()` adds: read controller → update RPM → shift gears → distribute axle torque → apply brakes → super.timestep()
- `WheeledVehicleClass.kt` — extends MotorVehicleClass. `maxSteeringAngle`. Applies steering to STEERING-flagged wheels from controller.turnLeft
- `TrackedVehicleClass.kt` — extends VehiclePhysClass. Differential steering via LEFT_TRACK/RIGHT_TRACK torque split
- `VTOLVehicleClass.kt` — extends VehiclePhysClass. Flight physics: lift, thrust, drag. PD controller for orientation stabilization (pitch/roll/yaw gains and damping)
- `MotorcycleClass.kt` — extends WheeledVehicleClass. Self-balancing torque, lean into turns

**Tests:**
- `physics/src/test/kotlin/ccr/physics/rigidbody/RigidBodyStateTest.kt` — vector serialization round-trip, lerp/slerp
- `physics/src/test/kotlin/ccr/physics/rigidbody/RigidBodyClassTest.kt` — free-fall, impulse response, inertia computation, sleep system, ODE state get/set round-trip
- `physics/src/test/kotlin/ccr/physics/vehicle/SuspensionElementTest.kt` — spring force computation
- `physics/src/test/kotlin/ccr/physics/vehicle/WheelTest.kt` — traction model
- `physics/src/test/kotlin/ccr/physics/vehicle/VehiclePhysClassTest.kt` — isFake bypass, wheel force integration
- `physics/src/test/kotlin/ccr/physics/vehicle/MotorVehicleClassTest.kt` — engine RPM, gear shifting

**C++ references:**
- `Code/wwphys/rbody.h`, `Code/wwphys/rbody.cpp` — RigidBodyClass (core: Compute_Derivatives, Update_Auxiliary_State, Timestep, sleep)
- `Code/wwphys/wheel.h` — SuspensionElementClass, WheelClass, WVWheelClass, TrackWheelClass, VTOLWheelClass
- `Code/wwphys/vehiclephys.h`, `Code/wwphys/vehiclephys.cpp`
- `Code/wwphys/motorvehicle.h`, `Code/wwphys/motorvehicle.cpp`
- `Code/wwphys/wheelvehicle.h`, `Code/wwphys/trackedvehicle.h`, `Code/wwphys/vtolvehicle.h`, `Code/wwphys/motorcycle.h`

---

## Key Design Decisions

1. **Abstract classes, not sealed** — server module must extend with Combat-specific subclasses (DoorPhysClass, ElevatorPhysClass, DamageableStaticPhysClass)
2. **No render model** — server has no rendering. Collision geometry via triangle lists directly on physics objects
3. **C++ RTTI replaced by Kotlin `is`** — no `As_XxxClass()` methods needed
4. **PhysDefClass as interfaces** — physics module defines interfaces; server's existing parsed def classes implement them
5. **Flag bits match C++ exactly** — for save/load compatibility
6. **Midpoint integration default** — matching C++ rbody.cpp default
7. **Physics module has zero network/IO/coroutine dependencies** — pure computation, fully testable in isolation

## Dependencies Between Streams

```
Stream 1 (Math) ─────────────┐
                              ├─> Stream 2 (ODE + Constants) ──┐
                              ├─> Stream 3 (Collision)          ├─> Stream 6 (Soldiers + Projectiles)
                              ├─> Stream 4 (Scene + PhysClass)  ├─> Stream 7 (Rigid Body + Vehicles)
                              └─> Stream 5 (Static + Dynamic)  ─┘
```

Stream 1 must complete first. Streams 2-5 can run in parallel after Stream 1. Streams 6-7 depend on Streams 2-5.

## Parallel Execution Plan

**Phase A** (1 agent): Stream 1 — Math module extensions
**Phase B** (4 agents in parallel): Streams 2, 3, 4, 5
**Phase C** (2 agents in parallel): Streams 6, 7

## Verification

After each stream completes:
```bash
/Users/marc/Documents/ccr/kotlin-server/gradlew -p /Users/marc/Documents/ccr/kotlin-server test
```

After all streams:
1. All tests pass: `gradlew test`
2. Build succeeds: `gradlew build`
3. Physics module structure mirrors C++ wwphys package layout
4. Every public class has corresponding test file
5. Assertions present at: fraction bounds [0,1], collision group range [0,15], quaternion normalization, mass > 0, dt > 0

## Status

- [x] Stream 1: Math Module Extensions — 57 tests
- [x] Stream 2: Physics Module Setup + ODE Integration — 15 tests
- [x] Stream 3: Collision System — 28 tests
- [x] Stream 4: Spatial Partitioning + Physics Scene — 21 tests
- [x] Stream 5: Static + Dynamic Physics Classes — 12 tests
- [x] Stream 6: Moveable Physics (Soldiers + Projectiles) — 15 tests  (note: math tests counted separately)
- [x] Stream 7: Rigid Body + Vehicle Physics — 22 tests
- **Total physics module: 113 tests, 27 test classes — BUILD SUCCESSFUL**
- **Implemented in branch: `feature/physics-system` (worktree: .worktrees/physics)**
