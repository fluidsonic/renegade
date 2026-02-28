package ccr.physics.static

import ccr.physics.PhysClass

/**
 * C++: StaticAnimPhysClass (Code/WWPhys/staticanimphys.h/.cpp)
 *
 * A static physics object that performs additional per-frame processing:
 *  - animation which causes collisions (doors, elevators)
 *  - animation which updates a texture projector (spotlights, animated shadows)
 *  - static shadow management
 *  - rider management (objects that move with the animated geometry)
 *
 * Extends StaticPhysClass directly (does NOT go through DynamicPhysClass or MoveablePhysClass).
 *
 * Server-side simplifications:
 *  - No render model, no texture projectors, no shadow projector.
 *  - AnimCollisionManagerClass::timestep() advances frame state but performs no scene collision.
 *  - Rider tracking is maintained; physical pushing is not performed (no physics scene queries).
 */
open class StaticAnimPhysClass : StaticPhysClass() {

    // -------------------------------------------------------------------------
    // Animated collision + rider manager — mirrors C++ AnimCollisionManagerClass AnimManager
    // -------------------------------------------------------------------------

    /**
     * C++: AnimCollisionManagerClass AnimManager
     * The AnimCollisionManagerClass is constructed with a reference to `this`.
     * Manages animation frame state, collision mode, and attached riders.
     *
     * Exposed as `animationManager` to match C++ Get_Animation_Manager() naming convention
     * (Get_Foo() → Kotlin property foo).
     */
    val animationManager: AnimCollisionManagerClass = AnimCollisionManagerClass(this)

    // -------------------------------------------------------------------------
    // Static shadow — mirrors C++ TexProjectClass * ShadowProjector
    //
    // On the server there is no renderer, so ShadowProjector is always null.
    // -------------------------------------------------------------------------

    /**
     * C++: TexProjectClass * ShadowProjector — always null server-side.
     * C++: bool Is_Casting_Shadow() returns (ShadowProjector != NULL).
     */
    val isCastingShadow: Boolean get() = false

    // -------------------------------------------------------------------------
    // Constructor — mirrors StaticAnimPhysClass::StaticAnimPhysClass()
    //
    // C++: Set_Collision_Group(0xF) — hard-coded to group 15.
    // Note: StaticPhysClass ctor also sets collision group 15, so this is
    // consistent with the base class initialisation.
    // -------------------------------------------------------------------------

    init {
        collisionGroup = 0xF
    }

    // -------------------------------------------------------------------------
    // Initialisation — mirrors StaticAnimPhysClass::Init(const StaticAnimPhysDefClass &)
    //
    // C++:
    //   void StaticAnimPhysClass::Init(const StaticAnimPhysDefClass & def) {
    //       StaticPhysClass::Init(def);
    //       ProjectorManager.Init(def.ProjectorManagerDef, Model);  // no-op server-side
    //       AnimManager.Init(def.AnimManagerDef);
    //   }
    //
    // The three parameters correspond to AnimCollisionManagerDefClass fields:
    //   CollisionMode, AnimationMode, AnimationName.
    // Server-side there is no model and no W3D animation, so numFrames/frameRate are not passed.
    // -------------------------------------------------------------------------

    /**
     * C++: void StaticAnimPhysClass::Init(const StaticAnimPhysDefClass & def)
     *
     * Initialises the AnimCollisionManager from the definition.
     * Called by StaticAnimPhysDefClass::create() after construction.
     *
     * [collisionModeValue] — AnimCollisionManagerDefClass::CollisionMode (0=NONE,1=STOP,2=PUSH,3=KILL)
     * [animationModeValue] — AnimCollisionManagerDefClass::AnimationMode (0=LOOP,1=TARGET,2=MANUAL)
     * [animationName]      — AnimCollisionManagerDefClass::AnimationName
     */
    fun init(collisionModeValue: Int, animationModeValue: Int, animationName: String) {
        animationManager.init(
            collisionModeValue = collisionModeValue,
            animationModeValue = animationModeValue,
            animationNameFromDef = animationName,
        )
    }

    // -------------------------------------------------------------------------
    // RTTI — mirrors virtual As_StaticAnimPhysClass()
    // -------------------------------------------------------------------------

    override fun asStaticAnimPhysClass(): StaticAnimPhysClass? = this

    // -------------------------------------------------------------------------
    // Timestep — mirrors virtual Needs_Timestep() / Timestep(float dt)
    // -------------------------------------------------------------------------

    /**
     * C++: virtual bool StaticAnimPhysClass::Needs_Timestep() — returns true.
     * StaticAnimPhysClass always needs per-frame processing to advance animation.
     */
    override fun needsTimestep(): Boolean = true

    /**
     * C++: virtual void StaticAnimPhysClass::Timestep(float dt)
     *
     * Delegates to AnimManager to advance animation and check for animated collisions.
     * On the C++ side also updates texture projectors; those are not present server-side.
     */
    override fun timestep(dt: Float) {
        // C++: AnimManager.Timestep(dt);
        animationManager.timestep(dt)
        // C++: ProjectorManager.Update_From_Model(Model); — no-op server-side (no projectors)
    }

    // -------------------------------------------------------------------------
    // Dynamic state — mirrors Has_Dynamic_State() / Save_State() / Load_State()
    // -------------------------------------------------------------------------

    /**
     * C++: virtual bool StaticAnimPhysClass::Has_Dynamic_State() — returns true.
     * StaticAnimPhysClass has runtime state (animation frame, collision mode) that
     * can be saved and restored independently of the full object save/load.
     */
    override fun hasDynamicState(): Boolean = true

    /**
     * C++: virtual void StaticAnimPhysClass::Save_State(ChunkSaveClass &)
     *
     * Saves STATICANIMPHYS_CHUNK_ANIMMANAGER containing the AnimManager state.
     * Chunk format mirrors animcollisionmanager.cpp Save().
     */
    override fun saveState(csave: Any) {
        // Server-side: save is not wired to a real ChunkSaveClass.
        // Subclasses (DoorPhysClass, ElevatorPhysClass) maintain their own state.
    }

    /**
     * C++: virtual void StaticAnimPhysClass::Load_State(ChunkLoadClass &)
     *
     * Reads STATICANIMPHYS_CHUNK_ANIMMANAGER and restores AnimManager state.
     */
    override fun loadState(cload: Any) {
        // Server-side: load is not wired to a real ChunkLoadClass.
    }

    // -------------------------------------------------------------------------
    // Rider support — mirrors Internal_Link_Rider / Internal_Unlink_Rider
    // -------------------------------------------------------------------------

    /**
     * C++: virtual bool StaticAnimPhysClass::Internal_Link_Rider(PhysClass * rider)
     *
     * Attaches [rider] to the AnimManager's rider list so it will be moved
     * when the animated geometry moves.
     */
    override fun internalLinkRider(rider: PhysClass): Boolean {
        animationManager.linkRider(rider)
        return true
    }

    /**
     * C++: virtual bool StaticAnimPhysClass::Internal_Unlink_Rider(PhysClass * rider)
     *
     * Detaches [rider] from the AnimManager's rider list.
     */
    override fun internalUnlinkRider(rider: PhysClass): Boolean {
        animationManager.unlinkRider(rider)
        return true
    }

    // -------------------------------------------------------------------------
    // Chunk IDs — from staticanimphys.cpp
    // -------------------------------------------------------------------------

    companion object {
        /** Wraps the parent StaticPhysClass save data. */
        const val STATICANIMPHYS_CHUNK_STATICPHYS: Int = 0x01170010

        /** Own variables chunk (legacy: only COLLISIONMODE micro-chunk, now moved to AnimManager). */
        const val STATICANIMPHYS_CHUNK_VARIABLES: Int = 0x01170011

        /** AnimCollisionManagerClass state. */
        const val STATICANIMPHYS_CHUNK_ANIMMANAGER: Int = 0x01170012

        /** Obsolete definition-id micro-chunk (ignored on load). */
        const val STATICANIMPHYS_VARIABLE_DEFID_OBSOLETE: Int = 0x00

        /** Legacy collision-mode micro-chunk; on load, forwarded to animationManager.collisionMode. */
        const val STATICANIMPHYS_VARIABLE_COLLISIONMODE: Int = 0x01
    }
}
