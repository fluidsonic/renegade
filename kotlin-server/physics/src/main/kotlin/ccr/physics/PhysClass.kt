package ccr.physics

import ccr.math.AABox
import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.OBBoxCollisionTest
import ccr.physics.collision.RayCollisionTest
import ccr.physics.dynamic.DecorationPhysClass
import ccr.physics.dynamic.DynamicAnimPhysClass
import ccr.physics.dynamic.DynamicPhysClass
import ccr.physics.dynamic.LightPhysClass
import ccr.physics.dynamic.RenderObjPhysClass
import ccr.physics.dynamic.TimedDecorationPhysClass
import ccr.physics.moveable.HumanPhysClass
import ccr.physics.moveable.MoveablePhysClass
import ccr.physics.moveable.Phys3Class
import ccr.physics.moveable.ProjectileClass
import ccr.physics.rigidbody.RigidBodyClass
import ccr.physics.static.AccessiblePhysClass
import ccr.physics.static.DamageableStaticPhysClass
import ccr.physics.static.DoorPhysClass
import ccr.physics.static.ElevatorPhysClass
import ccr.physics.static.StaticAnimPhysClass
import ccr.physics.static.StaticPhysClass
import ccr.physics.vehicle.MotorcycleClass
import ccr.physics.vehicle.MotorVehicleClass
import ccr.physics.vehicle.TrackedVehicleClass
import ccr.physics.vehicle.VehiclePhysClass
import ccr.physics.vehicle.VTOLVehicleClass
import ccr.physics.vehicle.WheeledVehicleClass

abstract class PhysClass {
    var scene: ccr.physics.scene.PhysicsScene? = null

    companion object {
        // Flags — must match C++ PhysClass enum in phys.h exactly
        const val COLLISION_MASK: Int = 0x0000000F       // bits for the collision group
        const val IMMOVABLE: Int = 0x00000100            // this object is immovable
        const val DISABLED: Int = 0x00000200             // some objects can be disabled (e.g. lights)
        const val DEBUGDISPLAY: Int = 0x00000400         // render debugging aids
        const val USERCONTROL: Int = 0x00000800          // ignore physics, move according to controller directly
        const val CASTSHADOW: Int = 0x00001000           // does this object cast a shadow?
        const val FORCE_PROJECTION_SHADOW: Int = 0x00002000  // when shadow mode is BLOBS_PLUS, still uses proper shadow
        const val DONT_SAVE: Int = 0x00004000            // scene should never save this object
        const val ASLEEP: Int = 0x00008000               // object is not moving so simulation was skipped
        const val IS_WS_MESH: Int = 0x00010000           // enable static-world-space-mesh rendering optimizations
        const val IS_PRE_LIT: Int = 0x00020000           // light-mapped object that doesn't need static lights applied
        const val IS_IN_THE_SUN: Int = 0x00040000        // object is illuminated by the sun
        const val IS_STATE_DIRTY: Int = 0x00080000       // object's state has changed
        const val STATIC_LIGHTING_DIRTY: Int = 0x00100000 // object's static lighting cache is dirty
        const val FRICTION_DISABLED: Int = 0x00200000    // friction is disabled for this object
        const val SIMULATION_DISABLED: Int = 0x00400000  // turn on/off simulation for this object

        const val IGNORE_SHIFT: Int = 28                 // shift count for the 'ignore-me' counter
        const val IGNORE_MASK: Int = 0xF0000000.toInt()  // mask for the 'ignore-me' counter
    }

    var flags: Int = 0
    var observer: PhysObserver? = null

    /** Optional instance name (matches C++ Name field). */
    var name: String = ""

    /** Optional instance identifier, unique if non-zero (matches C++ InstanceID). */
    var instanceId: UInt = 0u

    /** Vis object ID assigned by the vis generation process (matches C++ VisObjectID). */
    var visObjectId: UInt = 0u

    /** Definition object containing constants shared between instances (matches C++ Definition). */
    var definition: Any? = null  // TODO: replace with PhysDefClass when added to physics module

    /** C++: RenderObjClass* Model — the render object attached to this physics object. */
    @get:JvmName("modelField") @set:JvmName("setModelField")
    var model: Any? = null  // TODO: replace with RenderObjClass when added to physics module

    abstract var transform: Matrix3D

    var collisionGroup: Int
        get() = flags and COLLISION_MASK
        set(v) {
            flags = (flags and COLLISION_MASK.inv()) or (v and COLLISION_MASK)
        }

    var isImmovable: Boolean
        get() = (flags and IMMOVABLE) != 0
        set(v) {
            flags = if (v) flags or IMMOVABLE else flags and IMMOVABLE.inv()
        }

    var isAsleep: Boolean
        get() = (flags and ASLEEP) != 0
        set(v) {
            flags = if (v) flags or ASLEEP else flags and ASLEEP.inv()
        }

    var isDisabled: Boolean
        get() = (flags and DISABLED) != 0
        set(v) {
            flags = if (v) flags or DISABLED else flags and DISABLED.inv()
        }

    var isFrictionDisabled: Boolean
        get() = (flags and FRICTION_DISABLED) != 0
        set(v) {
            flags = if (v) flags or FRICTION_DISABLED else flags and FRICTION_DISABLED.inv()
        }

    var isSimulationDisabled: Boolean
        get() = (flags and SIMULATION_DISABLED) != 0
        set(v) {
            flags = if (v) flags or SIMULATION_DISABLED else flags and SIMULATION_DISABLED.inv()
        }

    /** The 4-bit ignore counter stored in the top nibble of flags (bits 28..31). */
    var ignoreCount: Int
        get() = (flags ushr IGNORE_SHIFT) and 0xF
        set(v) {
            flags = (flags and IGNORE_MASK.inv()) or ((v and 0xF) shl IGNORE_SHIFT)
        }

    open fun timestep(dt: Float) {}
    open fun postTimestepProcess() {}
    open fun needsTimestep(): Boolean = false
    open fun castRay(test: RayCollisionTest): Boolean = false
    open fun castAABox(test: AABoxCollisionTest): Boolean = false
    open fun castOBBox(test: OBBoxCollisionTest): Boolean = false

    // C++ convenience methods mirrored from PhysClass interface.
    // These exist so the server module can call them on ccr.physics.PhysClass without casting.
    // Note: getTransform()/setTransform() and getCollisionGroup()/setCollisionGroup() are intentionally
    // absent here — they would clash with the JVM getters/setters generated for the `transform` and
    // `collisionGroup` properties.  Server code uses the Kotlin property syntax directly.

    /** C++: Get_Position() — returns the world-space translation of the transform. */
    open fun getPosition(): Vector3 = transform.translation

    /** C++: Set_Position() — no-op stub; MoveablePhysClass overrides via its position property. */
    open fun setPosition(pos: Vector3) {}

    /** C++: Get_Facing() — returns Z-axis rotation of the transform (stub returns 0). */
    open fun getFacing(): Float = 0f

    /** C++: Get_Vis_Object_ID() — returns the vis object ID as Int. */
    open fun getVisObjectId(): Int = visObjectId.toInt()

    /** C++: Is_In_Scene() — true when assigned to a PhysicsScene. */
    open fun isInScene(): Boolean = scene != null

    /** C++: Get_Contact_Surface_Type() — stub returns 0. */
    open fun getContactSurfaceType(): Int = 0

    /** C++: Is_Enabled() — inverse of isDisabled flag. */
    open fun isEnabled(): Boolean = !isDisabled

    /** C++: Add_Effect_To_Me() — no-op stub. */
    open fun addEffectToMe(effect: Any) {}

    /** C++: Enable_User_Control() — sets/clears the USERCONTROL flag. */
    open fun enableUserControl(enable: Boolean) {
        flags = if (enable) flags or USERCONTROL else flags and USERCONTROL.inv()
    }

    fun getId() =
        instanceId

    /** C++: Peek_Model() — returns the attached render object without ref-counting. */
    open fun peekModel(): Any? = model

    /** C++: Set_Model() — virtual; attaches a render object to this physics object. */
    open fun setModel(model: Any?) { this.model = model }

    /** C++: Set_Model_By_Name() — no-op stub (server has no render objects). */
    open fun setModelByName(name: String) {}

    /** C++: Enable_Is_State_Dirty(bool) — sets/clears the IS_STATE_DIRTY flag. */
    fun enableIsStateDirty(onoff: Boolean) {
        flags = if (onoff) flags or IS_STATE_DIRTY else flags and IS_STATE_DIRTY.inv()
    }

    /** C++: Is_State_Dirty() — returns the IS_STATE_DIRTY flag. */
    fun isStateDirty(): Boolean = (flags and IS_STATE_DIRTY) != 0

    /** C++: Inc_Ignore_Counter() / Dec_Ignore_Counter(). */
    open fun incIgnoreCounter() { ignoreCount = (ignoreCount + 1).coerceAtMost(0xF) }
    open fun decIgnoreCounter() { ignoreCount = (ignoreCount - 1).coerceAtLeast(0) }

    /** World-space bounding box for this object. Used by spatial structures (AABTree, PhysGrid). */
    open fun worldBoundingBox(): AABox {
        val t = transform.translation
        val pad = Vector3(1f, 1f, 1f)
        return AABox.fromMinMax(t - pad, t + pad)
    }

    // Physics RTTI — matches C++ virtual As_*() methods in phys.h
    open fun asDynamicPhysClass(): DynamicPhysClass? = null
    open fun asMoveablePhysClass(): MoveablePhysClass? = null
    open fun asPhys3Class(): Phys3Class? = null
    open fun asHumanPhysClass(): HumanPhysClass? = null
    open fun asRigidBodyClass(): RigidBodyClass? = null
    open fun asVehiclePhysClass(): VehiclePhysClass? = null
    open fun asMotorVehicleClass(): MotorVehicleClass? = null
    open fun asWheeledVehicleClass(): WheeledVehicleClass? = null
    open fun asMotorcycleClass(): MotorcycleClass? = null
    open fun asTrackedVehicleClass(): TrackedVehicleClass? = null
    open fun asVTOLVehicleClass(): VTOLVehicleClass? = null
    open fun asStaticPhysClass(): StaticPhysClass? = null
    open fun asStaticAnimPhysClass(): StaticAnimPhysClass? = null
    open fun asElevatorPhysClass(): ElevatorPhysClass? = null
    open fun asDamageableStaticPhysClass(): DamageableStaticPhysClass? = null
    open fun asDoorPhysClass(): DoorPhysClass? = null
    open fun asDecorationPhysClass(): DecorationPhysClass? = null
    open fun asTimedDecorationPhysClass(): TimedDecorationPhysClass? = null
    open fun asDynamicAnimPhysClass(): DynamicAnimPhysClass? = null
    open fun asLightPhysClass(): LightPhysClass? = null
    open fun asRenderObjPhysClass(): RenderObjPhysClass? = null
    open fun asProjectileClass(): ProjectileClass? = null
    open fun asAccessiblePhysClass(): AccessiblePhysClass? = null
}
