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
 * Inheritance: StaticAnimPhysClass : StaticPhysClass : PhysClass
 *
 * Server-side simplifications:
 *  - No render model, no texture projectors, no shadow projector.
 *  - AnimCollisionManagerClass::timestep() advances frame state but performs no scene collision.
 *  - Rider tracking is maintained; physical pushing is not performed (no physics scene queries).
 *
 * C++ constructor notes:
 *  - Calls Set_Collision_Group(0xF) — hard-coded to group 15.
 *  - StaticPhysClass constructor also sets collision group 15, so this is consistent.
 *  - ShadowProjector = NULL (always null server-side).
 */
open class StaticAnimPhysClass : StaticPhysClass() {

    // -------------------------------------------------------------------------
    // Animated collision + rider manager — mirrors C++ AnimCollisionManagerClass AnimManager
    // -------------------------------------------------------------------------

    /**
     * C++: AnimCollisionManagerClass AnimManager
     *
     * Constructed with a reference to `this` (matches C++ member init).
     * Manages animation frame state, collision mode, and attached riders.
     * Exposed as `animationManager` to match C++ Get_Animation_Manager() naming convention.
     */
    val animationManager: AnimCollisionManagerClass = AnimCollisionManagerClass(this)

    // -------------------------------------------------------------------------
    // Static shadow — mirrors C++ TexProjectClass * ShadowProjector
    //
    // C++: ShadowProjector is non-null when a static shadow projector is attached.
    // On the server there is no renderer, so ShadowProjector is always null.
    // -------------------------------------------------------------------------

    /**
     * C++: virtual bool Is_Casting_Shadow() — returns (ShadowProjector != NULL).
     * Server-side: always false (no ShadowProjector).
     */
    val isCastingShadow: Boolean get() = false

    // -------------------------------------------------------------------------
    // Constructor init block — mirrors StaticAnimPhysClass::StaticAnimPhysClass()
    //
    // C++:
    //   StaticAnimPhysClass::StaticAnimPhysClass(void) :
    //       AnimManager(*this),
    //       ShadowProjector(NULL)
    //   {
    //       Set_Collision_Group(0xF);
    //   }
    // -------------------------------------------------------------------------

    init {
        // C++: Set_Collision_Group(0xF) — hard-codes collision group to 15
        collisionGroup = 0xF
    }

    // -------------------------------------------------------------------------
    // Initialisation — mirrors StaticAnimPhysClass::Init(const StaticAnimPhysDefClass &)
    //
    // C++:
    //   void StaticAnimPhysClass::Init(const StaticAnimPhysDefClass & def) {
    //       StaticPhysClass::Init(def);                           // sets model, flags, etc.
    //       ProjectorManager.Init(def.ProjectorManagerDef,Model); // no-op server-side
    //       AnimManager.Init(def.AnimManagerDef);
    //   }
    // -------------------------------------------------------------------------

    /**
     * C++: void StaticAnimPhysClass::Init(const StaticAnimPhysDefClass & def)
     *
     * Initialises the AnimCollisionManager from the definition.
     * Called by StaticAnimPhysDefClass::Create() after construction.
     *
     * [collisionModeValue] — AnimCollisionManagerDefClass::CollisionMode (0=NONE,1=STOP,2=PUSH,3=KILL)
     * [animationModeValue] — AnimCollisionManagerDefClass::AnimationMode (0=LOOP,1=TARGET,2=MANUAL)
     * [animationName]      — AnimCollisionManagerDefClass::AnimationName
     * [modelName]          — parent model name (used to derive animation name when animationName is empty)
     */
    fun init(
        collisionModeValue: Int,
        animationModeValue: Int,
        animationName: String,
        modelName: String = "",
    ) {
        // C++: StaticPhysClass::Init(def) — sets model, etc; no server-side equivalent here
        // C++: ProjectorManager.Init(def.ProjectorManagerDef, Model) — no-op server-side
        animationManager.init(
            collisionModeValue = collisionModeValue,
            animationModeValue = animationModeValue,
            animationNameFromDef = animationName,
            modelName = modelName,
        )
    }

    // -------------------------------------------------------------------------
    // RTTI — mirrors virtual As_StaticAnimPhysClass()
    // -------------------------------------------------------------------------

    /** C++: virtual StaticAnimPhysClass * As_StaticAnimPhysClass() { return this; } */
    override fun asStaticAnimPhysClass(): StaticAnimPhysClass? = this

    // -------------------------------------------------------------------------
    // Timestep — mirrors virtual Needs_Timestep() / Timestep(float dt)
    //
    // C++:
    //   virtual bool StaticAnimPhysClass::Needs_Timestep() { return true; }
    //
    //   virtual void StaticAnimPhysClass::Timestep(float dt) {
    //       AnimManager.Timestep(dt);
    //       ProjectorManager.Update_From_Model(Model);  // no-op server-side
    //   }
    // -------------------------------------------------------------------------

    /**
     * C++: virtual bool StaticAnimPhysClass::Needs_Timestep() — always true.
     */
    override fun needsTimestep(): Boolean = true

    /**
     * C++: virtual void StaticAnimPhysClass::Timestep(float dt)
     *
     * Delegates to AnimManager.Timestep(dt) to advance animation and check for
     * animated collisions. Also updates texture projectors (no-op server-side).
     */
    override fun timestep(dt: Float) {
        // C++: AnimManager.Timestep(dt)
        animationManager.timestep(dt)
        // C++: ProjectorManager.Update_From_Model(Model) — no-op server-side
    }

    // -------------------------------------------------------------------------
    // Dynamic state — mirrors Has_Dynamic_State() / Save_State() / Load_State()
    //
    // C++:
    //   virtual bool Has_Dynamic_State() { return true; }
    //
    //   virtual void Save_State(ChunkSaveClass & csave) {
    //       csave.Begin_Chunk(STATICANIMPHYS_CHUNK_ANIMMANAGER);
    //       AnimManager.Save(csave);
    //       csave.End_Chunk();
    //   }
    //
    //   virtual void Load_State(ChunkLoadClass & cload) {
    //       while (cload.Open_Chunk()) {
    //           if (cload.Cur_Chunk_ID() == STATICANIMPHYS_CHUNK_ANIMMANAGER)
    //               AnimManager.Load(cload);
    //           cload.Close_Chunk();
    //       }
    //   }
    //
    // Server-side: the Kotlin server does not implement level save-game persistence.
    // ChunkSaveClass/ChunkLoadClass live in the server module (CombatTypeStubs.kt) and
    // the physics module does not depend on the server module. These methods are no-ops
    // since no runtime save-state is needed.
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
     * Saves AnimManager state wrapped in STATICANIMPHYS_CHUNK_ANIMMANAGER.
     * Server-side no-op — no save-game support in Kotlin server.
     */
    override fun saveState(csave: Any) {
        // Server-side: no-op — save-game persistence not implemented
    }

    /**
     * C++: virtual void StaticAnimPhysClass::Load_State(ChunkLoadClass &)
     *
     * Loads AnimManager state from STATICANIMPHYS_CHUNK_ANIMMANAGER.
     * Server-side no-op — no save-game support in Kotlin server.
     */
    override fun loadState(cload: Any) {
        // Server-side: no-op — save-game persistence not implemented
    }

    // -------------------------------------------------------------------------
    // Rider support — mirrors Internal_Link_Rider / Internal_Unlink_Rider
    //
    // C++:
    //   virtual bool Internal_Link_Rider(PhysClass * rider) {
    //       AnimManager.Link_Rider(rider); return true;
    //   }
    //   virtual bool Internal_Unlink_Rider(PhysClass * rider) {
    //       AnimManager.Unlink_Rider(rider); return true;
    //   }
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
    // Update_Cached_Model_Parameters — mirrors C++ virtual method
    //
    // C++:
    //   void StaticAnimPhysClass::Update_Cached_Model_Parameters() {
    //       AnimManager.Update_Cached_Model_Parameters();
    //   }
    // -------------------------------------------------------------------------

    /**
     * C++: void StaticAnimPhysClass::Update_Cached_Model_Parameters()
     *
     * Rebuilds cached model-dependent parameters (collideable mesh list, etc.).
     * Called by Set_Model() and On_Post_Load().
     * Server-side: delegates to AnimManager which is a no-op.
     */
    fun updateCachedModelParameters() {
        animationManager.updateCachedModelParameters()
    }

    // -------------------------------------------------------------------------
    // Chunk IDs — from staticanimphys.cpp
    // -------------------------------------------------------------------------

    companion object {
        /** STATICANIMPHYS_CHUNK_STATICPHYS = 0x01170010
         *  Wraps the parent StaticPhysClass save data. */
        const val STATICANIMPHYS_CHUNK_STATICPHYS: Int = 0x01170010

        /** STATICANIMPHYS_CHUNK_VARIABLES = 0x01170011
         *  Own variables chunk (legacy: only COLLISIONMODE micro-chunk, now moved to AnimManager). */
        const val STATICANIMPHYS_CHUNK_VARIABLES: Int = 0x01170011

        /** STATICANIMPHYS_CHUNK_ANIMMANAGER = 0x01170012
         *  AnimCollisionManagerClass state. */
        const val STATICANIMPHYS_CHUNK_ANIMMANAGER: Int = 0x01170012

        /** Obsolete definition-id micro-chunk (ignored on load). */
        const val STATICANIMPHYS_VARIABLE_DEFID_OBSOLETE: Int = 0x00

        /** Legacy collision-mode micro-chunk; on load, forwarded to animationManager.collisionMode. */
        const val STATICANIMPHYS_VARIABLE_COLLISIONMODE: Int = 0x01
    }
}
