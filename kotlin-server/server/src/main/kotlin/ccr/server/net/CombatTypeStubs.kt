package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.defs.DefinitionClass
import ccr.physics.PhysClass as RealPhysClass
import ccr.physics.scene.PhysicsScene

// ---------------------------------------------------------------------------
// Serialisation stubs — C++ wwsaveload chunk/micro-chunk I/O
// ---------------------------------------------------------------------------

// C++: ChunkSaveClass (wwsaveload/chunkio.h)
class ChunkSaveClass {
    fun beginChunk(chunkId: UInt): Unit = TODO("stub")
    fun beginChunk(chunkId: Int): Unit = beginChunk(chunkId.toUInt())
    fun endChunk(): Unit = TODO("stub")
    fun beginMicroChunk(id: Int): Unit = TODO("stub")
    fun endMicroChunk(): Unit = TODO("stub")
    fun writeMicroChunk(id: Int, value: Boolean): Unit = TODO("stub")
    fun writeMicroChunk(id: Int, value: Int): Unit = TODO("stub")
    fun writeMicroChunk(id: Int, value: Float): Unit = TODO("stub")
    fun writeMicroChunk(id: Int, value: UInt): Unit = TODO("stub")
    fun writeMicroChunk(id: Int, value: Vector3): Unit = TODO("stub")
    fun writeMicroChunk(id: Int, value: Any?): Unit = TODO("stub")
    fun writeMicroChunkWwString(id: Int, value: String): Unit = TODO("stub")
    fun writeInt(value: Int): Unit = TODO("stub")
    fun writeString(value: String): Unit = TODO("stub")
}

// C++: ChunkLoadClass (wwsaveload/chunkio.h)
class ChunkLoadClass {
    fun openChunk(): Boolean = TODO("stub")
    fun closeChunk(): Unit = TODO("stub")
    val curChunkId: Int get() = TODO("stub")
    fun openMicroChunk(): Boolean = TODO("stub")
    fun closeMicroChunk(): Unit = TODO("stub")
    val curMicroChunkId: Int get() = TODO("stub")
    fun readBool(): Boolean = TODO("stub")
    fun readInt(): Int = TODO("stub")
    fun readFloat(): Float = TODO("stub")
    fun readUInt(): UInt = TODO("stub")
    fun readWwString(): String = TODO("stub")
    fun readString(): String = TODO("stub")
    fun readPtr(): Any = TODO("stub")
    fun readVector3(): Vector3 = TODO("stub")
    fun readMatrix3D(): Matrix3D = TODO("stub")
    fun readSphere(): ccr.server.level.Sphere = TODO("stub")
    fun readOBBox(): ccr.server.defs.OBBoxClass = TODO("stub")
    fun skip(): Unit = TODO("stub")
    fun skipChunk(): Unit = TODO("stub")
    fun load(target: Any): Boolean = TODO("stub")
}

// ---------------------------------------------------------------------------
// Render object stubs — C++ ww3d2 render/model system
// ---------------------------------------------------------------------------

// C++: RenderObjClass (ww3d2/rendobj.h)
open class RenderObjClass {
    open fun getName(): String = TODO("stub")
    fun getNumSubObjects(): Int = TODO("stub")
    fun getSubObject(i: Int): RenderObjClass = TODO("stub")
    fun setHidden(hidden: Boolean): Unit = Unit
    fun isHidden(): Boolean = false
    fun getBoneIndex(name: String): Int = TODO("stub")
    fun getBoneTransform(boneIndex: Int): Matrix3D = TODO("stub")
    fun getNumBones(): Int = TODO("stub")
    fun getBoneName(boneIndex: Int): String = TODO("stub")
    fun getSubObjectBoneIndex(subObj: RenderObjClass?): Int = TODO("stub")
    fun getNumSubObjectsOnBone(boneIndex: Int): Int = TODO("stub")
    fun getSubObjectOnBone(boneIndex: Int, index: Int): RenderObjClass = TODO("stub")
    fun addSubObjectToBone(subObj: RenderObjClass, boneIndex: Int): Unit = Unit
    fun addSubObjectToBone(subObj: RenderObjClass, boneName: String): Unit = Unit
    fun removeSubObject(subObj: RenderObjClass): Unit = TODO("stub")
    fun captureBone(boneIndex: Int): Unit = TODO("stub")
    fun releaseBone(boneIndex: Int): Unit = TODO("stub")
    fun isBoneCaptured(boneIndex: Int): Boolean = TODO("stub")
    fun setParentTransform(tm: Matrix3D): Unit = TODO("stub")
    fun getOrientation(): ccr.math.Quaternion = TODO("stub")
    fun getHTree(): HTreeClass? = null
    // As_* casts
    open fun asMeshClass(): MeshClass? = null
    open fun asAnimatable3DObjClass(): Animatable3DObjClass? = null

    open val classId: Int get() = 0

    companion object {
        const val ANIM_MODE_LOOP   = 0
        const val ANIM_MODE_ONCE   = 1
        const val ANIM_MODE_STOP   = 2
        const val ANIM_MODE_TARGET = 3
        const val ANIM_MODE_LOOP_ONCE = 4
        const val ANIM_MODE_MANUAL = 5
        const val CLASSID_MESH = 1
    }
}

// C++: MeshClass : public RenderObjClass
open class MeshClass : RenderObjClass() {
    override val classId: Int get() = RenderObjClass.CLASSID_MESH
    override fun asMeshClass(): MeshClass = this
    fun getVertexCount(): Int = TODO("stub")
    // Returns array of vertex positions — each element is a Vector3
    fun getVertexArray(): Array<Vector3> = TODO("stub")
    // Returns array of triangles — each element is an IntArray of 3 vertex indices
    fun getPolygonArray(): Array<IntArray> = TODO("stub")
    fun getModel(): MeshClass = TODO("stub")  // returns the underlying model
    fun enableAlternateMaterialDescription(enable: Boolean): Unit = TODO("stub")
    fun peekModel(): MeshClass? = TODO("stub")
}

// C++: Animatable3DObjClass : public RenderObjClass
open class Animatable3DObjClass : RenderObjClass() {
    override fun asAnimatable3DObjClass(): Animatable3DObjClass = this
    fun setAnimation(animName: String, mode: Int = 0, frame: Float = 0f): Unit = TODO("stub")
    fun setAnimation(anim: HAnimClass, frame: Float, mode: Int): Unit = Unit
    fun getAnimationName(): String = TODO("stub")
    fun getNumFrames(): Int = TODO("stub")
    fun isLoaded(): Boolean = TODO("stub")
    fun getTotalTime(): Float = TODO("stub")
}

// C++: void Get_Render_Obj_Name_From_Filename(StringClass&, const char*) — strips path and extension
// Server stub: server has no asset manager; just strips extension and path from the filename
fun getRenderObjNameFromFilename(filename: String): String {
    val withoutPath = filename.substringAfterLast('/').substringAfterLast('\\')
    val withoutExt = withoutPath.substringBeforeLast('.')
    return withoutExt
}

// C++: RenderObjClass* Create_Render_Obj_From_Filename(const char*) — loads a render object from file
// Server stub: server has no renderer; always returns null
fun createRenderObjFromFilename(@Suppress("UNUSED_PARAMETER") filename: String): RenderObjClass? = null

// ---------------------------------------------------------------------------
// Physics class stubs — C++ wwphys
// ---------------------------------------------------------------------------

// C++: CollisionEventClass (wwphys/phys.h)
class CollisionEventClass {
    val otherObj: PhysClass? = null
    val collidedRenderObj: RenderObjClass? = null
    val collisionResult: CastResultStruct? = null
}

// C++: CastResultStruct (wwphys/castres.h)
class CastResultStruct {
    val surfaceType: Int = 0
    val fraction: Float = 0f
    val normal: Vector3 = Vector3()
    val startInside: Boolean = false
}

// C++: PhysClass server stub — extends the real ccr.physics.PhysClass so that server-stub
// instances (HumanPhysClass bridge, PhysDefClass-created stubs) can be stored in physObj
// (typed as ccr.physics.PhysClass) alongside real physics objects (VehiclePhysClass, etc.).
//
// Methods that are purely server-specific (peekModel, setObserver, getObserver, getFactory,
// intersectionTest, addToScene, removeFromScene) remain here.  All other methods are
// inherited from the real ccr.physics.PhysClass.
open class PhysClass : RealPhysClass() {
    // abstract var transform is satisfied by DynamicPhysClass in the real hierarchy; here we
    // provide a concrete backing field so the server-stub instances are instantiable.
    private var _transform: Matrix3D = Matrix3D.IDENTITY
    override var transform: Matrix3D
        get() = _transform
        set(v) { _transform = v }

    // Server-specific observer storage.  The real ccr.physics.PhysClass.observer is typed as
    // ccr.physics.PhysObserver which is incompatible with the server's CombatPhysObserverClass
    // interface (different method signatures / parameter types).  We keep a parallel field.
    var combatObserver: CombatPhysObserverClass? = null

    // Server-specific methods — not present in ccr.physics.PhysClass (would need server types).
    override fun peekModel(): RenderObjClass? = model as? RenderObjClass
    fun setObserver(observer: CombatPhysObserverClass?) { combatObserver = observer }
    fun getObserver(): PhysObserverClass? = combatObserver
    fun getFactory(): PhysFactoryClass? = null
    fun intersectionTest(test: PhysAABoxIntersectionTestClass): Boolean = false
    open fun addToScene(scene: Any): Unit = Unit
    open fun removeFromScene(): Unit = Unit

    // asVehiclePhysClass() is inherited from the real PhysClass with the same return type — no override needed.
    // The stub-specific As_* methods that conflict in return type are intentionally omitted:
    // callers should use the real As_*() returns (ccr.physics.*) or the peekHumanPhys()/etc helpers.
    open fun asSound3DClass(): Sound3DClass? = null
}

// C++: MoveablePhysClass : public PhysClass
open class MoveablePhysClass : PhysClass() {
    open fun setVelocity(vel: Vector3): Unit = Unit
    open fun getVelocity(): Vector3 = Vector3()
    open fun getAngularVelocity(): Vector3 = Vector3()
    open fun cinematicMoveTo(tm: Matrix3D): Unit = Unit
    override fun addEffectToMe(effect: Any): Unit = Unit
    open fun removeEffectFromMe(effect: Any): Unit = Unit
    open fun computeApproximateRideHeight(): Float = 0f
    open fun getSquishVelocity(): Float = 0f
    open fun setGravityMultiplier(multiplier: Float): Unit = Unit
    override fun enableUserControl(enable: Boolean): Unit = Unit
}

// C++: StaticPhysClass : public PhysClass
open class StaticPhysClass : PhysClass() {
    fun getStaticLights(): List<Any> = TODO("stub")
    fun getStaticObjects(): List<Any> = TODO("stub")
    fun enableIsStateDirty(enable: Boolean): Unit {}
    // C++: Get_Factory() returns PhysFactoryClass; factory->Get_Chunk_ID() checked against known IDs
    fun isFromFactory(chunkId: UInt): Boolean = TODO("stub")
    fun isFromFactory(chunkId: Int): Boolean = isFromFactory(chunkId.toUInt())
    open fun getName(): String = TODO("stub")
}

// C++: StaticAnimPhysClass : public StaticPhysClass
open class StaticAnimPhysClass : StaticPhysClass() {
    // asStaticAnimPhysClass() intentionally not overridden (return type clash with real hierarchy).
    fun getAnimState(): Int = TODO("stub")
    fun setCurrentState(state: Int): Unit = TODO("stub")
    fun getStateFlag(flag: Int): Boolean = TODO("stub")
    fun toggleStateFlag(flag: Int): Unit = TODO("stub")
    fun getStateTimer(): Float = TODO("stub")
    fun setStateTimer(timer: Float): Unit = TODO("stub")
    fun isTriggered(): Boolean = TODO("stub")
    fun getStateName(): String = TODO("stub")
    fun peekAnimation(): Animatable3DObjClass? = TODO("stub")
    fun getAnimationManager(): AnimCollisionManagerClass = TODO("stub")
    fun getCollisionBox(): Any? = TODO("stub")
    fun enableRemoveOnComplete(enable: Boolean): Unit = TODO("stub")
    // C++: DefinitionClass* Get_Definition() / Get_ID() / Get_Position(Vector3&)
    fun getDefinition(): DefinitionClass? = TODO("stub")
    fun getId(): Int = TODO("stub")
    fun getPosition(out: Vector3): Unit = TODO("stub")
}

// C++: HumanPhysClass : public MoveablePhysClass
open class HumanPhysClass : MoveablePhysClass() {
    // asHumanPhysClass() intentionally not overridden (return type clash with real hierarchy).
    open fun getLegMode(): Boolean = false
    open fun setLadderIndex(index: Int): Unit = Unit
    open fun isSubStateAdjustable(): Boolean = false
    open fun getHeading(): Float = 0f
    open fun setHeading(heading: Float): Unit = Unit
    fun setInContact(inContact: Boolean): Unit = Unit
    fun setLoitersAllowed(allowed: Boolean): Unit = Unit
    fun resetLoiterDelay(): Unit = Unit
    fun canTeleport(pos: Vector3): Boolean = false
    fun findTeleportLocation(pos: Vector3, radius: Float, out: Vector3): Boolean = false
    fun setDisabled(disabled: Boolean): Unit = Unit
    fun isEngineEnabled(): Boolean = false
    fun enableEngine(enable: Boolean): Unit = Unit
}

// C++: ProjectileClass (physics stub)
open class ProjectilePhysClass : MoveablePhysClass() {
    // asProjectileClass() intentionally not overridden (return type clash with real hierarchy).
}

// C++: Sound3DClass physics stub
open class Sound3DClass {
    fun asSound3DClass(): Sound3DClass = this
    fun setMaxVolRadius(radius: Float): Unit = TODO("stub")
    fun isSoundCulled(): Boolean = TODO("stub")
    fun isPlaying(): Boolean = TODO("stub")
    fun play(): Unit = TODO("stub")
    fun stop(): Unit = TODO("stub")
}

// C++: LightPhysClass : public StaticPhysClass
open class LightPhysClass : StaticPhysClass() {
    // asLightPhysClass() intentionally not overridden (return type clash with real hierarchy).
    fun setDisabled(disabled: Boolean): Unit = Unit
    fun getGroupId(): Int = TODO("stub")
    override fun getName(): String = TODO("stub")
    override fun getPosition(): Vector3 = TODO("stub")
}

// C++: PhysFactoryClass — factory with chunkId for type identification
class PhysFactoryClass {
    val chunkId: UInt = 0u
}

// ---------------------------------------------------------------------------
// Physics def stubs
// ---------------------------------------------------------------------------

// C++: PhysDefClass base (server stub)
abstract class PhysDefClass {
    abstract fun create(): PhysClass
}

// ---------------------------------------------------------------------------
// Animation controller stubs
// ---------------------------------------------------------------------------

// C++: AnimControlClass (ww3d2/animobj.h)
open class AnimControlClass {
    fun setModel(model: RenderObjClass?): Unit = Unit
    fun setAnimation(name: String, vararg args: Any): Unit = Unit
    fun setMode(mode: Int, vararg args: Any): Unit = Unit
    fun setTargetFrame(frame: Int): Unit = Unit
    fun update(dt: Float): Unit = Unit
    fun isComplete(): Boolean = false
    fun getAnimationName(): String = ""
    fun getCurrentFrame(): Int = 0
    fun getTargetFrame(): Int = 0
    fun getMode(): Int = 0
    fun save(csave: ChunkSaveClass): Unit = Unit
    fun load(cload: ChunkLoadClass): Unit = Unit
    fun peekCarrierObject(): Any? = null
    fun peekAnimation(): Animatable3DObjClass? = null
    fun peekModel(): RenderObjClass? = null
    fun resetAnimUpdate(): Unit = Unit
}

// C++: SimpleAnimControlClass : public AnimControlClass
class SimpleAnimControlClass : AnimControlClass()

// C++: HumanAnimControlClass : public AnimControlClass
class HumanAnimControlClass : AnimControlClass() {
    fun setHumanAnimOverride(animName: String, weight: Float): Unit = TODO("stub")
    fun setHumanLoiterCollection(collection: Any?): Unit = TODO("stub")
    fun generateIdleAnimation(): Unit = TODO("stub")
    fun isModelUpdateNeeded(): Boolean = TODO("stub")
    fun getAnimState(): Int = TODO("stub")
    fun setAnimControl(animControl: AnimControlClass?): Unit = TODO("stub")
    fun startScriptedAnimation(animName: String, looping: Boolean): Unit = TODO("stub")
    fun stopScriptedAnimation(): Unit = TODO("stub")
    fun startTransitionAnimation(dataId: Int): Unit = TODO("stub")
    fun forceAnimation(animName: String): Unit = TODO("stub")
    fun isBusy(): Boolean = TODO("stub")
    fun isInterruptable(): Boolean = TODO("stub")
    fun getLegMode(): Boolean = TODO("stub")
}

// C++: AnimCollisionManagerClass — manages animation/collision interaction
class AnimCollisionManagerClass {
    fun networkStateUpdate(animControl: AnimControlClass?): Unit = TODO("stub")
    fun networkInterpolateStateUpdate(animControl: AnimControlClass?): Unit = TODO("stub")
    fun setAnimationMode(mode: Int): Unit = TODO("stub")
    fun setTargetFrame(frame: Float): Unit = TODO("stub")
    fun setTargetFrame(frame: Int): Unit = setTargetFrame(frame.toFloat())
    fun getTargetFrame(): Float = TODO("stub")
    fun setTargetFrameEnd(): Unit = TODO("stub")
    // C++: AnimCollisionManagerClass::Get_Animation() — returns current animation asset
    fun peekAnimation(): HAnimClass? = null

    companion object {
        const val ANIMATE_TARGET = 3  // same as ANIM_MODE_TARGET
    }
}

// ---------------------------------------------------------------------------
// Physics scene stub (C++ server-side, NOT ccr.physics.scene.PhysicsScene)
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Human state / soldier animation stubs
// ---------------------------------------------------------------------------

<<<<<<< HEAD
// C++: HumanStateClass (soldier.h)
class HumanStateClass {
    private var _state: Int = UPRIGHT
    private var _subState: Int = 0
    private var _stateTimer: Float = 0f

    fun setAnimControl(animControl: HumanAnimControlClass?): Unit = Unit
    fun getState(): Int = _state
    // C++: void Set_State(StateType state, int sub_state = 0)
    fun setState(state: Int, subState: Int = 0): Unit { _state = state; _subState = subState }
    fun getSubState(): Int = _subState
    fun getStateName(): String = ""
    fun getLegFacing(): Float = 0f
    fun isFiring(): Boolean = false
    fun isReloading(): Boolean = false
    fun getStateTimer(): Float = _stateTimer
    fun setStateTimer(timer: Float): Unit { _stateTimer = timer }
    fun reset(): Unit { _state = UPRIGHT; _subState = 0; _stateTimer = 0f }
    fun postThink(): Unit = Unit
    fun setTurnVelocity(velocity: Float): Unit = Unit
    fun updateAiming(tilt: Float, roll: Float): Unit = Unit
    fun updateWeapon(weapon: WeaponClass?, weaponChanged: Boolean): Unit = Unit
    fun updateState(): Unit = Unit
    fun startScriptedAnimation(animName: String, looping: Boolean, resetOnComplete: Boolean = false): Unit = Unit
    fun forceAnimation(animName: String, looping: Boolean = false): Unit = Unit
    fun save(csave: ChunkSaveClass): Unit = Unit
    fun load(cload: ChunkLoadClass): Unit = Unit
    fun getInformation(sb: StringBuilder): Unit = Unit
    fun init(humanPhys: HumanPhysClass?): Unit = Unit
    // C++: HumanStateClass::Set_Human_Anim_Override(int defId)
    fun setHumanAnimOverride(defId: Int): Unit = Unit
    // C++: HumanStateClass::Set_Human_Loiter_Collection(int defId)
    fun setHumanLoiterCollection(defId: Int): Unit = Unit
    // C++: HumanStateClass::Is_Sub_State_Adjustable()
    fun isSubStateAdjustable(): Boolean = false
    // C++: HumanStateClass::Get_State_Flag(int flag)
    fun getStateFlag(flag: Int): Boolean = false
    // C++: HumanStateClass::Toggle_State_Flag(int flag)
    fun toggleStateFlag(flag: Int): Unit = Unit
    // C++: HumanStateClass::Get_Can_Snipe()
    fun getCanSnipe(): Boolean = false
    // C++: HumanStateClass::Get_Normalized_Speed()
    fun getNormalizedSpeed(): Float = 0f
    // C++: HumanStateClass::Set_Normalized_Speed(float)
    fun setNormalizedSpeed(speed: Float): Unit = Unit
    // C++: HumanStateClass::Get_Leg_Mode()
    fun getLegMode(): Boolean = false
    // C++: HumanStateClass::Is_Locked()
    fun isLocked(): Boolean = false
    // C++: HumanStateClass::Reset_Loiter_Delay()
    fun resetLoiterDelay(): Unit = Unit
    // C++: HumanStateClass::Set_Loiters_Allowed(bool)
    fun setLoitersAllowed(allowed: Boolean): Unit = Unit
    // C++: HumanStateClass::Start_Transition_Animation(int dataId)
    fun startTransitionAnimation(dataId: Int): Unit = Unit
    // C++: HumanStateClass::Stop_Scripted_Animation()
    fun stopScriptedAnimation(): Unit = Unit
    // C++: HumanStateClass::Get_Collision_Box()
    fun getCollisionBox(): AABoxClass? = null
    // C++: HumanStateClass::Network_State_Update(AnimControlClass*)
    fun networkStateUpdate(animControl: AnimControlClass?): Unit = Unit
    // C++: HumanStateClass::Get_Information(StringClass&) — for getInformation() on HumanStateClass obj directly
    // (already added above for the StringBuilder overload)
    // C++: HumanStateClass::Stop_Conversation()
    fun stopConversation(): Unit = Unit

    companion object {
        const val UPRIGHT   = 0
        const val CROUCH    = 1
        const val DEATH     = 2
        const val IN_VEHICLE = 3
        const val OUCH_FIRE = 4
        const val LADDER    = 5
    }
}
=======
// NOTE: HumanStateClass is defined in HumanStateClass.kt — not a stub.
>>>>>>> f7bb8f8 (feat: port HumanStateClass from C++ — full 1:1 migration)

// C++: TransitionDataClass — data for vehicle/object transition animations
class TransitionDataClass {
    val duration: Float = 0f
    val totalTime: Float = 0f
    fun isFromFactory(): Boolean = TODO("stub")
    fun getStyle(): Int = TODO("stub")

    companion object {
        // C++: TransitionDataClass::StyleType enum values
        const val VEHICLE_ENTER      = 0
        const val VEHICLE_EXIT       = 1
        const val LADDER_ENTER_TOP   = 2
        const val LADDER_ENTER_BOTTOM = 3
        const val LADDER_EXIT_TOP    = 4
        const val LADDER_EXIT_BOTTOM = 5
    }
}

// C++: TransitionCompletionDataStruct
class TransitionCompletionDataStruct {
    val vehicleId: Int = 0
    val seatIndex: Int = 0
    val entering: Boolean = false
    // C++: int Type (transition style)
    val type: Int = 0
    // C++: ReferencerClass Vehicle (weak reference to the vehicle)
    val vehicle: GameObjReference = GameObjReference()
}

// C++: TransitionEffectClass — visual effect during state transitions
// Shares material-effect interface with MaterialEffectClass
open class TransitionEffectClass {
    fun start(): Unit = TODO("stub")
    fun stop(): Unit = TODO("stub")
    fun isActive(): Boolean = TODO("stub")
    fun setTransitionTime(t: Float): Unit = Unit
    fun setTargetParameter(v: Float): Unit = Unit
    fun getTargetParameter(): Float = 0f
    fun getParameter(): Float = 0f
    fun enableRemoveOnComplete(enable: Boolean): Unit = Unit
}

// C++: TransitionInstanceClass — instance of a transition (e.g. enter/exit vehicle)
class TransitionInstanceClass(val data: ccr.server.defs.TransitionDataClass? = null) {
    fun setParentTransform(tm: Matrix3D): Unit {}
    fun setLadderIndex(index: Int): Unit {}

    companion object {
        // C++: TransitionInstanceClass::End(SoldierGameObj*, TransitionCompletionDataStruct*)
        fun end(soldier: SoldierGameObj, data: TransitionCompletionDataStruct): Unit = Unit
    }
}

// C++: TransitionManager singleton
object TransitionManager {
    fun findTransition(vehicleObj: Any?, seatIndex: Int, entering: Boolean): TransitionDataClass? = null
    fun getTransitionDuration(vehicleObj: Any?, seatIndex: Int, entering: Boolean): Float = 0f
    fun add(trans: TransitionInstanceClass): Unit {}
    fun destroy(trans: TransitionInstanceClass): Unit {}
    fun destroyPending(): Unit {}
}

// C++: DialogueClass — single dialogue entry for NPC speech
class DialogueClass {
    var soundId: Int = 0
    var delay: Float = 0f
    var loop: Boolean = false

    fun save(csave: ChunkSaveClass): Unit = TODO("stub")
    fun load(cload: ChunkLoadClass): Unit = TODO("stub")

    companion object {
        fun fromData(data: Any?): DialogueClass = DialogueClass()
    }
}

// C++: DynamicSpeechAnimClass — animated speech/emote on soldier
class DynamicSpeechAnimClass(skeletonName: String = "") {
    fun update(dt: Float): Unit = TODO("stub")
    fun isActive(): Boolean = TODO("stub")
    fun generateIdleAnimation(param1: Int, param2: Float): Unit = Unit
}

// C++: AudibleSoundClass — playing sound instance
class AudibleSoundClass {
    fun stop(): Unit = Unit
    fun play(): Unit = Unit
    fun isPlaying(): Boolean = false
    fun isSoundCulled(): Boolean = false
    fun asSound3DClass(): Sound3DClass? = null
    fun getDuration(): Float = 0f
    fun setDropOffRadius(radius: Float): Unit = Unit
    fun setTransform(center: Vector3): Unit = Unit
    fun setTransform(tm: Matrix3D): Unit = Unit
    fun addToScene(): Unit = Unit
    fun addToScene(updateNow: Boolean): Unit = Unit
    fun removeFromScene(): Unit = Unit
    fun getString(): String? = null
    fun getSoundId(): Int = 0
}

// C++: PersistantSurfaceEmitterClass — particle emitter that persists on a surface
class PersistantSurfaceEmitterClass {
    fun update(pos: Vector3, type: Int): Unit = TODO("stub")
    fun setActive(active: Boolean): Unit = TODO("stub")
}

// ---------------------------------------------------------------------------
// Observer / script stubs
// ---------------------------------------------------------------------------

// C++: GameObjObserverClass (observer.h)
open class GameObjObserverClass {
    open fun getId(): Int = 0
    open fun getName(): String = ""
    open fun attach(obj: ScriptableGameObj): Unit {}
    open fun detach(obj: ScriptableGameObj): Unit {}
    open fun created(obj: ScriptableGameObj): Unit {}
    open fun destroyed(obj: ScriptableGameObj): Unit {}
    open fun damaged(obj: ScriptableGameObj, damager: Any?, amount: Float): Unit {}
    open fun killed(obj: ScriptableGameObj, killer: Any?): Unit {}
    open fun timerExpired(obj: ScriptableGameObj, timerId: Int): Unit {}
    open fun custom(obj: ScriptableGameObj, type: Int, param: Int, sender: ScriptableGameObj?): Unit {}
    open fun animationComplete(obj: ScriptableGameObj, animName: String): Unit {}
    open fun setParametersString(params: String): Unit {}
    open fun enemySeen(obj: ScriptableGameObj, enemy: ScriptableGameObj?): Unit {}
    open fun soundHeard(obj: ScriptableGameObj, sound: Any?): Unit {}
    open fun entered(obj: ScriptableGameObj, enterer: ScriptableGameObj?): Unit {}
    open fun exited(obj: ScriptableGameObj, exiter: ScriptableGameObj?): Unit {}
    open fun onBuildingDamaged(obj: ScriptableGameObj, damager: Any?, amount: Float): Unit {}
    open fun onVehicleDelivered(obj: ScriptableGameObj): Unit {}
    open fun poked(obj: ScriptableGameObj, poker: ScriptableGameObj): Unit {}
    open fun onVehicleGenerated(obj: ScriptableGameObj): Unit {}
    fun save(csave: ChunkSaveClass): Unit = TODO("stub")
    fun load(cload: ChunkLoadClass): Unit = TODO("stub")
}

// C++: SoldierObserverClass : public GameObjObserverClass (observer.h)
open class SoldierObserverClass : GameObjObserverClass()

// C++: BuildingMonitorClass : public GameObjObserverClass
class BuildingMonitorClass(private val building: Any?) : GameObjObserverClass() {
    fun onBuildingDamaged(b: BuildingGameObj): Unit = TODO("stub")
}

// C++: ActiveConversationClass — running NPC conversation
class ActiveConversationClass {
    fun stop(): Unit = TODO("stub")
    fun isActive(): Boolean = TODO("stub")
}

// C++: GameObjObserverTimerClass — countdown timer for an observer
class GameObjObserverTimerClass(
    val observerId: Int = 0,
    var duration: Float = 0f,
    val timerId: Int = 0,
) {
    fun update(): Boolean = TODO("stub")
    fun save(csave: ChunkSaveClass): Unit = TODO("stub")
    fun load(cload: ChunkLoadClass): Unit = TODO("stub")
}

// C++: GameObjCustomTimerClass — custom timer (fires custom() on expiry)
class GameObjCustomTimerClass(
    val sender: ScriptableGameObj? = null,
    var delay: Float = 0f,
    val type: Int = 0,
    val param: Int = 0,
) {
    fun update(): Boolean = TODO("stub")
    fun save(csave: ChunkSaveClass): Unit = TODO("stub")
    fun load(cload: ChunkLoadClass): Unit = TODO("stub")
}

// ---------------------------------------------------------------------------
// Singleton managers
// ---------------------------------------------------------------------------

// C++: CombatManager (combatmanager.h)
object CombatManager {
    fun areObserversActive(): Boolean = true
    fun getScene(): PhysicsScene? = null
    fun canDamage(attacker: Any?, target: Any?): Boolean = true
    fun getTheStar(): DamageableGameObj? = null
    fun getCombatStar(): SoldierGameObj? = null
    fun getDifficultyLevel(): Int = 1
    fun onSoldierDeath(soldier: SoldierGameObj): Unit = Unit
    fun isGameplayPermitted(): Boolean = true
    fun getGlobalSettings(): GlobalSettingsDefClass = GlobalSettingsDefClass
    fun isSkeletonSliderDemoEnabled(): Boolean = false
    fun getMessageWindow(): MessageWindowClass = MessageWindowClass()
}

// C++: ScriptManager (scriptmanager.h)
object ScriptManager {
    fun createScript(name: String): GameObjObserverClass? = null
}

// C++: SurfaceEffectsManager (surfaceeffects.h)
object SurfaceEffectsManager {
    const val HITTER_TYPE_BULLET        = 0
    const val HITTER_TYPE_FOOTSTEP_RUN  = 1
    const val HITTER_TYPE_FOOTSTEP_WALK = 2
    const val HITTER_TYPE_FOOTSTEP_CROUCHED = 3

    fun applyEffect(surfaceType: Int, hitterType: Int, tm: Matrix3D,
                    emitter: Any? = null, owner: Any? = null,
                    allowSound: Boolean = true, allowEmitters: Boolean = true): Unit = Unit
    fun applyDamage(surfaceType: Int, owner: Any?): Unit = Unit
    fun isSurfacePermeable(surfaceType: Int): Boolean = false
    fun createPersistantEmitter(): PersistantSurfaceEmitterClass? = null
    fun destroyPersistantEmitter(emitter: PersistantSurfaceEmitterClass): Unit = Unit
    fun updatePersistantEmitter(emitter: PersistantSurfaceEmitterClass?,
                                pos: Vector3, type: Int): Unit = Unit
}

// C++: RadarManager (radarmanager.h)
object RadarManager {
    const val BLIP_COLOR_TYPE_NOD      = 0
    const val BLIP_COLOR_TYPE_GDI      = 1
    const val BLIP_COLOR_TYPE_MUTANT   = 2
    const val BLIP_COLOR_TYPE_RENEGADE = 3
    const val BLIP_COLOR_TYPE_NEUTRAL  = 4
}

// C++: ExplosionManager (explosion.h)
object ExplosionManager {
    fun createExplosionAt(defId: Int, tm: Matrix3D, owner: Any?): Unit = Unit
    fun createExplosionAt(defId: Int, pos: Vector3, owner: Any?): Unit = Unit
    // C++: Server_Explode overloads
    fun serverExplode(defId: Int, pos: Vector3, ownerNetId: Int): Unit = Unit
    fun serverExplode(defId: Int, pos: Vector3, ownerNetId: Int, forceVictim: DamageableGameObj?): Unit = Unit
    fun serverExplode(defId: Int, pos: Vector3, owner: Any?): Unit = Unit
    // C++: Explosion_Damage_Building
    fun explosionDamageBuilding(defId: Int, building: BuildingGameObj, mctDamage: Boolean, owner: SoldierGameObj?): Unit = Unit
}

// C++: DefinitionMgrClass (wwsaveload/definitionmgr.h) — server-side registry
object DefinitionMgrClass {
    fun findDefinition(id: Int): DefinitionClass? = null
    fun findTypedDefinition(name: String, classId: Int): Any? = null
    fun findAmmoDefinition(id: Int): Any? = null
    fun findAmmoDefinitionByName(name: String): Any? = null
    fun getFirst(classId: Int): Any? = null
    fun getNext(def: Any?, classId: Int): Any? = null
}

// C++: ObjectLibraryManager (objectlibrary.h)
object ObjectLibraryManager {
    fun findObject(name: String): Any? = null
    fun createObject(defId: Int): ScriptableGameObj? = null
}

// C++: ConversationMgrClass (conversationmgr.h)
object ConversationMgrClass {
    fun findConversation(name: String): Any? = null
    fun startConversation(convId: Int, obj1: Any?, obj2: Any?): ActiveConversationClass? = null
}

// C++: MapMgrClass (mapmgr.h)
object MapMgrClass {
    fun findBase(playerType: Int): Any? = null
    fun getBeaconZone(playerType: Int): Any? = null
    fun areVtolVehiclesEnabled(): Boolean = true
}

// C++: BonesManager
object BonesManager {
    fun findBone(boneId: Int): Any? = null
}

// C++: CombatMaterialEffectManager
object CombatMaterialEffectManager {
    fun getSpawnEffect(): MaterialEffectClass? = null
    fun getDeathEffect(): MaterialEffectClass = MaterialEffectClass()
}

// C++: TDBObjClass — translate DB object returned by TranslateDBClass::Find_Object
class TDBObjClass {
    fun getString(): String? = null
    fun getSoundId(): Int = 0
}

// C++: TranslateDBClass (translatedb.h)
object TranslateDBClass {
    fun getTranslation(id: Int): String = ""
    fun findObject(id: Int): TDBObjClass? = null
    fun getString(id: Int): String? = null
}

// C++: UnitCoordinationZoneMgr
object UnitCoordinationZoneMgr {
    fun registerUnit(obj: Any?): Unit = Unit
    fun unregisterUnit(obj: Any?): Unit = Unit
    fun isUnitInZone(pos: Vector3): Boolean = false
}

// C++: WWAudioClass (wwaudio.h)
object WWAudioClass {
    fun getInstance(): WWAudioClass = this
    fun playSound(soundId: Int, position: Vector3?): AudibleSoundClass? = null
    fun createInstantSound(soundId: Int, tm: Matrix3D, owner: Any?): AudibleSoundClass? = null
    fun createInstantSound(soundId: Int, tm: Matrix3D): AudibleSoundClass? = null
    fun createSound(soundId: Int): AudibleSoundClass? = null
    fun createSound(name: String): AudibleSoundClass? = null
    fun createContinuousSound(soundId: Int): AudibleSoundClass? = null
}

// C++: WWMath utilities
object WWMath {
    fun clamp(v: Float, min: Float, max: Float): Float = v.coerceIn(min, max)
}

// C++: SaveLoadSystemClass (wwsaveload/saveload.h)
object SaveLoadSystemClass {
    fun registerPointer(oldPtr: Any?, newPtr: Any?): Unit = Unit
    fun registerPostLoadCallback(obj: Any?): Unit = Unit
    fun findPersistFactory(chunkId: Int): PersistFactoryClass? = null
}

// C++: WeaponManager (weaponmanager.h)
object WeaponManager {
    fun findWeapon(name: String): ccr.server.defs.WeaponDefinitionClass? = null
    fun findAmmoDefinition(id: Int): ccr.server.defs.AmmoDefinitionClass? = null
    fun findAmmoDefinitionByName(name: String): ccr.server.defs.AmmoDefinitionClass? = null
}

// C++: HUDInfo (hudinfo.h) — HUD display stub
object HUDInfo {
    fun getInformation(obj: Any?): String = ""
}

// C++: FreeRandom — global random number generator
object FreeRandom {
    fun get(): Float = kotlin.random.Random.nextFloat()
    fun getInt(max: Int): Int = kotlin.random.Random.nextInt(max)
}

// C++: GlobalSettingsDefClass instance returned by GlobalSettingsDef.getGlobalSettings()
object GlobalSettingsInstance {
    fun getMpStealthDistanceVehicle(): Float = 300f
    fun getMpStealthDistanceHuman(): Float = 150f
    fun getSoldierCrouchSpeed(): Float = 0.5f
    fun getSoldierWalkSpeed(): Float = 0.5f
    fun getDeathSoundId(): Int = 0
}

// C++: GlobalSettingsDef
object GlobalSettingsDef {
    val encounteredEnemyTimeout: Float = 5f
    val stealthPowerupDuration: Float = 30f
    val repairMultiplier: Float = 1f
    val maxTimeSinceEnemySeen: Float = 30f

    // C++: CNCModeSettingsDef::Get_Global_Settings() / GlobalSettingsDefClass::Get_Instance()
    fun getGlobalSettings(): GlobalSettingsInstance = GlobalSettingsInstance
}

// C++: HarvesterClass — stub for refinery harvester vehicle
class HarvesterClass : GameObjObserverClass() {
    var playerType: Int = 0
    fun goHarvest(target: Any? = null): Unit = TODO("stub")
    fun setDropOffRadius(radius: Float): Unit = TODO("stub")
    fun addMoney(amount: Float): Unit = TODO("stub")
    fun getVehicle(): VehicleGameObj? = TODO("stub")
    fun setRefinery(building: BuildingGameObj?): Unit = TODO("stub")
    fun setDockLocation(pos: Vector3): Unit = TODO("stub")
    fun setDockEntrance(pos: Vector3): Unit = TODO("stub")
    fun setHarvestAnim(name: String): Unit = TODO("stub")
    fun initialize(): Unit = TODO("stub")
    fun think(): Unit = TODO("stub")
}

// C++: PathfindClass — path finding
class PathfindClass {
    fun goto(target: Vector3): Unit = TODO("stub")
    fun attack(target: Any?, range: Float): Unit = TODO("stub")
    fun attackObject(target: Any?, range: Float): Unit = TODO("stub")
    fun stop(): Unit = TODO("stub")
    fun reset(): Unit = TODO("stub")
    fun getWaypathStartingInBox(box: AABoxClass, lastIndex: Int): WaypathClass? = null
    fun countWaypathsStartingInBox(box: AABoxClass): Int = 1

    companion object {
        fun getInstance(): PathfindClass? = null
    }
}

// C++: NonRefPhysListClass — unowned list of physics objects (iterable via delegation)
class NonRefPhysListClass : MutableList<PhysClass> by mutableListOf()

// C++: AABoxClass — axis-aligned bounding box
class AABoxClass(val center: Vector3 = Vector3(), val extent: Vector3 = Vector3()) {
    fun toAABox(): AABoxClass = this
}

// C++: WaypathClass — waypath for vehicle delivery routing
class WaypathClass {
    fun getId(): Int = 0
}

// C++: OBBoxClass::toAABox — convert oriented box to axis-aligned box
fun ccr.server.defs.OBBoxClass.toAABox(): AABoxClass = AABoxClass(center, extent)

// C++: LineSegClass — line segment
class LineSegClass(val start: Vector3 = Vector3(), val end: Vector3 = Vector3())

// C++: PhysRayCollisionTestClass — ray vs physics scene test
class PhysRayCollisionTestClass(
    val ray: LineSegClass = LineSegClass(),
    val collisionGroup: Int = 0,
) {
    // C++: PhysRayCollisionTestClass(ray, result, collisionGroup, collisionType)
    constructor(ray: LineSegClass, result: CastResultStruct, collisionGroup: Int, collisionType: Int) :
        this(ray, collisionGroup)

    val result: CastResultStruct = CastResultStruct()
    val hitObject: PhysClass? = null
    val collidedPhysObj: PhysClass? = null
}

// C++: PhysAABoxIntersectionTestClass — AABB vs physics scene test
class PhysAABoxIntersectionTestClass(
    val box: AABoxClass = AABoxClass(),
    val collisionGroup: Int = 0,
    val collisionType: Int = 0,
)

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val DEFAULT_COLLISION_GROUP        = 0
const val TERRAIN_AND_BULLET_COLLISION_GROUP = 1
const val TERRAIN_ONLY_COLLISION_GROUP   = 2
const val UNCOLLIDEABLE_GROUP            = 3
const val COLLISION_TYPE_PHYSICAL        = 0x10
const val COLLISION_REACTION_DEFAULT     = 0


const val ANIM_MODE_LOOP   = 0
const val ANIM_MODE_ONCE   = 1
const val ANIM_MODE_STOP   = 2
const val ANIM_MODE_TARGET = 3

// C++: BitPackResolution class stub
class BitPackResolution(val bits: Int) {
    companion object {
        // C++: BITPACK_WORLD_POSITION_Z.Get_Half_Resolution() — half the Z quantization step
        fun getWorldPositionZHalfResolution(): Float = 0f
    }
}

// ---------------------------------------------------------------------------
// Building aggregate / MCT stub
// ---------------------------------------------------------------------------

// C++: BuildingAggregateClass : public StaticPhysClass
class BuildingAggregateClass : StaticPhysClass() {
    fun isMct(): Boolean = TODO("stub")
    fun isRepair(): Boolean = TODO("stub")
    fun getGroupId(): Int = TODO("stub")
    fun getDamageRadius(): Float = TODO("stub")
    fun getDamageStrength(): Float = TODO("stub")
    fun getDamageWarhead(): Int = TODO("stub")
    fun isDestroyed(): Boolean = TODO("stub")
    fun setDisabled(disabled: Boolean): Unit = TODO("stub")
    fun setCurrentState(state: Int, forceUpdate: Boolean): Unit = TODO("stub")
}

// ---------------------------------------------------------------------------
// CombatPhysObserverClass As_* extension functions
// Used in PhysicalGameObj, BuildingGameObj etc.
// ---------------------------------------------------------------------------

fun CombatPhysObserverClass.asPhysicalGameObj(): PhysicalGameObj? = this as? PhysicalGameObj
fun CombatPhysObserverClass.asBuildingGameObj(): BuildingGameObj? = this as? BuildingGameObj
fun CombatPhysObserverClass.asSmartGameObj(): SmartGameObj? = this as? SmartGameObj
fun CombatPhysObserverClass.asSoldierGameObj(): SoldierGameObj? = this as? SoldierGameObj
fun CombatPhysObserverClass.asVehicleGameObj(): VehicleGameObj? = this as? VehicleGameObj
fun CombatPhysObserverClass.asC4GameObj(): C4GameObj? = this as? C4GameObj

// ScriptableGameObj As_* helper extensions
fun ScriptableGameObj.asPhysicalGameObj(): PhysicalGameObj? = this as? PhysicalGameObj
fun ScriptableGameObj.asSmartGameObj(): SmartGameObj? = this as? SmartGameObj
fun ScriptableGameObj.asSoldierGameObj(): SoldierGameObj? = this as? SoldierGameObj
fun ScriptableGameObj.asVehicleGameObj(): VehicleGameObj? = this as? VehicleGameObj
fun ScriptableGameObj.asBuildingGameObj(): BuildingGameObj? = this as? BuildingGameObj
fun ScriptableGameObj.asC4GameObj(): C4GameObj? = this as? C4GameObj
fun ScriptableGameObj.asBeaconGameObj(): BeaconGameObj? = this as? BeaconGameObj

// PhysicalGameObj As_* helpers
fun PhysicalGameObj.asSmartGameObj(): SmartGameObj? = this as? SmartGameObj
fun PhysicalGameObj.asSoldierGameObj(): SoldierGameObj? = this as? SoldierGameObj
fun PhysicalGameObj.asVehicleGameObj(): VehicleGameObj? = this as? VehicleGameObj
fun PhysicalGameObj.asC4GameObj(): C4GameObj? = this as? C4GameObj
fun PhysicalGameObj.asBeaconGameObj(): BeaconGameObj? = this as? BeaconGameObj

// ---------------------------------------------------------------------------
// Matrix3D extension helpers used in server net code
// ---------------------------------------------------------------------------

// C++: Matrix3D::Inverse_Transform_Vector (1-param version — returns result)
fun Matrix3D.inverseTransformVector(v: Vector3): Vector3 {
    // Transpose of 3x3 rotation * (v - translation)
    val dx = v.x - m03
    val dy = v.y - m13
    val dz = v.z - m23
    return Vector3(
        m00 * dx + m10 * dy + m20 * dz,
        m01 * dx + m11 * dy + m21 * dz,
        m02 * dx + m12 * dy + m22 * dz,
    )
}

// C++: Matrix3D::Inverse_Transform_Vector (2-param version — writes result into out)
fun Matrix3D.inverseTransformVector(v: Vector3, out: Vector3) {
    val r = inverseTransformVector(v)
    out.x = r.x; out.y = r.y; out.z = r.z
}

// C++: Matrix3D::Get_X_Vector — returns first column (local X axis)
fun Matrix3D.getXVector(): Vector3 = Vector3(m00, m10, m20)

// C++: Matrix3D::Transform_Point — applies full rotation+translation to a point
fun Matrix3D.transformPoint(v: Vector3): Vector3 = Vector3(
    m00 * v.x + m01 * v.y + m02 * v.z + m03,
    m10 * v.x + m11 * v.y + m12 * v.z + m13,
    m20 * v.x + m21 * v.y + m22 * v.z + m23,
)

// C++: Matrix3D::Obj_Look_At (static factory) — returns a look-at matrix
fun Matrix3D.Companion.objLookAt(from: Vector3, to: Vector3, roll: Float): Matrix3D = IDENTITY

// C++: Matrix3D::Get_Translation
fun Matrix3D.getTranslation(): Vector3 = translation

// C++: Matrix3D::Translate
fun Matrix3D.translate(v: Vector3): Unit {
    // no-op stub — Matrix3D is a data class, so callers must reassign
}

// C++: Matrix3D::Rotate_Z
fun Matrix3D.rotateZ(angle: Float): Unit {
    // stub — callers that need actual rotation must use Matrix3D.copy()
}

// C++: Matrix3D::Rotate_Y
fun Matrix3D.rotateY(angle: Float): Unit {}

// C++: Matrix3D::Get_Y_Rotation
fun Matrix3D.getYRotation(): Float = 0f

// C++: Matrix3D::identity() factory
fun Matrix3D.Companion.identity(): Matrix3D = IDENTITY

// C++: createAnimationName — derives full animation name from model name
fun createAnimationName(animBase: String, modelName: String): String {
    val model = modelName.substringBefore('.').uppercase()
    return "$model.$animBase"
}

// Extend HumanStateClass companion with all state constants
// C++: HumanStateClass::StateType enum and sub-state flags
val HumanStateClass.Companion.UPRIGHT: Int get() = 0
val HumanStateClass.Companion.CROUCH: Int get() = 1
val HumanStateClass.Companion.DEATH: Int get() = 2
val HumanStateClass.Companion.IN_VEHICLE: Int get() = 3
val HumanStateClass.Companion.OUCH_FIRE: Int get() = 4
val HumanStateClass.Companion.LADDER: Int get() = 5
val HumanStateClass.Companion.DIVE: Int get() = 6
val HumanStateClass.Companion.TRANSITION: Int get() = 7
val HumanStateClass.Companion.ANIMATION: Int get() = 8
val HumanStateClass.Companion.ON_FIRE: Int get() = 9
val HumanStateClass.Companion.ON_CHEM: Int get() = 10
val HumanStateClass.Companion.ON_CNC_FIRE: Int get() = 11
val HumanStateClass.Companion.ON_CNC_CHEM: Int get() = 12
val HumanStateClass.Companion.ON_ELECTRIC: Int get() = 13
val HumanStateClass.Companion.DESTROY: Int get() = 14
val HumanStateClass.Companion.TRANSITION_COMPLETE: Int get() = 15
val HumanStateClass.Companion.AIRBORNE: Int get() = 16
val HumanStateClass.Companion.SNIPING: Int get() = 17
val HumanStateClass.Companion.WOUNDED: Int get() = 18
val HumanStateClass.Companion.DEBUG_FLY: Int get() = 19
val HumanStateClass.Companion.SUB_STATE_NONE: Int get() = 0
val HumanStateClass.Companion.SUB_STATE_FORWARD: Int get() = 1
val HumanStateClass.Companion.SUB_STATE_BACKWARD: Int get() = 2
val HumanStateClass.Companion.SUB_STATE_LEFT: Int get() = 3
val HumanStateClass.Companion.SUB_STATE_RIGHT: Int get() = 4
val HumanStateClass.Companion.SUB_STATE_SLOW: Int get() = 8
val HumanStateClass.Companion.CROUCHED_FLAG: Int get() = 2
val HumanStateClass.Companion.SNIPING_FLAG: Int get() = 1
val HumanStateClass.Companion.STATE_COUNT: Int get() = 20

// ---------------------------------------------------------------------------
// WeatherMgrClass singleton
// ---------------------------------------------------------------------------

// C++: WeatherMgrClass (weathermgr.h)
object WeatherMgrClass {
    fun getInstance(): WeatherMgrClass = this
    fun overrideSkyTint(r: Float, g: Float, b: Float): Unit = Unit
    fun restoreSkyTint(): Unit = Unit
    fun overrideClouds(level: Float): Unit = Unit
    fun restoreClouds(): Unit = Unit
    fun overrideLightning(intensity: Float): Unit = Unit
    fun restoreLightning(): Unit = Unit
}

// C++: GlobalSettingsDef instance (extended)
object GlobalSettingsDefClass {
    val encounteredEnemyTimeout: Float = 5f
    val stealthPowerupDuration: Float = 30f
    val repairMultiplier: Float = 1f
    val maxTimeSinceEnemySeen: Float = 30f
    val beaconPlacementEndsGame: Boolean = false
    val tiberiumMultiplier: Float = 1f
    val veterancyGainMultiplier: Float = 1f
    val veterancyPointsForKillMultiplier: Float = 1f
}

// ---------------------------------------------------------------------------
// RenderObjClass — additional methods
// ---------------------------------------------------------------------------

fun RenderObjClass.controlBone(boneIndex: Int, tm: Matrix3D): Unit = Unit
fun RenderObjClass.controlBone(boneIndex: Int, tm: Matrix3D, relative: Boolean): Unit = Unit
fun RenderObjClass.getBoneTransform(name: String): Matrix3D = Matrix3D.IDENTITY
fun RenderObjClass.setSubObjectsMatchLod(match: Boolean): Unit = Unit
fun RenderObjClass.setParentTransform(obj: Any?, boneIndex: Int = -1): Unit = Unit
fun RenderObjClass.getFactory(): RenderObjFactoryClass = RenderObjFactoryClass()
fun RenderObjClass.getBoundingBox(): AABoxClass = AABoxClass()
fun RenderObjClass.getTransform(): Matrix3D = Matrix3D.IDENTITY

// C++: RenderObjClass factory stub
class RenderObjFactoryClass {
    val chunkId: UInt = 0u
    fun save(csave: ChunkSaveClass, obj: RenderObjClass): Unit = Unit
    fun load(cload: ChunkLoadClass): RenderObjClass? = null
}

// ---------------------------------------------------------------------------
// PhysClass — additional methods (on server stub PhysClass which extends RealPhysClass)
// ---------------------------------------------------------------------------

fun PhysClass.setTransform(tm: Matrix3D): Unit = Unit
fun PhysClass.addToScene(scene: PhysicsSceneClass): Unit = Unit
fun PhysClass.removeFromScene(scene: PhysicsSceneClass? = null): Unit = Unit

// ---------------------------------------------------------------------------
// RealPhysClass — extension methods needed by server combat code.
// RealPhysClass = ccr.physics.PhysClass (aliased at top of this file).
// These cover methods that exist in C++ PhysClass but are missing from the Kotlin physics module.
// ---------------------------------------------------------------------------

/** C++: PhysClass::Set_Collision_Group(int) — property setter bridge */
fun RealPhysClass.setCollisionGroup(group: Int) { collisionGroup = group }

/** C++: PhysClass::Peek_Model() — returns null on server (no render objects) */
fun RealPhysClass.peekModel(): RenderObjClass? = null

/** C++: PhysClass::Get_Observer() — returns the CombatPhysObserverClass-typed observer.
 *  The real PhysClass.observer is typed as ccr.physics.PhysObserver; server code needs the
 *  server-stub CombatPhysObserverClass which is stored in the combatObserver field of the
 *  server-stub PhysClass subclass, or null for real physics objects. */
fun RealPhysClass.getObserver(): CombatPhysObserverClass? =
    (this as? PhysClass)?.combatObserver

/** C++: PhysClass::Set_Observer(CombatPhysObserverClass*) */
fun RealPhysClass.setObserver(obs: CombatPhysObserverClass?) {
    (this as? PhysClass)?.combatObserver = obs
}

/** C++: PhysClass::Get_Factory() — returns null on server */
fun RealPhysClass.getFactory(): PhysFactoryClass? =
    (this as? PhysClass)?.getFactory()

/** C++: PhysClass::Set_Tiberium_Region() — no-op on server */
fun RealPhysClass.setTiberiumRegion(enable: Boolean) {}

/** C++: StaticAnimPhysClass::Get_Definition() — returns the PhysDefClass definition.
 *  The definition field is Any?; cast to DefinitionClass for server use. */
fun ccr.physics.static.StaticAnimPhysClass.getDefinition(): ccr.server.defs.DefinitionClass? =
    definition as? ccr.server.defs.DefinitionClass

/** C++: StaticAnimPhysClass::Get_Animation_Manager() — server stub returns a no-op manager */
fun ccr.physics.static.StaticAnimPhysClass.getAnimationManager(): AnimCollisionManagerClass =
    AnimCollisionManagerClass()

/** C++: StaticAnimPhysClass::Peek_Model() — server has no render objects; returns null */
fun ccr.physics.static.StaticAnimPhysClass.peekModel(): RenderObjClass? = null

/** C++: StaticPhysClass::Peek_Model() — server has no render objects; returns null */
fun ccr.physics.static.StaticPhysClass.peekModel(): RenderObjClass? = null

/** C++: StaticPhysClass::Get_Observer() — returns the server-side CombatPhysObserverClass observer */
fun ccr.physics.static.StaticPhysClass.getObserver(): CombatPhysObserverClass? =
    (this as? PhysClass)?.combatObserver

/** C++: StaticPhysClass::Set_Observer() — sets server-side observer */
fun ccr.physics.static.StaticPhysClass.setObserver(obs: CombatPhysObserverClass?) {
    (this as? PhysClass)?.combatObserver = obs
}

/** C++: StaticPhysClass::Is_From_Factory(unsigned int) — check factory chunk ID */
fun ccr.physics.static.StaticPhysClass.isFromFactory(chunkId: UInt): Boolean = false
fun ccr.physics.static.StaticPhysClass.isFromFactory(chunkId: Int): Boolean = false

/** C++: StaticPhysClass::Get_Name() — returns physics object name */
fun ccr.physics.static.StaticPhysClass.getName(): String = this.name

/** C++: LightPhysClass::Get_Group_ID() — server stub returns 0 */
fun ccr.physics.dynamic.LightPhysClass.getGroupId(): Int = 0
fun ccr.physics.dynamic.LightPhysClass.setDisabled(disabled: Boolean) {}
fun ccr.physics.dynamic.LightPhysClass.getName(): String = this.name

/** C++: PhysClass::Is_In_Scene() — already a method on real PhysClass */
// isInScene() is already defined on ccr.physics.PhysClass

/** C++: Remove — alias for removeFromScene (used on emotIconModel / render objs in SoldierGameObj)
 *  Used as `someRenderPhysObj.remove()` — no-op on server */
fun RealPhysClass.remove() {}

/** C++: Get_Position() — already on real PhysClass */
// getPosition() is already defined

/** C++: Set_Position(Vector3) — bridged through real PhysClass.setPosition() which is open */
// setPosition() is already defined

// ---------------------------------------------------------------------------
// Matrix3D — additional methods
// ---------------------------------------------------------------------------

fun Matrix3D.setTranslation(v: Vector3): Matrix3D = this.copy(m03 = v.x, m13 = v.y, m23 = v.z)
fun Matrix3D.translated(dx: Float, dy: Float, dz: Float): Matrix3D = this.copy(m03 = m03 + dx, m13 = m13 + dy, m23 = m23 + dz)

// ---------------------------------------------------------------------------
// PhysicalGameObj — setAppPacketType (network packet type)
// ---------------------------------------------------------------------------

// C++: PhysicalGameObj::Set_App_Packet_Type — sets network replication type
fun PhysicalGameObj.setAppPacketType(type: Int): Unit = Unit

const val APPPACKETTYPE_SOLDIER  = 1
const val APPPACKETTYPE_VEHICLE  = 2
const val APPPACKETTYPE_POWERUP  = 3
const val APPPACKETTYPE_BEACON   = 4
const val APPPACKETTYPE_C4       = 5
const val APPPACKETTYPE_DEFAULT  = 0

// ---------------------------------------------------------------------------
// SaveLoadSystemClass — PersistFactoryClass stub
// ---------------------------------------------------------------------------

// C++: PersistFactoryClass — chunk factory for save/load
class PersistFactoryClass {
    fun load(cload: ChunkLoadClass): Any? = TODO("stub")
}

// ---------------------------------------------------------------------------
// MessageWindowClass (getMessageWindow)
// ---------------------------------------------------------------------------

// C++: MessageWindowClass — server-side message window stub
class MessageWindowClass {
    fun addMessage(message: String): Unit = Unit
    fun addMessage(r: Float, g: Float, b: Float, message: String): Unit = Unit
    fun addMessage(message: String, color: Vector3, extra: Any?, duration: Float): Unit = Unit
}

// C++: CollisionMath namespace — stub for server-side collision queries
object CollisionMath {
    // C++: CollisionMath::OUTSIDE sentinel returned for non-overlapping boxes
    val OUTSIDE: ccr.physics.collision.OverlapType = ccr.physics.collision.OverlapType.OUTSIDE

    fun overlapTest(box: ccr.server.defs.OBBoxClass, pos: Vector3): ccr.physics.collision.OverlapType =
        ccr.physics.collision.OverlapType.OUTSIDE

    // C++: CollisionMath::Overlap_Test(AABoxClass, AABoxClass) — simple AABB overlap test
    fun overlapTest(a: AABoxClass, b: AABoxClass): ccr.physics.collision.OverlapType {
        val ax0 = a.center.x - a.extent.x; val ax1 = a.center.x + a.extent.x
        val ay0 = a.center.y - a.extent.y; val ay1 = a.center.y + a.extent.y
        val az0 = a.center.z - a.extent.z; val az1 = a.center.z + a.extent.z
        val bx0 = b.center.x - b.extent.x; val bx1 = b.center.x + b.extent.x
        val by0 = b.center.y - b.extent.y; val by1 = b.center.y + b.extent.y
        val bz0 = b.center.z - b.extent.z; val bz1 = b.center.z + b.extent.z
        return if (ax1 < bx0 || bx1 < ax0 || ay1 < by0 || by1 < ay0 || az1 < bz0 || bz1 < az0)
            ccr.physics.collision.OverlapType.OUTSIDE
        else
            ccr.physics.collision.OverlapType.INSIDE
    }
}

val ControlClass.Companion.BOOLEAN_JUMP: ControlClass.BooleanControl get() = ControlClass.BooleanControl.JUMP
val ControlClass.Companion.BOOLEAN_WEAPON_NEXT: ControlClass.BooleanControl get() = ControlClass.BooleanControl.WEAPON_NEXT
val ControlClass.Companion.BOOLEAN_WEAPON_PREV: ControlClass.BooleanControl get() = ControlClass.BooleanControl.WEAPON_PREV
val ControlClass.Companion.BOOLEAN_WEAPON_RELOAD: ControlClass.BooleanControl get() = ControlClass.BooleanControl.WEAPON_RELOAD
val ControlClass.Companion.BOOLEAN_WEAPON_USE: ControlClass.BooleanControl get() = ControlClass.BooleanControl.WEAPON_USE
val ControlClass.Companion.BOOLEAN_DIVE_FORWARD: ControlClass.BooleanControl get() = ControlClass.BooleanControl.DIVE_FORWARD
val ControlClass.Companion.BOOLEAN_DIVE_BACKWARD: ControlClass.BooleanControl get() = ControlClass.BooleanControl.DIVE_BACKWARD
val ControlClass.Companion.BOOLEAN_DIVE_LEFT: ControlClass.BooleanControl get() = ControlClass.BooleanControl.DIVE_LEFT
val ControlClass.Companion.BOOLEAN_DIVE_RIGHT: ControlClass.BooleanControl get() = ControlClass.BooleanControl.DIVE_RIGHT
val ControlClass.Companion.BOOLEAN_ACTION: ControlClass.BooleanControl get() = ControlClass.BooleanControl.ACTION
val ControlClass.Companion.BOOLEAN_CROUCH: ControlClass.BooleanControl get() = ControlClass.BooleanControl.CROUCH
val ControlClass.Companion.BOOLEAN_WALK: ControlClass.BooleanControl get() = ControlClass.BooleanControl.WALK
val ControlClass.Companion.BOOLEAN_WEAPON_FIRE_PRIMARY: ControlClass.BooleanControl get() = ControlClass.BooleanControl.WEAPON_FIRE_PRIMARY
val ControlClass.Companion.BOOLEAN_WEAPON_FIRE_SECONDARY: ControlClass.BooleanControl get() = ControlClass.BooleanControl.WEAPON_FIRE_SECONDARY
val ControlClass.Companion.BOOLEAN_VEHICLE_TOGGLE_GUNNER: ControlClass.BooleanControl get() = ControlClass.BooleanControl.VEHICLE_TOGGLE_GUNNER
val ControlClass.Companion.BOOLEAN_DROP_FLAG: ControlClass.BooleanControl get() = ControlClass.BooleanControl.DROP_FLAG
val ControlClass.Companion.ANALOG_MOVE_FORWARD: ControlClass.AnalogControl get() = ControlClass.AnalogControl.MOVE_FORWARD
val ControlClass.Companion.ANALOG_MOVE_LEFT: ControlClass.AnalogControl get() = ControlClass.AnalogControl.MOVE_LEFT
val ControlClass.Companion.ANALOG_MOVE_UP: ControlClass.AnalogControl get() = ControlClass.AnalogControl.MOVE_UP
val ControlClass.Companion.ANALOG_TURN_LEFT: ControlClass.AnalogControl get() = ControlClass.AnalogControl.TURN_LEFT

val HumanStateClass.Companion.LAND: Int get() = 18
val HumanStateClass.Companion.LADDER_ENTER_TOP: Int get() = 0
val HumanStateClass.Companion.LADDER_ENTER_BOTTOM: Int get() = 1
val HumanStateClass.Companion.VEHICLE_ENTER: Int get() = 0

// ---------------------------------------------------------------------------
// HumanStateClass — body zone (hit zone) constants
// ---------------------------------------------------------------------------

val HumanStateClass.Companion.HEAD_FROM_BEHIND: Int       get() = 20
val HumanStateClass.Companion.HEAD_FROM_FRONT: Int        get() = 21
val HumanStateClass.Companion.TORSO_FROM_BEHIND: Int      get() = 22
val HumanStateClass.Companion.TORSO_FROM_FRONT: Int       get() = 23
val HumanStateClass.Companion.LEFT_ARM_FROM_BEHIND: Int   get() = 24
val HumanStateClass.Companion.LEFT_ARM_FROM_FRONT: Int    get() = 25
val HumanStateClass.Companion.RIGHT_ARM_FROM_BEHIND: Int  get() = 26
val HumanStateClass.Companion.RIGHT_ARM_FROM_FRONT: Int   get() = 27
val HumanStateClass.Companion.LEFT_LEG_FROM_BEHIND: Int   get() = 28
val HumanStateClass.Companion.LEFT_LEG_FROM_FRONT: Int    get() = 29
val HumanStateClass.Companion.RIGHT_LEG_FROM_BEHIND: Int  get() = 30
val HumanStateClass.Companion.RIGHT_LEG_FROM_FRONT: Int   get() = 31
val HumanStateClass.Companion.GROIN: Int                  get() = 32

// C++: HumanStateClass::Get_Ouch_Type(direction, collisionBoxName)
fun HumanStateClass.getOuchType(direction: Vector3, collisionBoxName: String?): Int =
    HumanStateClass.TORSO_FROM_FRONT

// ---------------------------------------------------------------------------
// CombatMaterialEffectManager — getHealthEffect, getElectrocutionEffect
// ---------------------------------------------------------------------------

// MaterialEffectClass is a concrete subtype of TransitionEffectClass
class MaterialEffectClass : TransitionEffectClass()

// ---------------------------------------------------------------------------
// BackgroundMgrClass — overrideSkyTint, overrideClouds, overrideLightning, restoreAll
// ---------------------------------------------------------------------------

object BackgroundMgrClass {
    fun overrideSkyTint(brightness: Float, duration: Float): Unit = Unit
    fun overrideClouds(gloom: Float, raininess: Float, duration: Float): Unit = Unit
    fun overrideLightning(hz: Float, start: Float, duration: Float, intensity: Float, startDist: Float, transitionTime: Float): Unit = Unit
    fun restoreSkyTint(duration: Float): Unit = Unit
    fun restoreClouds(duration: Float): Unit = Unit
    fun restoreLightning(duration: Float): Unit = Unit
}

// ---------------------------------------------------------------------------
// WeatherMgrClass — full weather methods with correct signatures
// ---------------------------------------------------------------------------

fun WeatherMgrClass.overrideSkyTint(brightness: Float, duration: Float): Unit = Unit
fun WeatherMgrClass.overrideWind(speed: Float, direction: Float, variability: Float, duration: Float): Unit = Unit
fun WeatherMgrClass.overrideClouds(gloom: Float, raininess: Float, duration: Float): Unit = Unit
fun WeatherMgrClass.overrideLightning(hz: Float, start: Float, duration: Float, intensity: Float, startDist: Float, transitionTime: Float): Unit = Unit
fun WeatherMgrClass.overridePrecipitation(type: Int, density: Float, duration: Float = 0f): Unit = Unit
fun WeatherMgrClass.restoreSkyTint(duration: Float): Unit = Unit
fun WeatherMgrClass.restoreWind(duration: Float): Unit = Unit
fun WeatherMgrClass.restoreClouds(duration: Float): Unit = Unit
fun WeatherMgrClass.restoreLightning(duration: Float): Unit = Unit
fun WeatherMgrClass.restorePrecipitation(duration: Float): Unit = Unit

val WeatherMgrClass.PRECIPITATION_RAIN: Int get() = 0
val WeatherMgrClass.PRECIPITATION_ASH: Int get() = 1

// ---------------------------------------------------------------------------
// ExplosionDefinitionClass — stub
// ---------------------------------------------------------------------------

class ExplosionDefinitionClass {
    val damageRadius: Float = 0f
    val damageStrength: Float = 0f
    val damageWarhead: Int = 0
}

fun PhysAABoxIntersectionTestClass(box: AABoxClass?, collisionGroup: Int, collisionType: Int): PhysAABoxIntersectionTestClass =
    PhysAABoxIntersectionTestClass(box ?: AABoxClass(), collisionGroup, collisionType)
