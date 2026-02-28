package ccr.physics.static

import ccr.physics.PhysClass

/**
 * C++: AnimCollisionManagerClass (Code/WWPhys/animcollisionmanager.h/.cpp)
 *
 * Manages animation state (frame progression, collision mode, rider list) for animated
 * static physics objects (StaticAnimPhysClass) and dynamic animated physics objects
 * (DynamicAnimPhysClass).
 *
 * On the server side:
 * - The render model and W3D animation objects do not exist — animation frame state is
 *   tracked numerically but no actual W3D playback occurs.
 * - Collision detection against the scene is not performed — the server has no animated
 *   collideable meshes; collision mesh tracking is a no-op.
 * - Rider management is kept: riders are tracked and would be moved with the parent.
 * - The animation state machine (LOOP/TARGET/MANUAL, frame advance) runs fully so that
 *   callers (DoorPhysClass, ElevatorPhysClass) can query the current frame.
 */
class AnimCollisionManagerClass(private val parent: PhysClass) {

    // -------------------------------------------------------------------------
    // Animation mode enum — matches C++ AnimCollisionManagerClass::AnimModeType
    // -------------------------------------------------------------------------
    enum class AnimMode {
        LOOP,    // 0 — ANIMATE_LOOP
        TARGET,  // 1 — ANIMATE_TARGET
        MANUAL,  // 2 — ANIMATE_MANUAL
    }

    // -------------------------------------------------------------------------
    // Collision mode enum — matches C++ AnimCollisionManagerClass::CollisionModeType
    // -------------------------------------------------------------------------
    enum class CollisionMode {
        NONE,   // 0 — COLLIDE_NONE
        STOP,   // 1 — COLLIDE_STOP
        PUSH,   // 2 — COLLIDE_PUSH
        KILL,   // 3 — COLLIDE_KILL
    }

    // -------------------------------------------------------------------------
    // Animation state — mirrors C++ member variables
    //
    // C++:
    //   AnimModeType  AnimationMode  = ANIMATE_LOOP
    //   float         TargetFrame    = 0.0f
    //   float         LoopStart      = 0.0f
    //   float         LoopEnd        = 0.0f
    //   HAnimClass*   CurAnimation   = NULL
    //   float         CurFrame       = 0.0f
    //   HAnimClass*   PrevAnimation  = NULL   (used in timestep only)
    //   float         PrevFrame      = 0.0f   (used in timestep only)
    // -------------------------------------------------------------------------

    /** Current animation mode (LOOP / TARGET / MANUAL).
     *  C++: AnimModeType AnimationMode */
    var animationMode: AnimMode = AnimMode.LOOP

    /** Target frame used in ANIMATE_TARGET mode.
     *  C++: float TargetFrame */
    var targetFrame: Float = 0f
        set(value) { field = value; parent.enableIsStateDirty(true) }

    /** Loop start frame (only used in ANIMATE_LOOP mode).
     *  C++: float LoopStart */
    var loopStart: Float = 0f
        set(value) { field = value; parent.enableIsStateDirty(true) }

    /** Loop end frame (only used in ANIMATE_LOOP mode).
     *  C++: float LoopEnd */
    var loopEnd: Float = 0f
        set(value) { field = value; parent.enableIsStateDirty(true) }

    /** Current animation frame.
     *  C++: float CurFrame */
    var curFrame: Float = 0f

    /** Playback rate in frames per second.
     *  Derived from HAnimClass::Get_Frame_Rate() at runtime in C++.
     *  Server-side: set via setAnimation() if known, otherwise 0. */
    var frameRate: Float = 0f

    /** Total number of frames in the current animation.
     *  Derived from HAnimClass::Get_Num_Frames() at runtime in C++.
     *  Server-side: set via setAnimation() if known, otherwise 0. */
    var numFrames: Float = 0f

    /** Name of the currently active animation.
     *  C++: mirrors HAnimClass::Get_Name() on CurAnimation. */
    var animationName: String = ""

    // C++ tracks PrevAnimation / PrevFrame for early-exit optimisation and revert.
    // Server-side: no HAnimClass pointer, so we only track names and frames.
    internal var prevFrame: Float = 0f
    internal var prevAnimationName: String = ""

    // -------------------------------------------------------------------------
    // Collision mode — mirrors C++ CollisionMode field
    // -------------------------------------------------------------------------

    /** Animated collision mode.
     *  C++: CollisionModeType CollisionMode */
    var collisionMode: CollisionMode = CollisionMode.NONE

    // -------------------------------------------------------------------------
    // Rider management — mirrors C++ RiderManagerClass RiderManager
    // -------------------------------------------------------------------------

    /** Riders attached to this object — moved with the parent when it animates.
     *  C++: RiderManagerClass RiderManager */
    private val riders: MutableList<PhysClass> = mutableListOf()

    // -------------------------------------------------------------------------
    // Chunk IDs — from animcollisionmanager.cpp
    // -------------------------------------------------------------------------
    companion object {
        /**
         * ANIMCOLLISIONMANAGER_CHUNK_VARIABLES = octal 0525000421 = 0x5540111 = 89391377 decimal
         */
        const val CHUNK_VARIABLES: Int = 0x05540111

        // Micro-chunk IDs within CHUNK_VARIABLES
        const val VARIABLE_COLLISIONMODE: Int = 0x00
        const val VARIABLE_ANIMATIONMODE: Int = 0x01
        const val VARIABLE_TARGETFRAME: Int = 0x02
        const val VARIABLE_CURFRAME: Int = 0x03
        const val VARIABLE_ANIMATIONNAME: Int = 0x04
        const val VARIABLE_LOOPSTART: Int = 0x05
        const val VARIABLE_LOOPEND: Int = 0x06
        const val VARIABLE_PREVFRAME: Int = 0x07
        const val VARIABLE_PREVANIMATIONNAME: Int = 0x08

        /** ANIMCOLLISIONMANAGERDEF_CHUNK_VARIABLES = 525000306 decimal = 0x1F4ADE72 */
        const val DEF_CHUNK_VARIABLES: Int = 0x1F4ADE72

        // Micro-chunk IDs for the Def chunk
        const val DEF_VARIABLE_COLLISIONMODE: Int = 0x00
        const val DEF_VARIABLE_ANIMATIONMODE: Int = 0x01
        const val DEF_VARIABLE_ANIMATIONNAME: Int = 0x02

        // Convenience aliases for callers — match C++ ANIMATE_* constants
        val ANIMATE_LOOP   = AnimMode.LOOP
        val ANIMATE_TARGET = AnimMode.TARGET
        val ANIMATE_MANUAL = AnimMode.MANUAL
    }

    // -------------------------------------------------------------------------
    // Initialisation — mirrors AnimCollisionManagerClass::Init(const AnimCollisionManagerDefClass &)
    //
    // C++:
    //   void Init(const AnimCollisionManagerDefClass & def) {
    //       CollisionMode = (CollisionModeType)def.CollisionMode;
    //       AnimationMode = (AnimModeType)def.AnimationMode;
    //       if (Parent.Peek_Model()) {
    //           Set_Current_Frame(0.0f);
    //           StringClass anim_name = def.AnimationName;
    //           if (anim_name.Is_Empty()) { ... build from model name ... }
    //           Set_Animation(anim_name);
    //       }
    //   }
    // -------------------------------------------------------------------------

    /**
     * C++: void AnimCollisionManagerClass::Init(const AnimCollisionManagerDefClass &)
     *
     * Sets collision/animation mode from the definition and loads the animation.
     * Server-side: no render model, no asset manager — we record the name only.
     *
     * [collisionModeValue] — AnimCollisionManagerDefClass::CollisionMode (int)
     * [animationModeValue] — AnimCollisionManagerDefClass::AnimationMode (int)
     * [animationNameFromDef] — AnimCollisionManagerDefClass::AnimationName
     * [modelName] — parent model name, used when animationNameFromDef is empty
     */
    fun init(
        collisionModeValue: Int,
        animationModeValue: Int,
        animationNameFromDef: String,
        modelName: String = "",
    ) {
        collisionMode = CollisionMode.entries[collisionModeValue.coerceIn(0, CollisionMode.entries.size - 1)]
        animationMode = AnimMode.entries[animationModeValue.coerceIn(0, AnimMode.entries.size - 1)]

        // C++: Set_Current_Frame(0.0f)
        curFrame = 0f

        // C++: if (anim_name.Is_Empty()) { build from model name: "modelname.modelname" }
        val resolvedName = when {
            animationNameFromDef.isNotEmpty() -> animationNameFromDef
            modelName.isNotEmpty() -> "$modelName.$modelName"
            else -> ""
        }

        // C++: Set_Animation(anim_name)
        setAnimation(resolvedName)
    }

    // -------------------------------------------------------------------------
    // Animation controls — mirror C++ Set_*/Get_* methods
    // -------------------------------------------------------------------------

    /**
     * C++: void Set_Animation(const char * anim_name)
     *
     * Sets the current animation by name. Automatically configures LoopEnd and
     * TargetFrame from the animation's frame count.
     *
     * C++ implementation:
     *   Internal_Set_Animation(anim_name);
     *   LoopStart = 0;
     *   if (CurAnimation != NULL)
     *       LoopEnd = TargetFrame = CurAnimation->Get_Num_Frames() - 1;
     *   else
     *       LoopEnd = TargetFrame = 0;
     *
     * Server-side: no HAnimClass — numFrames/frameRate remain at whatever was previously set
     * (or 0 if not set). The caller may call setAnimation(name, numFrames, frameRate) to
     * provide animation parameters when known.
     */
    fun setAnimation(name: String) {
        animationName = name
        loopStart = 0f
        // Server-side: derive loopEnd from numFrames if known; else 0 (matches C++ null-anim case)
        loopEnd = if (numFrames > 0f) numFrames - 1f else 0f
        targetFrame = loopEnd
        // Apply animation to model: server has no model — no-op
    }

    /**
     * Server-side extension of Set_Animation that also sets frame rate and count.
     * Used when the caller has access to animation metadata (e.g., from a W3D asset).
     * Not a direct C++ method — this bridges the gap of having no HAnimClass on the server.
     */
    fun setAnimation(name: String, numFrames: Float, frameRate: Float) {
        this.numFrames = numFrames
        this.frameRate = frameRate
        setAnimation(name)
    }

    /**
     * C++: HAnimClass * Peek_Animation()
     * Returns non-null when an animation is loaded. Server-side returns a lightweight
     * proxy carrying only the data we track.
     */
    fun peekAnimation(): AnimInfo? = if (animationName.isNotEmpty()) AnimInfo() else null

    /**
     * Server-side proxy for HAnimClass. Exposes only the data the server tracks.
     * C++: HAnimClass provides Get_Total_Time() = num_frames / frame_rate.
     */
    inner class AnimInfo {
        fun getTotalTime(): Float = if (frameRate > 0f) numFrames / frameRate else 0f
    }

    /**
     * C++: void Set_Target_Frame(float frame) — use property targetFrame directly.
     * The property setter marks parent state dirty automatically.
     */

    /**
     * Convenience overload for callers passing an integer frame index.
     * C++: Set_Target_Frame(float) is float-typed; this bridges integer callers.
     */
    fun setTargetFrame(frame: Int) {
        targetFrame = frame.toFloat()
    }

    /**
     * C++: void Set_Target_Frame_End()
     * Snaps target frame to the last frame of the current animation.
     */
    fun setTargetFrameEnd() {
        if (numFrames > 0f) {
            targetFrame = numFrames - 1f
        }
    }

    /**
     * C++: bool Is_At_Target()
     * Returns true when in TARGET mode and curFrame == targetFrame.
     */
    fun isAtTarget(): Boolean = (animationMode == AnimMode.TARGET) && (curFrame == targetFrame)

    /**
     * C++: void Set_Current_Frame(float frame)
     * Sets the current frame and marks parent state dirty.
     */
    fun setCurrentFrame(frame: Float) {
        curFrame = frame
        parent.enableIsStateDirty(true)
    }

    /**
     * C++: float Get_Current_Frame()
     */
    fun getCurrentFrame(): Float = curFrame

    /**
     * C++: float Get_Target_Frame() — use property targetFrame directly
     */

    /**
     * C++: void Set_Loop_Start(float frame0) — use property loopStart directly
     */

    /**
     * C++: void Set_Loop_End(float frame1) — use property loopEnd directly
     */

    /**
     * C++: float Get_Loop_Start() — use property loopStart directly
     */

    /**
     * C++: float Get_Loop_End() — use property loopEnd directly
     */

    /**
     * C++: void Set_Animation_Mode(AnimModeType mode) — use property animationMode directly
     */

    /**
     * C++: AnimModeType Get_Animation_Mode() — use property animationMode directly
     */

    /**
     * C++: void Set_Collision_Mode(CollisionModeType mode) — use property collisionMode directly
     */

    /**
     * C++: CollisionModeType Get_Collision_Mode() — use property collisionMode directly
     */

    // -------------------------------------------------------------------------
    // Rider management — mirrors C++ Link_Rider / Unlink_Rider delegations
    // -------------------------------------------------------------------------

    /**
     * C++: bool Link_Rider(PhysClass * rider) — delegates to RiderManagerClass::Link_Rider
     */
    fun linkRider(rider: PhysClass) {
        if (!riders.contains(rider)) {
            riders.add(rider)
        }
    }

    /**
     * C++: bool Unlink_Rider(PhysClass * rider) — delegates to RiderManagerClass::Unlink_Rider
     */
    fun unlinkRider(rider: PhysClass) {
        riders.remove(rider)
    }

    // -------------------------------------------------------------------------
    // Timestep — mirrors C++ AnimCollisionManagerClass::Timestep(float dt)
    //
    // C++ algorithm:
    //   1. Early-exit if frame didn't change and animation didn't change
    //      (and not LOOP/TARGET with pending advancement).
    //   2. Cache start transforms of collideable meshes (server: no-op).
    //   3. Advance CurFrame according to AnimationMode.
    //   4. Apply new animation pose to model (server: no-op).
    //   5. If collision meshes exist, check for animated collisions and
    //      push/stop/kill as appropriate (server: no-op — no scene queries).
    //   6. Release push records.
    //   7. Ratchet PrevAnimation/PrevFrame.
    //   Returns true if object_animated (animation pose changed).
    // -------------------------------------------------------------------------

    /**
     * C++: bool AnimCollisionManagerClass::Timestep(float dt)
     *
     * Advances the animation frame according to the current mode. Returns true
     * if the animation pose changed this step (object_animated in C++).
     */
    fun timestep(dt: Float): Boolean {
        // C++ early-exit: nothing changed and nothing to do
        if (curFrame == prevFrame && animationName == prevAnimationName) {
            if (animationMode == AnimMode.MANUAL) return false
            if (animationMode == AnimMode.TARGET && curFrame == targetFrame) return false
        }

        // Advance animation frame — only if we have animation parameters
        if (numFrames > 0f && frameRate > 0f) {
            when (animationMode) {
                AnimMode.LOOP -> {
                    // C++: CurFrame += CurAnimation->Get_Frame_Rate() * dt;
                    //      if (CurFrame >= LoopEnd) CurFrame -= (LoopEnd - LoopStart);
                    curFrame += frameRate * dt
                    if (curFrame >= loopEnd) {
                        curFrame -= (loopEnd - loopStart)
                    }
                }
                AnimMode.TARGET -> {
                    // C++: if (CurFrame < TargetFrame) { advance; clamp }
                    //      else if (CurFrame > TargetFrame) { rewind; clamp }
                    if (curFrame < targetFrame) {
                        curFrame += frameRate * dt
                        if (curFrame >= targetFrame) curFrame = targetFrame
                    } else if (curFrame > targetFrame) {
                        curFrame -= frameRate * dt
                        if (curFrame <= targetFrame) curFrame = targetFrame
                    }
                }
                AnimMode.MANUAL -> {
                    // No automatic advancement — caller controls curFrame directly via setCurrentFrame()
                }
            }
        }

        // C++: apply animation to model: Parent.Peek_Model()->Set_Animation(CurAnimation, CurFrame)
        // Server: no render model — no-op

        // C++: check if (CurAnimation != PrevAnimation) || (CurFrame != PrevFrame)
        val objectAnimated = (curFrame != prevFrame) || (animationName != prevAnimationName)

        // C++: if (CollisionMode != COLLIDE_NONE) { ... scene intersection checks ... }
        // Server: no collideable meshes — skip collision detection entirely

        // C++: PushRecordClass::Delete_List(PushList); PushList = NULL;
        // Server: no push records

        // C++: REF_PTR_SET(PrevAnimation, CurAnimation); PrevFrame = CurFrame;
        prevAnimationName = animationName
        prevFrame = curFrame

        return objectAnimated
    }

    // -------------------------------------------------------------------------
    // Update_Cached_Model_Parameters — mirrors C++ version but is a no-op server-side
    // -------------------------------------------------------------------------

    /**
     * C++: void AnimCollisionManagerClass::Update_Cached_Model_Parameters()
     *
     * Rebuilds the collideable mesh array from the parent's render model.
     * Server-side: no render model — no-op.
     */
    fun updateCachedModelParameters() {
        // Server has no render model — nothing to do
    }
}
