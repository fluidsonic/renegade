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
     * C++: TexProjectClass * ShadowProjector — null server-side.
     *
     * Typed as Any? because TexProjectClass does not exist in the physics module.
     * See Set_Shadow() / Is_Casting_Shadow().
     */
    private var shadowProjector: Any? = null

    /**
     * C++: virtual bool Is_Casting_Shadow() — returns (ShadowProjector != NULL).
     * Server-side: always false (no ShadowProjector).
     */
    val isCastingShadow: Boolean get() = shadowProjector != null

    // -------------------------------------------------------------------------
    // Projector manager — mirrors C++ ProjectorManagerClass ProjectorManager
    //
    // C++: ProjectorManagerClass ProjectorManager — manages animated texture projectors.
    // Server-side: no-op placeholder typed as Any? (no ProjectorManagerClass in physics module).
    // -------------------------------------------------------------------------

    /**
     * C++: ProjectorManagerClass ProjectorManager
     *
     * Typed as Any? because ProjectorManagerClass does not exist in the physics module.
     * Initialized via Init() from the definition — no-op server-side.
     */
    private val projectorManager: Any? = null

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
    // Set_Model — mirrors virtual void StaticAnimPhysClass::Set_Model(RenderObjClass * model)
    //
    // C++:
    //   void StaticAnimPhysClass::Set_Model(RenderObjClass * model) {
    //       StaticPhysClass::Set_Model(model);       // sets PhysClass::Model + cull box
    //       Update_Cached_Model_Parameters();        // rebuilds AnimManager collideable mesh list
    //   }
    //
    // Kotlin: PhysClass.model is `var model: Any?` — its JVM setter is already named
    // `setModel`, which would clash with an explicit fun setModel(Any?) in this class.
    // Since @JvmName is not applicable to open member functions and `model` is not declared
    // `open` in PhysClass (so it cannot be overridden as a property), the C++ Set_Model
    // behavior is instead documented here: callers that change the model must call
    // updateCachedModelParameters() immediately after assigning `model`.
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Vis_Render — mirrors virtual void StaticAnimPhysClass::Vis_Render(SpecialRenderInfoClass &)
    //
    // C++: temporarily un-hides the BoundingBox sub-object, calls parent Vis_Render,
    // then restores hidden state. Server-side: no render model — no-op.
    // -------------------------------------------------------------------------

    /**
     * C++: virtual void StaticAnimPhysClass::Vis_Render(SpecialRenderInfoClass & rinfo)
     *
     * Temporarily reveals the "BoundingBox" sub-object so vis rendering captures it,
     * then restores hidden state. Server-side: no-op (no render model, no vis system).
     */
    open fun visRender(rinfo: Any) {
        // Server-side: no render model — no-op
    }

    // -------------------------------------------------------------------------
    // Static properties — typed definition accessor
    //
    // C++: StaticAnimPhysDefClass * Get_StaticAnimPhysDef()
    //      { return (StaticAnimPhysDefClass *)Definition; }
    //
    // Server-side: Definition is Any? (PhysClass.definition). Return as Any?.
    // -------------------------------------------------------------------------

    /**
     * C++: StaticAnimPhysDefClass * Get_StaticAnimPhysDef()
     *
     * Returns the definition object cast to StaticAnimPhysDefClass. In the Kotlin server,
     * PhysClass.definition is typed Any?; callers must cast at the use site.
     */
    fun getStaticAnimPhysDef(): Any? = definition

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
    // Shadow management — mirrors Set_Shadow / Is_Casting_Shadow / Debug_Display_Shadow
    //
    // C++:
    //   void Set_Shadow(TexProjectClass * shadow) {
    //       if (ShadowProjector != NULL) {
    //           PhysicsSceneClass::Get_Instance()->Remove_Static_Texture_Projector(ShadowProjector);
    //           ShadowProjector->Release_Ref();
    //           ShadowProjector = NULL;
    //       }
    //       ShadowProjector = shadow;
    //       if (ShadowProjector != NULL) {
    //           ShadowProjector->Add_Ref();
    //           ShadowProjector->Enable_Affect_Static_Objects(false);
    //           PhysicsSceneClass::Get_Instance()->Add_Static_Texture_Projector(ShadowProjector);
    //       }
    //   }
    //
    //   virtual bool Is_Casting_Shadow() { return (ShadowProjector != NULL); }
    //
    //   void Debug_Display_Shadow(const Vector2 & v0, const Vector2 & v1) { /* render-only */ }
    // -------------------------------------------------------------------------

    /**
     * C++: void StaticAnimPhysClass::Set_Shadow(TexProjectClass * shadow)
     *
     * Attaches a static texture projector as a shadow for this object. In C++ this
     * adds/removes the projector from the physics scene's static projector list and
     * manages Add_Ref/Release_Ref. Server-side: no render scene or texture projectors
     * — records the reference only so that isCastingShadow returns correctly.
     */
    fun setShadow(shadow: Any?) {
        shadowProjector = shadow
    }

    /**
     * C++: void StaticAnimPhysClass::Debug_Display_Shadow(const Vector2 & v0, const Vector2 & v1)
     *
     * Renders a debug visualization of the shadow projector texture in screen space.
     * Server-side: no renderer — no-op.
     */
    fun debugDisplayShadow(v0: Any, v1: Any) {
        // Server-side: no render system — no-op
    }

    // -------------------------------------------------------------------------
    // Save / Load — mirrors virtual bool Save(ChunkSaveClass&) / Load(ChunkLoadClass&)
    //
    // C++ Save():
    //   csave.Begin_Chunk(STATICANIMPHYS_CHUNK_STATICPHYS);
    //   StaticPhysClass::Save(csave);
    //   csave.End_Chunk();
    //   csave.Begin_Chunk(STATICANIMPHYS_CHUNK_ANIMMANAGER);
    //   AnimManager.Save(csave);
    //   csave.End_Chunk();
    //   return true;
    //
    // C++ Load():
    //   int legacy_collision_mode = -1;
    //   while (cload.Open_Chunk()) {
    //       case STATICANIMPHYS_CHUNK_STATICPHYS:  StaticPhysClass::Load(cload);
    //       case STATICANIMPHYS_CHUNK_VARIABLES:   legacy_collision_mode = micro-read
    //       case STATICANIMPHYS_CHUNK_ANIMMANAGER: AnimManager.Load(cload);
    //   }
    //   if (legacy_collision_mode != -1) AnimManager.Set_Collision_Mode(legacy_collision_mode);
    //   SaveLoadSystemClass::Register_Post_Load_Callback(this);
    //   return true;
    //
    // Server-side: ChunkSaveClass/ChunkLoadClass live in the server module; the physics
    // module has no dependency on them. Level loading is handled by the server's own
    // LevelLoader / StaticPhysLoader which reads the same chunks directly. These methods
    // are therefore no-ops in the physics module — they exist only to document the C++
    // wire format and to complete the class interface.
    // -------------------------------------------------------------------------

    /**
     * C++: virtual bool StaticAnimPhysClass::Save(ChunkSaveClass & csave)
     *
     * Writes STATICANIMPHYS_CHUNK_STATICPHYS (parent state) and
     * STATICANIMPHYS_CHUNK_ANIMMANAGER (AnimManager state) to the save stream.
     * Server-side: no-op — level persistence is handled by the server's LevelLoader.
     */
    open fun save(csave: Any): Boolean = true

    /**
     * C++: virtual bool StaticAnimPhysClass::Load(ChunkLoadClass & cload)
     *
     * Reads STATICANIMPHYS_CHUNK_STATICPHYS, STATICANIMPHYS_CHUNK_VARIABLES (legacy
     * collision mode), and STATICANIMPHYS_CHUNK_ANIMMANAGER from the save stream.
     * Also handles legacy STATICANIMPHYS_VARIABLE_COLLISIONMODE forwarding to
     * animationManager.collisionMode. Registers an On_Post_Load callback.
     * Server-side: no-op — level persistence is handled by the server's LevelLoader.
     */
    open fun load(cload: Any): Boolean = true

    // -------------------------------------------------------------------------
    // Update_Cached_Model_Parameters — mirrors C++ protected method
    //
    // C++:
    //   void StaticAnimPhysClass::Update_Cached_Model_Parameters() {
    //       AnimManager.Update_Cached_Model_Parameters();
    //   }
    //
    // Called by: Set_Model(), On_Post_Load()
    // -------------------------------------------------------------------------

    /**
     * C++: void StaticAnimPhysClass::Update_Cached_Model_Parameters()
     *
     * Rebuilds cached model-dependent parameters (collideable mesh list, etc.).
     * Called by setModel() and onPostLoad().
     * Server-side: delegates to AnimManager which is a no-op.
     */
    fun updateCachedModelParameters() {
        // C++: AnimManager.Update_Cached_Model_Parameters()
        animationManager.updateCachedModelParameters()
    }

    // -------------------------------------------------------------------------
    // On_Post_Load — mirrors virtual void StaticAnimPhysClass::On_Post_Load()
    //
    // C++:
    //   void StaticAnimPhysClass::On_Post_Load() {
    //       StaticPhysClass::On_Post_Load();
    //       Update_Cached_Model_Parameters();
    //       StaticAnimPhysDefClass * definition = Get_StaticAnimPhysDef();
    //       if (definition != NULL) {
    //           ProjectorManager.Init(definition->ProjectorManagerDef, Model);
    //       }
    //   }
    //
    // StaticPhysClass::On_Post_Load() calls PhysClass::On_Post_Load() and rebuilds the
    // bounding box from the model. Neither the bounding box nor the projector manager
    // applies server-side, but Update_Cached_Model_Parameters() is still called to
    // keep AnimManager state consistent after a load.
    // -------------------------------------------------------------------------

    /**
     * C++: virtual void StaticAnimPhysClass::On_Post_Load()
     *
     * Post-load callback: rebuilds model-dependent cached parameters and re-initialises
     * the projector manager from the definition. Called by the save/load system after
     * all objects have been loaded.
     * Server-side: calls updateCachedModelParameters(); projector re-init is no-op.
     */
    open fun onPostLoad() {
        // C++: StaticPhysClass::On_Post_Load() — rebuilds cull box from model; server no-op
        // C++: Update_Cached_Model_Parameters()
        updateCachedModelParameters()
        // C++: ProjectorManager.Init(definition->ProjectorManagerDef, Model) — no-op server-side
    }

    // -------------------------------------------------------------------------
    // Update_Sun_Status — mirrors virtual void StaticAnimPhysClass::Update_Sun_Status()
    //
    // C++:
    //   void StaticAnimPhysClass::Update_Sun_Status() {
    //       float MAX_XYSIZE2 = 15.0f * 15.0f;
    //       // Leapfrog past StaticPhysClass to PhysClass::Update_Sun_Status()
    //       // only for "small" animated objects (doors, elevators).
    //       AABoxClass box;
    //       Model->Get_Obj_Space_Bounding_Box(box);
    //       float xysize2 = box.Extent.X * box.Extent.X + box.Extent.Y * box.Extent.Y;
    //       if (xysize2 < MAX_XYSIZE2) {
    //           PhysClass::Update_Sun_Status();   // perform actual sun occlusion check
    //       } else {
    //           Set_Flag(IS_IN_THE_SUN, true);    // large objects: assume always in sun
    //       }
    //   }
    //
    // C++ rationale: StaticPhysClass::Update_Sun_Status just force-sets IS_IN_THE_SUN=true
    // for all static objects. StaticAnimPhysClass "leapfrogs" it to apply the real check
    // only for small animated objects (xysize < 15m radius). Large animated objects also
    // get IS_IN_THE_SUN=true.
    // Server-side: no model, no sun occlusion system. Force IS_IN_THE_SUN=true as the
    // conservative fallback (same as the "large object" branch in C++).
    // -------------------------------------------------------------------------

    /**
     * C++: virtual void StaticAnimPhysClass::Update_Sun_Status()
     *
     * For small animated objects (XY extent < 15m), performs a real sun occlusion check
     * by delegating to PhysClass::Update_Sun_Status(). For large objects forces
     * IS_IN_THE_SUN = true. Server-side: no sun system — force IS_IN_THE_SUN = true.
     */
    protected open fun updateSunStatus() {
        // C++: for large objects or when no model is present: Set_Flag(IS_IN_THE_SUN, true)
        // Server-side: no model, no scene — use conservative "always in sun" path
        flags = flags or IS_IN_THE_SUN
    }

    // -------------------------------------------------------------------------
    // Animation manager accessor — mirrors C++ Get_Animation_Manager()
    //
    // C++: AnimCollisionManagerClass & Get_Animation_Manager() { return AnimManager; }
    //
    // Kotlin: animationManager is a public val — callers access it directly.
    // The C++ accessor returns a mutable reference; Kotlin's val provides the same.
    // No separate getAnimationManager() function needed since Kotlin property access
    // is already the canonical idiom.
    // -------------------------------------------------------------------------

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
