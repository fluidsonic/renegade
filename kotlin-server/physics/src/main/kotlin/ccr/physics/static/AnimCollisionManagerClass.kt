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
    // -------------------------------------------------------------------------

    /** Current animation mode (LOOP / TARGET / MANUAL). */
    var animationMode: AnimMode = AnimMode.LOOP

    /** Target frame used in ANIMATE_TARGET mode. */
    var targetFrame: Float = 0f
        set(value) { field = value; parent.enableIsStateDirty(true) }

    /** Loop start frame (only used in ANIMATE_LOOP mode). */
    var loopStart: Float = 0f
        set(value) { field = value; parent.enableIsStateDirty(true) }

    /** Loop end frame (only used in ANIMATE_LOOP mode). */
    var loopEnd: Float = 0f
        set(value) { field = value; parent.enableIsStateDirty(true) }

    /** Current animation frame. */
    var curFrame: Float = 0f

    /** Playback rate in frames per second. Derived from the animation definition. */
    var frameRate: Float = 0f

    /** Total number of frames in the current animation (0 if no animation). */
    var numFrames: Float = 0f

    /** Name of the currently active animation (mirrors HAnimClass::Get_Name()). */
    var animationName: String = ""

    // C++ tracks PrevAnimation / PrevFrame for early-exit optimisation and revert.
    // We only need PrevFrame server-side (no HAnimClass pointer to compare).
    internal var prevFrame: Float = 0f
    internal var prevAnimationName: String = ""

    // -------------------------------------------------------------------------
    // Collision mode — mirrors C++ CollisionMode field
    // -------------------------------------------------------------------------

    /** Animated collision mode. */
    var collisionMode: CollisionMode = CollisionMode.NONE

    // -------------------------------------------------------------------------
    // Rider management — mirrors C++ RiderManagerClass RiderManager
    // -------------------------------------------------------------------------

    /** Riders attached to this object — moved with the parent when it animates. */
    private val riders: MutableList<PhysClass> = mutableListOf()

    // -------------------------------------------------------------------------
    // Chunk IDs — from animcollisionmanager.cpp
    // -------------------------------------------------------------------------
    companion object {
        /**
         * ANIMCOLLISIONMANAGER_CHUNK_VARIABLES in C++ = octal 0525000421 = decimal 89391377 = 0x555_0111
         * However the C++ file uses an octal literal: 0525000421 in octal = 0x05540111.
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

        /** ANIMCOLLISIONMANAGERDEF_CHUNK_VARIABLES = decimal 525000306 = 0x1F4ADE72 */
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
    // Initialisation — mirrors AnimCollisionManagerClass::Init(def)
    // -------------------------------------------------------------------------

    /**
     * C++: AnimCollisionManagerClass::Init(const AnimCollisionManagerDefClass &)
     *
     * Sets collision/animation mode from the definition. If the model exists and an
     * animation name is provided (or derived from the model name), the animation is
     * set. On the server there is no model, so we just record the parameters.
     */
    fun init(
        collisionModeValue: Int,
        animationModeValue: Int,
        animationNameFromDef: String,
        modelName: String = "",
        numFrames: Float = 0f,
        frameRate: Float = 0f,
    ) {
        collisionMode = CollisionMode.entries[collisionModeValue.coerceIn(0, CollisionMode.entries.size - 1)]
        animationMode = AnimMode.entries[animationModeValue.coerceIn(0, AnimMode.entries.size - 1)]

        val resolvedName = if (animationNameFromDef.isNotEmpty()) {
            animationNameFromDef
        } else if (modelName.isNotEmpty()) {
            "$modelName.$modelName"
        } else {
            ""
        }

        setAnimation(resolvedName, numFrames, frameRate)
        curFrame = 0f
    }

    // -------------------------------------------------------------------------
    // Animation controls — mirror C++ Set_*/Get_* methods
    // -------------------------------------------------------------------------

    /**
     * C++: void Set_Animation(const char * anim_name)
     *
     * Sets animation, auto-configures loopEnd / targetFrame from numFrames.
     */
    fun setAnimation(name: String, numFrames: Float, frameRate: Float) {
        this.animationName = name
        this.numFrames = numFrames
        this.frameRate = frameRate
        loopStart = 0f
        loopEnd = if (numFrames > 0f) numFrames - 1f else 0f
        targetFrame = loopEnd
    }

    /**
     * Server-side proxy for HAnimClass — only exposes what the server needs.
     * C++: HAnimClass provides Get_Total_Time() = num_frames / frame_rate.
     */
    inner class AnimInfo {
        fun getTotalTime(): Float = if (frameRate > 0f) numFrames / frameRate else 0f
    }

    /** C++: HAnimClass * Peek_Animation() — returns non-null when an animation is loaded. */
    fun peekAnimation(): AnimInfo? = if (animationName.isNotEmpty()) AnimInfo() else null

    /** C++: void Set_Target_Frame(int frame) — integer overload used by callers. */
    fun setTargetFrame(frame: Int) { targetFrame = frame.toFloat() }

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
     * Also marks the parent's state dirty.
     */
    fun setCurrentFrame(frame: Float) {
        curFrame = frame
        parent.enableIsStateDirty(true)
    }

    // -------------------------------------------------------------------------
    // Rider management — mirrors C++ Link_Rider / Unlink_Rider delegations
    // -------------------------------------------------------------------------

    /** C++: AnimCollisionManagerClass::Link_Rider — delegates to RiderManagerClass */
    fun linkRider(rider: PhysClass) {
        if (!riders.contains(rider)) {
            riders.add(rider)
        }
    }

    /** C++: AnimCollisionManagerClass::Unlink_Rider — delegates to RiderManagerClass */
    fun unlinkRider(rider: PhysClass) {
        riders.remove(rider)
    }

    // -------------------------------------------------------------------------
    // Timestep — mirrors C++ AnimCollisionManagerClass::Timestep(float dt)
    //
    // Server-side simplification:
    //  - No HAnimClass / render model — frame advancement is purely numerical.
    //  - No collision mesh intersection against the scene.
    //  - Riders are moved conceptually but server has no actual push logic.
    //  - Returns true if the animation pose changed (object_animated).
    // -------------------------------------------------------------------------

    /**
     * C++: bool AnimCollisionManagerClass::Timestep(float dt)
     *
     * Advances the animation frame according to the current mode. Returns true
     * if the animation pose changed this step (object_animated in C++).
     */
    fun timestep(dt: Float): Boolean {
        // Early-exit: nothing changed and nothing to do
        if (curFrame == prevFrame && animationName == prevAnimationName) {
            if (animationMode == AnimMode.MANUAL) return false
            if (animationMode == AnimMode.TARGET && curFrame == targetFrame) return false
        }

        // Advance animation frame
        if (numFrames > 0f && frameRate > 0f) {
            when (animationMode) {
                AnimMode.LOOP -> {
                    curFrame += frameRate * dt
                    if (curFrame >= loopEnd) {
                        curFrame -= (loopEnd - loopStart)
                    }
                }
                AnimMode.TARGET -> {
                    if (curFrame < targetFrame) {
                        curFrame += frameRate * dt
                        if (curFrame >= targetFrame) curFrame = targetFrame
                    } else if (curFrame > targetFrame) {
                        curFrame -= frameRate * dt
                        if (curFrame <= targetFrame) curFrame = targetFrame
                    }
                }
                AnimMode.MANUAL -> {
                    // No automatic advancement — caller controls curFrame directly
                }
            }
        }

        // Determine if animation moved
        val objectAnimated = (curFrame != prevFrame) || (animationName != prevAnimationName)

        // Server-side: no collision mesh intersection — skip collision detection entirely

        // Ratchet prev state for next frame
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
     * Server-side: no render model, so this is a no-op.
     */
    fun updateCachedModelParameters() {
        // Server has no render model — nothing to do
    }
}
