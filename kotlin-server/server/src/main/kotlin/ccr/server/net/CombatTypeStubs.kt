package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.net.replication.NetworkObjectManager
import ccr.server.defs.DefinitionClass

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
    fun setHidden(hidden: Boolean): Unit = TODO("stub")
    fun isHidden(): Boolean = TODO("stub")
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
    fun getHTree(): HTreeClass? = TODO("stub")
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

// C++: PhysClass abstract stub — real impl is ccr.physics.PhysClass
// These stubs are used by server net code that needs the C++ interface.
// Note: ccr.physics.PhysClass (the real one) is imported explicitly where needed.
open class PhysClass {
    open fun getPosition(): Vector3 = TODO("stub")
    fun setPosition(pos: Vector3): Unit = TODO("stub")
    fun getFacing(): Float = TODO("stub")
    open fun getTransform(): Matrix3D = TODO("stub")
    fun setTransform(tm: Matrix3D): Unit = TODO("stub")
    fun peekModel(): RenderObjClass? = TODO("stub")
    fun setModelByName(name: String): Unit = TODO("stub")
    fun setCollisionGroup(group: Int): Unit = TODO("stub")
    fun setObserver(observer: CombatPhysObserverClass?): Unit = TODO("stub")
    fun getVisObjectId(): Int = TODO("stub")
    fun isInScene(): Boolean = TODO("stub")
    fun getCollisionGroup(): Int = TODO("stub")
    fun getContactSurfaceType(): Int = TODO("stub")
    fun getObserver(): PhysObserverClass? = TODO("stub")
    fun getFactory(): PhysFactoryClass? = TODO("stub")
    // As_* casts
    open fun asMoveablePhysClass(): MoveablePhysClass? = null
    open fun asProjectileClass(): ProjectilePhysClass? = null
    open fun asStaticPhysClass(): StaticPhysClass? = null
    open fun asStaticAnimPhysClass(): StaticAnimPhysClass? = null
    open fun asHumanPhysClass(): HumanPhysClass? = null
    open fun asVehiclePhysClass(): VehiclePhysClassStub? = null
    open fun asSound3DClass(): Sound3DClass? = null
    open fun asLightPhysClass(): LightPhysClass? = null
    open fun incIgnoreCounter(): Unit = TODO("stub")
    open fun decIgnoreCounter(): Unit = TODO("stub")
    fun intersectionTest(test: PhysAABoxIntersectionTestClass): Boolean = TODO("stub")
    open fun enableUserControl(enable: Boolean): Unit = Unit
    open fun addEffectToMe(effect: Any): Unit = TODO("stub")
    open fun addToScene(scene: Any): Unit = TODO("stub")
    open fun removeFromScene(): Unit = TODO("stub")
    open fun isEnabled(): Boolean = TODO("stub")
    var scene: ccr.physics.scene.PhysicsScene? = null
}

// C++: MoveablePhysClass : public PhysClass
open class MoveablePhysClass : PhysClass() {
    override fun asMoveablePhysClass(): MoveablePhysClass = this
    fun setVelocity(vel: Vector3): Unit = TODO("stub")
    fun getVelocity(): Vector3 = TODO("stub")
    fun getAngularVelocity(): Vector3 = TODO("stub")
    fun cinematicMoveTo(tm: Matrix3D): Unit = TODO("stub")
    override fun addEffectToMe(effect: Any): Unit = TODO("stub")
    fun removeEffectFromMe(effect: Any): Unit = TODO("stub")
    fun computeApproximateRideHeight(): Float = TODO("stub")
    fun getSquishVelocity(): Float = TODO("stub")
    fun setGravityMultiplier(multiplier: Float): Unit = TODO("stub")
    override fun enableUserControl(enable: Boolean): Unit = TODO("stub")
}

// C++: StaticPhysClass : public PhysClass
open class StaticPhysClass : PhysClass() {
    override fun asStaticPhysClass(): StaticPhysClass = this
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
    override fun asStaticAnimPhysClass(): StaticAnimPhysClass = this
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
    override fun asHumanPhysClass(): HumanPhysClass = this
    fun getLegMode(): Boolean = TODO("stub")
    fun setLadderIndex(index: Int): Unit = TODO("stub")
    fun isSubStateAdjustable(): Boolean = TODO("stub")
    fun getHeading(): Float = TODO("stub")
    fun setHeading(heading: Float): Unit = TODO("stub")
    fun setInContact(inContact: Boolean): Unit = TODO("stub")
    fun setLoitersAllowed(allowed: Boolean): Unit = TODO("stub")
    fun resetLoiterDelay(): Unit = TODO("stub")
    fun canTeleport(pos: Vector3): Boolean = TODO("stub")
    fun findTeleportLocation(pos: Vector3, radius: Float, out: Vector3): Boolean = TODO("stub")
    fun setDisabled(disabled: Boolean): Unit = TODO("stub")
    fun isEngineEnabled(): Boolean = TODO("stub")
    fun enableEngine(enable: Boolean): Unit = TODO("stub")
}

// C++: VehiclePhysClass stub (different from ccr.physics.vehicle.VehiclePhysClass)
open class VehiclePhysClassStub : MoveablePhysClass() {
    override fun asVehiclePhysClass(): VehiclePhysClassStub = this
    fun getNormalizedSpeed(): Float = TODO("stub")
    fun setNormalizedSpeed(speed: Float): Unit = TODO("stub")
    fun setTurnVelocity(vel: Float): Unit = TODO("stub")
    fun isEngineEnabled(): Boolean = TODO("stub")
    fun enableEngine(enable: Boolean): Unit = TODO("stub")
    fun getPitchFactor(): Float = TODO("stub")
    fun setPitchFactor(f: Float): Unit = TODO("stub")
    fun getOrientation(): ccr.math.Quaternion = TODO("stub")
    fun networkInterpolateStateUpdate(
        pos: Vector3, q: ccr.math.Quaternion, vel: Vector3, angVel: Vector3, dt: Float
    ): Unit = TODO("stub")
    open fun asVTOLVehicleClass(): VTOLVehicleClass? = null
}

// C++: VTOLVehicleClass : public VehiclePhysClass
open class VTOLVehicleClass : VehiclePhysClassStub() {
    override fun asVTOLVehicleClass(): VTOLVehicleClass? = this
}

// C++: ProjectileClass (physics stub)
open class ProjectilePhysClass : MoveablePhysClass() {
    override fun asProjectileClass(): ProjectilePhysClass = this
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
    override fun asLightPhysClass(): LightPhysClass = this
    fun setDisabled(disabled: Boolean): Unit = TODO("stub")
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
    fun setAnimation(name: String, vararg args: Any): Unit = TODO("stub")
    fun setMode(mode: Int, vararg args: Any): Unit = TODO("stub")
    fun setTargetFrame(frame: Int): Unit = TODO("stub")
    fun update(dt: Float): Unit = TODO("stub")
    fun isComplete(): Boolean = TODO("stub")
    fun getAnimationName(): String = TODO("stub")
    fun getCurrentFrame(): Int = TODO("stub")
    fun getTargetFrame(): Int = TODO("stub")
    fun getMode(): Int = TODO("stub")
    fun save(csave: ChunkSaveClass): Unit = TODO("stub")
    fun load(cload: ChunkLoadClass): Unit = TODO("stub")
    fun peekCarrierObject(): Any? = TODO("stub")
    fun peekAnimation(): Animatable3DObjClass? = null
    fun peekModel(): RenderObjClass? = null
    fun resetAnimUpdate(): Unit = TODO("stub")
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

// C++: PhysicsSceneClass — server-side physics scene
class PhysicsSceneClass {
    fun removeObject(obj: PhysClass?): Unit = Unit
    fun addDynamicObject(obj: PhysClass): Unit = Unit
    fun getLevelMin(): Vector3 = Vector3()
    fun getLevelMax(): Vector3 = Vector3()
    fun collectObjects(box: Any, dynamic: Boolean, includeAll: Boolean, results: MutableList<PhysClass>): Unit = Unit
    // Convenience overload that returns a list directly
    fun collectObjects(box: Any, dynamic: Boolean, includeAll: Boolean): List<PhysClass> = emptyList()
    fun castRay(ray: PhysRayCollisionTestClass): Boolean = false
    fun collectStaticObjects(pos: Vector3, radius: Float, results: MutableList<Any>): Unit = Unit
    fun findStaticObject(name: String): StaticPhysClass? = null
    fun findStaticObject(id: Int): StaticPhysClass? = null
    // C++: Get_Static_Objects() iterates all static objects
    fun getStaticObjects(): List<StaticPhysClass> = emptyList()
    // C++: Get_Static_Lights() iterates all static light objects
    fun getStaticLights(): List<LightPhysClass> = emptyList()

    companion object {
        fun getInstance(): PhysicsSceneClass = PhysicsSceneClass()
    }
}

// ---------------------------------------------------------------------------
// Human state / soldier animation stubs
// ---------------------------------------------------------------------------

// C++: HumanStateClass (soldier.h)
class HumanStateClass {
    private var _state: Int = UPRIGHT
    private var _subState: Int = 0

    fun setAnimControl(animControl: HumanAnimControlClass?): Unit = Unit
    fun getState(): Int = _state
    fun setState(state: Int): Unit { _state = state }
    fun getSubState(): Int = _subState
    fun getStateName(): String = ""
    fun getLegFacing(): Float = 0f
    fun isFiring(): Boolean = false
    fun isReloading(): Boolean = false

    companion object {
        const val UPRIGHT   = 0
        const val CROUCH    = 1
        const val DEATH     = 2
        const val IN_VEHICLE = 3
        const val OUCH_FIRE = 4
        const val LADDER    = 5
    }
}

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
    fun stop(): Unit = TODO("stub")
    fun play(): Unit = TODO("stub")
    fun isPlaying(): Boolean = TODO("stub")
    fun isSoundCulled(): Boolean = TODO("stub")
    fun asSound3DClass(): Sound3DClass? = null
    fun getDuration(): Float = TODO("stub")
    fun setDropOffRadius(radius: Float): Unit = TODO("stub")
    fun setTransform(center: Vector3): Unit = TODO("stub")
    fun setTransform(tm: Matrix3D): Unit = TODO("stub")
    fun addToScene(): Unit = TODO("stub")
    fun addToScene(updateNow: Boolean): Unit = TODO("stub")
    fun removeFromScene(): Unit = TODO("stub")
    fun getString(): String? = TODO("stub")
    fun getSoundId(): Int = TODO("stub")
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
    fun getScene(): PhysicsSceneClass? = null
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

// C++: WW3DAssetManager (ww3d2/assetmgr.h)
object WW3DAssetManager {
    fun getInstance(): WW3DAssetManager = this
    fun createRenderObjFromFilename(filename: String): RenderObjClass? = null
    fun getRenderObjNameFromFilename(filename: String): String = ""
    // C++: Get_HAnim(name) — returns animation object; getTotalTime() called on it
    fun getHAnim(name: String): HAnimClass? = null
}

// C++: HAnimClass — animation asset stub
class HAnimClass {
    fun getTotalTime(): Float = TODO("stub")
}

// C++: HTreeClass — skeleton/bone hierarchy (htree.h)
class HTreeClass {
    fun getName(): String = ""
    fun getNumBones(): Int = 0
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

// C++: GlobalSettingsDef
object GlobalSettingsDef {
    val encounteredEnemyTimeout: Float = 5f
    val stealthPowerupDuration: Float = 30f
    val repairMultiplier: Float = 1f
    val maxTimeSinceEnemySeen: Float = 30f
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

const val SERVER_CONTROL_OWNER = -2

const val BOOLEAN_ACTION                 = 9
const val BOOLEAN_CROUCH                 = 10
const val BOOLEAN_DIVE_FORWARD           = 5
const val BOOLEAN_DIVE_BACKWARD          = 6
const val BOOLEAN_DIVE_LEFT              = 7
const val BOOLEAN_DIVE_RIGHT             = 8
const val BOOLEAN_DROP_FLAG              = 11
const val BOOLEAN_JUMP                   = 0
const val BOOLEAN_ONE_TIME_FIRST         = 0
const val BOOLEAN_VEHICLE_TOGGLE_GUNNER  = 12
const val BOOLEAN_WALK                   = 13
const val BOOLEAN_WEAPON_FIRE_PRIMARY    = 14
const val BOOLEAN_WEAPON_USE             = 4

const val ANALOG_MOVE_FORWARD = 0
const val ANALOG_MOVE_LEFT    = 1
const val ANALOG_MOVE_UP      = 2
const val ANALOG_TURN_LEFT    = 3

const val STEALTH_BROKEN_FRACTION = 0.75f

// C++: physcontroller chunk IDs (physcontroller.h)
const val PHYSCONTROLLER_CHUNK_VARIABLES    = 0x00660015
const val PHYSCONTROLLER_VARIABLE_MOVEVECTOR = 0x01
const val PHYSCONTROLLER_VARIABLE_TURNLEFT   = 0x02
const val MICROCHUNKID_CONTROLLER_PTR        = 1
const val MICROCHUNKID_CONTROL_OWNER         = 2
const val MICROCHUNKID_CONTROL_ENABLED       = 3
const val MICROCHUNKID_STEALTH_ENABLED       = 4
const val MICROCHUNKID_STEALTH_POWERUP_TIMER = 5
const val MICROCHUNKID_STEALTH_FIRING_TIMER  = 6
const val MICROCHUNKID_PLAYER_DATA           = 7
const val MICROCHUNKID_IS_ENEMY_SEEN_ENABLED = 8
const val MICROCHUNKID_MOVING_SOUND_TIMER    = 9

const val CHUNKID_ARMEDGAMEOBJ_PARENT = 418001841
const val CHUNKID_CONTROL             = 627001201
const val CHUNKID_CONTROLLER          = 627001202
const val CHUNKID_ACTION              = 627001203
const val CHUNKID_STEALTH_EFFECT      = 627001204

// Soldier AI states
const val AI_STATE_IDLE    = 0
const val AI_STATE_PATROL  = 1
const val AI_STATE_ATTACK  = 2

// Building aggregate physics chunk ID
const val PHYSICS_CHUNKID_BUILDINGAGGREGATE = 0xDC3000u

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
fun Matrix3D.getYRotation(): Float = TODO("stub")

// C++: Matrix3D::identity() factory
fun Matrix3D.Companion.identity(): Matrix3D = IDENTITY

// C++: createAnimationName — derives full animation name from model name
fun createAnimationName(animBase: String, modelName: String): String {
    val model = modelName.substringBefore('.').uppercase()
    return "$model.$animBase"
}

// ---------------------------------------------------------------------------
// ArmorWarheadManager — armor × warhead damage/special-damage type
// ---------------------------------------------------------------------------

// C++: ArmorWarheadManager (armorwarheadmanager.h)
object ArmorWarheadManager {
    // C++: enum SpecialDamageType
    const val SPECIAL_DAMAGE_TYPE_NONE     = 0
    const val SPECIAL_DAMAGE_TYPE_FIRE     = 1
    const val SPECIAL_DAMAGE_TYPE_CHEM     = 2
    const val SPECIAL_DAMAGE_TYPE_ELECTRIC = 3
    const val SPECIAL_DAMAGE_TYPE_CNC_FIRE = 4
    const val SPECIAL_DAMAGE_TYPE_CNC_CHEM = 5

    fun scaleDamage(rawDamage: Float, warheadId: Int, armorId: Int): Float = rawDamage
    fun getSpecialDamageDuration(type: Int): Float = 5f
    fun getSpecialDamageWarhead(type: Int): Int = 0
    fun getSpecialDamageDef(type: Int): Any? = null
}

// ---------------------------------------------------------------------------
// HumanStateClass additional state constants
// ---------------------------------------------------------------------------

// These extend HumanStateClass.companion in CombatTypeStubs
// C++: Additional soldier states from soldier.h / humanstate.h
const val HUMAN_STATE_UPRIGHT   = 0
const val HUMAN_STATE_CROUCH    = 1
const val HUMAN_STATE_DEATH     = 2
const val HUMAN_STATE_IN_VEHICLE = 3
const val HUMAN_STATE_OUCH_FIRE = 4
const val HUMAN_STATE_LADDER    = 5
const val HUMAN_STATE_DIVE      = 6
const val HUMAN_STATE_TRANSITION = 7
const val HUMAN_STATE_ANIMATION  = 8
const val HUMAN_STATE_ON_FIRE    = 9
const val HUMAN_STATE_ON_CHEM    = 10
const val HUMAN_STATE_ON_CNC_FIRE = 11
const val HUMAN_STATE_ON_CNC_CHEM = 12
const val HUMAN_STATE_ON_ELECTRIC = 13
const val HUMAN_STATE_DESTROY    = 14
const val HUMAN_STATE_TRANSITION_COMPLETE = 15
const val HUMAN_STATE_AIRBORNE   = 16
const val HUMAN_STATE_SNIPING    = 17

// C++: HumanStateClass sub-states for DIVE
const val HUMAN_SUB_STATE_NONE     = 0
const val HUMAN_SUB_STATE_FORWARD  = 1
const val HUMAN_SUB_STATE_BACKWARD = 2
const val HUMAN_SUB_STATE_LEFT     = 3
const val HUMAN_SUB_STATE_RIGHT    = 4
const val HUMAN_SNIPING_FLAG       = 1

// ---------------------------------------------------------------------------
// HumanStateClass extended methods
// ---------------------------------------------------------------------------

// Extend HumanStateClass with additional methods needed by SoldierGameObj
fun HumanStateClass.init(physObj: HumanPhysClass?): Unit = Unit
fun HumanStateClass.setHumanAnimOverride(defId: Int): Unit = Unit
fun HumanStateClass.setHumanLoiterCollection(defId: Int): Unit = Unit
fun HumanStateClass.reset(): Unit = Unit
fun HumanStateClass.setState(state: Int, subState: Int = 0): Unit = Unit
fun HumanStateClass.setStateTimer(timer: Float): Unit = Unit
fun HumanStateClass.getStateTimer(): Float = 0f
fun HumanStateClass.save(csave: ChunkSaveClass): Unit = Unit
fun HumanStateClass.load(cload: ChunkLoadClass): Unit = Unit
fun HumanStateClass.updateState(physObj: HumanPhysClass? = null): Unit = Unit
fun HumanStateClass.postThink(): Unit = Unit
fun HumanStateClass.getStyle(): Int = 0
fun HumanStateClass.getModelName(): String = ""
fun HumanStateClass.isModelUpdateNeeded(): Boolean = false
fun HumanStateClass.resetModelUpdate(): Unit = Unit

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
// TransitionCompletionDataStruct — additional save/load
// ---------------------------------------------------------------------------

fun TransitionCompletionDataStruct.save(csave: ChunkSaveClass): Unit = Unit
fun TransitionCompletionDataStruct.load(cload: ChunkLoadClass): Unit = Unit

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
// PhysClass — additional methods
// ---------------------------------------------------------------------------

fun PhysClass.setTransform(tm: Matrix3D): Unit = Unit
fun PhysClass.addToScene(scene: PhysicsSceneClass): Unit = Unit
fun PhysClass.removeFromScene(scene: PhysicsSceneClass? = null): Unit = Unit

// ---------------------------------------------------------------------------
// Matrix3D — additional methods
// ---------------------------------------------------------------------------

fun Matrix3D.setTranslation(v: Vector3): Matrix3D = this.copy(m03 = v.x, m13 = v.y, m23 = v.z)
fun Matrix3D.getZRotation(): Float = TODO("stub")
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
// ControlClass — BooleanControl and AnalogControl access
// ---------------------------------------------------------------------------

fun ControlClass.getBoolean(control: ControlClass.BooleanControl): Boolean = false
fun ControlClass.getAnalog(control: ControlClass.AnalogControl): Float = 0f
fun ControlClass.setBoolean(control: ControlClass.BooleanControl, value: Boolean): Unit = Unit

// ---------------------------------------------------------------------------
// Vector3 — operator minus and length2
// ---------------------------------------------------------------------------

operator fun Vector3.minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
operator fun Vector3.times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
fun Vector3.length2(): Float = x * x + y * y + z * z
fun Vector3.length(): Float = kotlin.math.sqrt(length2())

// ---------------------------------------------------------------------------
// SmartGameObj — additional methods
// ---------------------------------------------------------------------------

// C++: HumanStateClass::Get_Information — appends state info to string builder
fun HumanStateClass.getInformation(sb: StringBuilder): Unit = Unit

// C++: SoldierObserverClass::Get_Information — appends observer info to string builder
fun SoldierObserverClass.getInformation(sb: StringBuilder): Unit = Unit

// ---------------------------------------------------------------------------
// SoldierGameObj — setActionStatusValue
// ---------------------------------------------------------------------------

fun SoldierGameObj.setActionStatusValue(value: Int): Unit = Unit
fun SoldierGameObj.doesBeaconPlacementEndsGame(): Boolean = false
fun SoldierGameObj.enableUserControl(enable: Boolean): Unit = Unit

// PlayerDataClass stats methods (called on PlayerDataClass, not SoldierGameObj)
fun PlayerDataClass.statsAddGameTime(delta: Float): Unit = Unit
fun PlayerDataClass.statsSetFinalHealth(health: Float): Unit = Unit
fun PlayerDataClass.statsAddVehicleTime(delta: Float): Unit = Unit
fun PlayerDataClass.statsAddVehicleDestroyed(): Unit = Unit
fun PlayerDataClass.statsAddBuildingDestroyed(): Unit = Unit
fun PlayerDataClass.statsAddSquish(): Unit = Unit
fun PlayerDataClass.getPunishTimer(): Float = 0f
fun PlayerDataClass.incPunishTimer(delta: Float): Unit = Unit

// ---------------------------------------------------------------------------
// PhysicalGameObj — additional methods
// ---------------------------------------------------------------------------

fun PhysicalGameObj.isUnitInZone(zoneType: Int): Boolean = false
fun PhysicalGameObj.enableUserControl(enable: Boolean): Unit = Unit

// ---------------------------------------------------------------------------
// AirStripGameObj helpers
// ---------------------------------------------------------------------------

// generatingVehicleId — on VehicleFactoryGameObj or similar
// These are fields accessed on AirStripGameObj, handled by declaring them in stubs

// ---------------------------------------------------------------------------
// BuildingGameObj — additional stub methods
// ---------------------------------------------------------------------------

fun BuildingAggregateClass.getStaticObjects(): List<StaticPhysClass> = emptyList()
fun BuildingAggregateClass.getStaticLights(): List<LightPhysClass> = emptyList()

// C++: StaticPhysClass::Get_Static_Objects (different from BuildingAgg version)
fun StaticPhysClass.getStaticObjects(): List<StaticPhysClass> = emptyList()

// C++: LightPhysClass::Get_Static_Lights (stub)
fun StaticPhysClass.getStaticLights(): List<LightPhysClass> = emptyList()

// MapMgrClass — already has findBase/getBeaconZone as object methods above

// ---------------------------------------------------------------------------
// MessageWindowClass (getMessageWindow)
// ---------------------------------------------------------------------------

// C++: MessageWindowClass — server-side message window stub
class MessageWindowClass {
    fun addMessage(message: String): Unit = Unit
    fun addMessage(r: Float, g: Float, b: Float, message: String): Unit = Unit
    fun addMessage(message: String, color: Vector3, extra: Any?, duration: Float): Unit = Unit
}

object MessageWindow {
    fun getMessageWindow(): MessageWindowClass = MessageWindowClass()
    fun getInstance(): MessageWindowClass = MessageWindowClass()
}

// C++: getMessageWindow() — free function
fun getMessageWindow(): MessageWindowClass = MessageWindowClass()

// ---------------------------------------------------------------------------
// WW3DAssetManager — extended
// ---------------------------------------------------------------------------

// WW3DAssetManager — createRenderObjFromFilename/getRenderObjNameFromFilename are already on the object above

// ---------------------------------------------------------------------------
// PlayerDataClass — additional methods
// ---------------------------------------------------------------------------

fun PlayerDataClass.getPlayerType(): Int = 0
fun PlayerDataClass.setPlayerType(type: Int): Unit = Unit
fun PlayerDataClass.getName(): String = ""
fun PlayerDataClass.addMoney(amount: Float): Unit = Unit
fun PlayerDataClass.getId(): Int = 0
fun PlayerDataClass.getOwner(): SoldierGameObj? = null
fun PlayerDataClass.getAction(): Any? = null
fun PlayerDataClass.getGlobalSettings(): GlobalSettingsDefClass = GlobalSettingsDefClass
fun PlayerDataClass.getMoveForward(): Float = 0f
fun PlayerDataClass.getMoveLeft(): Float = 0f
fun PlayerDataClass.setMoveForward(v: Float): Unit = Unit
fun PlayerDataClass.setMoveLeft(v: Float): Unit = Unit
fun PlayerDataClass.isAvailable(): Boolean = false
fun PlayerDataClass.setTargetParameter(key: String, value: Any?): Unit = Unit

// ---------------------------------------------------------------------------
// WeaponClass — additional methods
// ---------------------------------------------------------------------------

fun WeaponClass.getId(): Int = definitionId
fun WeaponClass.getPlayerType(): Int = 0
fun WeaponClass.setPlayerType(type: Int): Unit = Unit
fun WeaponClass.getTotalRounds(): Int = totalRounds
fun WeaponClass.getTotalTime(): Float = 0f
fun WeaponClass.isFiring(): Boolean = false
fun WeaponClass.getStyle(): Int = 0
fun WeaponClass.getWeaponBag(): WeaponBagClass? = null
fun WeaponClass.stopFiringSound(): Unit = Unit
fun WeaponClass.updateWeapon(isChanged: Boolean): Unit = Unit
fun WeaponClass.isLoaded(): Boolean = totalRounds > 0
fun WeaponClass.findTypedDefinition(): Any? = null
fun WeaponClass.findAmmoDefinition(): Any? = null
fun WeaponClass.getFloat(key: String): Float = 0f
fun WeaponClass.getUInt(): UInt = 0u

// ---------------------------------------------------------------------------
// WeaponDefinitionClass — additional
// ---------------------------------------------------------------------------

fun ccr.server.defs.WeaponDefinitionClass.getId(): Int = this.id.toInt()
fun ccr.server.defs.WeaponDefinitionClass.getPlayerType(): Int = 0
fun ccr.server.defs.WeaponDefinitionClass.getTotalRounds(): Int = 0
fun ccr.server.defs.WeaponDefinitionClass.explosionDefId(): Int = 0
fun ccr.server.defs.WeaponDefinitionClass.stuckOffsetX(): Float = 0f
fun ccr.server.defs.WeaponDefinitionClass.stuckOffsetY(): Float = 0f
fun ccr.server.defs.WeaponDefinitionClass.stuckOffsetZ(): Float = 0f
val ccr.server.defs.WeaponDefinitionClass.explosionDefId: Int get() = 0
val ccr.server.defs.WeaponDefinitionClass.stuckOffsetX: Float get() = 0f
val ccr.server.defs.WeaponDefinitionClass.stuckOffsetY: Float get() = 0f
val ccr.server.defs.WeaponDefinitionClass.stuckOffsetZ: Float get() = 0f

// ---------------------------------------------------------------------------
// ScriptableGameObj — additional methods
// ---------------------------------------------------------------------------

fun ScriptableGameObj.getOwner(): SoldierGameObj? = null
fun ScriptableGameObj.getName(): String = ""
fun ScriptableGameObj.getPlayerType(): Int = (this as? DamageableGameObj)?.playerType ?: 0
fun ScriptableGameObj.getId(): Int = networkId
fun ScriptableGameObj.getDefinition(): ccr.server.defs.BaseGameObjDef? = definition
fun ScriptableGameObj.getAction(): ActionClass? = null
fun ScriptableGameObj.getGlobalSettings(): GlobalSettingsDefClass = GlobalSettingsDefClass

// ---------------------------------------------------------------------------
// ArmedGameObj — additional methods
// ---------------------------------------------------------------------------

fun ArmedGameObj.getWeaponBag(): WeaponBagClass = weaponBag
fun ArmedGameObj.getOwner(): SoldierGameObj? = null

// ---------------------------------------------------------------------------
// VehicleGameObj — additional
// ---------------------------------------------------------------------------

fun VehicleGameObj.getId(): Int = networkId
fun VehicleGameObj.createObject(defId: Int, tm: Matrix3D): ScriptableGameObj? = null
fun VehicleGameObj.serverExplode(defId: Int, tm: Matrix3D, owner: Any?): Unit = Unit
fun VehicleGameObj.getOwner(): SoldierGameObj? = null
fun VehicleGameObj.getName(): String = ""

// ---------------------------------------------------------------------------
// BaseControllerClass — additional
// ---------------------------------------------------------------------------

// C++: BaseControllerClass::Distribute_Funds(team, amount)
// team field is on BaseControllerClass — handled as a var in BaseControllerClass.kt

// ---------------------------------------------------------------------------
// WarFactoryGameObj / AirStripGameObj — generatingVehicleId field
// ---------------------------------------------------------------------------

// C++: VehicleFactoryGameObj::GeneratingVehicleID — ID of vehicle being generated
// This field is already in VehicleFactoryGameObj.kt; if AirStripGameObj needs it,
// it should be declared there. We provide the stub concept here.

// ---------------------------------------------------------------------------
// StaticAnimPhysClass — additional
// ---------------------------------------------------------------------------

fun StaticAnimPhysClass.setAnimation(animName: String): Unit = Unit

// ---------------------------------------------------------------------------
// Explosion / server-side effects
// ---------------------------------------------------------------------------

// C++: serverExplode (server_explosion.h)
fun serverExplode(defId: Int, tm: Matrix3D, owner: Any?): Unit = Unit

// ---------------------------------------------------------------------------
// CollisionMath — proxy to ccr.physics.collision
// ---------------------------------------------------------------------------

// C++: CollisionMath namespace — stub for server-side collision queries
object CollisionMath {
    fun overlapTest(box: ccr.server.defs.OBBoxClass, pos: Vector3): ccr.physics.collision.OverlapType =
        ccr.physics.collision.OverlapType.OUTSIDE
}

// ---------------------------------------------------------------------------
// HumanPhysClass — additional method stubs
// ---------------------------------------------------------------------------

fun HumanPhysClass.enableUserControl(enable: Boolean): Unit = Unit
fun HumanPhysClass.setTransform(tm: Matrix3D): Unit = Unit
fun HumanPhysClass.getTranslation(): Vector3 = getPosition()
fun HumanPhysClass.isInScene(): Boolean = false
fun HumanPhysClass.remove(): Unit = Unit
fun HumanPhysClass.addToScene(scene: PhysicsSceneClass): Unit = Unit
fun HumanPhysClass.setLadderIndex(index: Int): Unit = Unit
fun HumanPhysClass.setTranslation(pos: Vector3): Unit = Unit

// ---------------------------------------------------------------------------
// MeshClass — classId constant (RenderObjClass sub-type ID)
// ---------------------------------------------------------------------------

// CLASSID_MESH is now RenderObjClass.companion.CLASSID_MESH; keep top-level alias
const val CLASSID_MESH = 1

// MeshClass.getPolygonArray returns array of triangle indices
fun MeshClass.getPolygonCountArray(): IntArray = intArrayOf()

// ---------------------------------------------------------------------------
// SmartGameObj.getOwner() and similar
// ---------------------------------------------------------------------------

fun SmartGameObj.getOwner(): SoldierGameObj? = null
fun SmartGameObj.getName(): String = ""
fun SmartGameObj.getId(): Int = networkId
fun SmartGameObj.getPlayerType(): Int = playerType
fun SmartGameObj.getGlobalSettings(): GlobalSettingsDefClass = GlobalSettingsDefClass
fun SmartGameObj.isGameplayPermitted(): Boolean = true
fun SmartGameObj.peekAnimation(): Animatable3DObjClass? = null
fun SmartGameObj.getTargetingPos(): Vector3 = targeting

// ---------------------------------------------------------------------------
// HarvesterClass — save/load
// ---------------------------------------------------------------------------

fun HarvesterClass.save(csave: ChunkSaveClass): Unit = Unit
fun HarvesterClass.load(cload: ChunkLoadClass): Unit = Unit
fun HarvesterClass.getInstance(): HarvesterClass? = null
fun HarvesterClass.setTiberiumRegion(region: Any?): Unit = Unit
fun HarvesterClass.addMoney(amount: Float): Unit = Unit

// C++: HarvesterClass.getInstance() companion
class HarvesterClassCompanion {
    fun getInstance(): HarvesterClass? = null
}

// ---------------------------------------------------------------------------
// GameObjObserverClass — additional methods
// ---------------------------------------------------------------------------

fun GameObjObserverClass.getOwner(): ScriptableGameObj? = null

// ---------------------------------------------------------------------------
// DefenseObjectClass — save/load
// ---------------------------------------------------------------------------

fun ccr.server.net.DefenseObjectClass.save(csave: ChunkSaveClass): Boolean = true
fun ccr.server.net.DefenseObjectClass.load(cload: ChunkLoadClass): Boolean = true

// ---------------------------------------------------------------------------
// DefinitionMgrClass — findAmmoDefinition
// ---------------------------------------------------------------------------

// DefinitionMgrClass — findAmmoDefinition/findTypedDefinition already on object above

// ---------------------------------------------------------------------------
// AnimCollisionManagerClass — wrap/unwrap
// ---------------------------------------------------------------------------

fun AnimCollisionManagerClass.wrap(): Unit = Unit

// ---------------------------------------------------------------------------
// MoveablePhysClass — additional
// ---------------------------------------------------------------------------

fun MoveablePhysClass.moveContents(delta: Vector3): Unit = Unit

// ---------------------------------------------------------------------------
// PhysicalGameObj helpers
// ---------------------------------------------------------------------------

fun PhysicalGameObj.getTargetingPos(): Vector3 = (this as? ArmedGameObj)?.targeting ?: Vector3()
fun PhysicalGameObj.getOwner(): SoldierGameObj? = null
fun PhysicalGameObj.getName(): String = ""
fun PhysicalGameObj.getId(): Int = networkId
fun PhysicalGameObj.getPlayerType(): Int = playerType
fun PhysicalGameObj.isGameplayPermitted(): Boolean = true
fun PhysicalGameObj.getGlobalSettings(): GlobalSettingsDefClass = GlobalSettingsDefClass

// ---------------------------------------------------------------------------
// AudibleSoundClass — additional
// ---------------------------------------------------------------------------

fun AudibleSoundClass.setPitchFactor(factor: Float): Unit = Unit

// ---------------------------------------------------------------------------
// GameObjReference — save/load
// ---------------------------------------------------------------------------

fun GameObjReference.save(csave: ChunkSaveClass): Unit = Unit
fun GameObjReference.load(cload: ChunkLoadClass): Unit = Unit

// ---------------------------------------------------------------------------
// ActionClass — save/load
// ---------------------------------------------------------------------------

fun ActionClass.save(csave: ChunkSaveClass): Unit = Unit
fun ActionClass.load(cload: ChunkLoadClass): Unit = Unit
fun ActionClass.reset(priority: Int): Unit = Unit
fun ActionClass.attack(params: ActionParamsStruct): Unit = Unit
fun ActionClass.goto(params: ActionParamsStruct): Unit = Unit
fun ActionClass.end(): Unit = Unit

// ---------------------------------------------------------------------------
// StaticNetworkObject (server port)
// ---------------------------------------------------------------------------

// C++: Stats tracking — stub
object StatsManager {
    fun statsAddGameTime(soldierNetId: Int, delta: Float): Unit = Unit
    fun statsSetFinalHealth(soldierNetId: Int, health: Float): Unit = Unit
    fun statsAddVehicleTime(soldierNetId: Int, delta: Float): Unit = Unit
    fun statsAddVehicleDestroyed(soldierNetId: Int): Unit = Unit
    fun statsAddBuildingDestroyed(soldierNetId: Int): Unit = Unit
}

// ---------------------------------------------------------------------------
// Additional constants needed by SoldierGameObj
// ---------------------------------------------------------------------------

const val ANIM_MODE_LOOP_ONCE = 4
const val ANIM_MODE_MANUAL = 5

// C++: ANIMATE_TARGET anim mode
const val ANIMATE_TARGET = ANIM_MODE_TARGET

// C++: ControlClass boolean controls
const val BOOLEAN_DUCK = 10
const val BOOLEAN_SPRINT = 15

// C++: SoldierGameObj collision group
const val SOLDIER_COLLISION_GROUP = 4

// C++: DialogMax
const val DIALOG_MAX = 10

// ---------------------------------------------------------------------------
// VehicleFactoryGameObj — generatingVehicleId
// This is typically a field on VehicleFactoryGameObj; exposed as a stub extension
// ---------------------------------------------------------------------------

// handled in VehicleFactoryGameObj.kt directly

// ---------------------------------------------------------------------------
// StealthEffectClass — save/load
// ---------------------------------------------------------------------------

fun StealthEffectClass.save(csave: ChunkSaveClass): Unit = Unit
fun StealthEffectClass.load(cload: ChunkLoadClass): Unit = Unit

// ---------------------------------------------------------------------------
// LogicalListenerClass — save/load
// ---------------------------------------------------------------------------

fun LogicalListenerClass.save(csave: ChunkSaveClass): Unit = Unit
fun LogicalListenerClass.load(cload: ChunkLoadClass): Unit = Unit

// ---------------------------------------------------------------------------
// PhysicalGameObjDef — needed for ArmedGameObj
// ---------------------------------------------------------------------------
// (This is a real class in ccr.server.defs — just ensure it's accessible via import)

// ---------------------------------------------------------------------------
// ControlClass — additional free function
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// PhysController — set/get move/turn helpers
// ---------------------------------------------------------------------------

fun ccr.physics.PhysController.setMoveForward(v: Float) { moveForward = v }
fun ccr.physics.PhysController.setMoveLeft(v: Float) { moveLeft = v }
fun ccr.physics.PhysController.setMoveUp(v: Float) { moveUp = v }
fun ccr.physics.PhysController.setTurnLeft(v: Float) { turnLeft = v }
fun ccr.physics.PhysController.getMoveForward(): Float = moveForward
fun ccr.physics.PhysController.getMoveLeft(): Float = moveLeft
fun ccr.physics.PhysController.getMoveUp(): Float = moveUp

// ---------------------------------------------------------------------------
// HumanStateClass — additional methods
// ---------------------------------------------------------------------------

fun HumanStateClass.updateWeapon(weapon: WeaponClass?, isChanged: Boolean): Unit = Unit
fun HumanStateClass.startScriptedAnimation(animName: String, looping: Boolean, arg3: Boolean = false): Unit = Unit
fun HumanStateClass.stopScriptedAnimation(): Unit = Unit
fun HumanStateClass.startTransitionAnimation(data: TransitionDataClass?): Unit = Unit
fun HumanStateClass.forceAnimation(animName: String, mode: Int = 0): Unit = Unit
fun HumanStateClass.isSubStateAdjustable(): Boolean = false
fun HumanStateClass.getStateFlag(flag: Int): Boolean = false
fun HumanStateClass.toggleStateFlag(flag: Int): Unit = Unit
fun HumanStateClass.setTurnVelocity(v: Float): Unit = Unit
fun HumanStateClass.getLegMode(): Boolean = false
fun HumanStateClass.networkStateUpdate(): Unit = Unit
fun HumanStateClass.getCollisionBox(): AABoxClass? = null
fun HumanStateClass.isInterruptable(): Boolean = false
fun HumanStateClass.stopConversation(): Unit = Unit
fun HumanStateClass.getNumFrames(): Int = 0

val HumanStateClass.Companion.LAND: Int get() = 18
val HumanStateClass.Companion.LADDER_ENTER_TOP: Int get() = 0
val HumanStateClass.Companion.LADDER_ENTER_BOTTOM: Int get() = 1
val HumanStateClass.Companion.VEHICLE_ENTER: Int get() = 0

// ---------------------------------------------------------------------------
// GlobalSettingsDef — getSoldierCrouchSpeed/getSoldierWalkSpeed/getMpStealth
// ---------------------------------------------------------------------------

// GlobalSettingsDef.getGlobalSettings() — forward to GlobalSettingsDefClass
fun GlobalSettingsDef.getGlobalSettings(): GlobalSettingsDefClass = GlobalSettingsDefClass

fun GlobalSettingsDefClass.getSoldierCrouchSpeed(): Float = 0.5f
fun GlobalSettingsDefClass.getSoldierWalkSpeed(): Float = 0.7f
fun GlobalSettingsDefClass.getMpStealthDistanceHuman(): Float = 20f
fun GlobalSettingsDefClass.getMpStealthDistanceVehicle(): Float = 30f

// ---------------------------------------------------------------------------
// TransitionManager — check method
// ---------------------------------------------------------------------------

fun TransitionManager.check(soldier: SoldierGameObj, actionTriggered: Boolean): Boolean = false
fun TransitionManager.startTransition(soldier: SoldierGameObj, data: TransitionDataClass?, vehicleId: Int, seatIndex: Int): Unit = Unit

// ---------------------------------------------------------------------------
// HumanPhysClass — getHeading/setHeading
// ---------------------------------------------------------------------------

fun HumanPhysClass.getHeading(): Float = 0f
fun HumanPhysClass.setHeading(heading: Float): Unit = Unit
fun HumanPhysClass.getContactSurfaceType(): Int = 0
fun HumanPhysClass.getVelocity(): Vector3 = Vector3()
fun HumanPhysClass.setCollisionGroup(group: Int): Unit = Unit

// ---------------------------------------------------------------------------
// PhysClass — setCollisionGroup / getObserver
// ---------------------------------------------------------------------------

fun PhysClass.setCollisionGroup(group: Int): Unit = Unit
fun PhysClass.getObserver(): Any? = null

// ---------------------------------------------------------------------------
// ArmorWarheadManager — getSpecialDamageExplosion / getSpecialDamageScale
// ---------------------------------------------------------------------------

fun ArmorWarheadManager.getSpecialDamageExplosion(mode: Int): String = ""
fun ArmorWarheadManager.getSpecialDamageScale(mode: Int): Float = 1f

// ---------------------------------------------------------------------------
// SoldierGameObj — additional methods
// ---------------------------------------------------------------------------

fun SoldierGameObj.wantsPowerups(): Boolean = true
fun SoldierGameObj.networkStateUpdate(): Unit = Unit
fun SoldierGameObj.peekCarrierObject(): Any? = null
fun SoldierGameObj.getVisTable(): Any? = null
fun SoldierGameObj.getOuchType(mode: Int): Int = 0
fun SoldierGameObj.getRenderObjNameFromFilename(filename: String): String =
    WW3DAssetManager.getRenderObjNameFromFilename(filename)
fun SoldierGameObj.getElectrocutionEffect(): TransitionEffectClass? = null
fun SoldierGameObj.getCanSnipe(): Boolean = false

// ---------------------------------------------------------------------------
// WeaponClass — additional
// ---------------------------------------------------------------------------

fun WeaponClass.getRange(): Float = 0f
fun WeaponClass.getCanSnipe(): Boolean = false
fun WeaponClass.getSpecialDamageExplosion(): String = ""
fun WeaponClass.getSpecialDamageScale(): Float = 1f
val WeaponClass.definitionId: Int get() = 0

// ---------------------------------------------------------------------------
// WeaponBagClass — isChanged/resetChanged (already in WeaponBagClass.kt — no extensions needed)
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// AnimControlClass and subtypes — additional
// ---------------------------------------------------------------------------

fun HumanAnimControlClass.generateIdleAnimation(delayMs: Int, blendTime: Float): Unit = Unit

fun RenderObjClass.setAnimation(anim: Any?, blendTime: Float, mode: Int): Unit = Unit
fun RenderObjClass.peekAnimation(): HAnimClass? = null
fun RenderObjClass.isInScene(): Boolean = false
fun RenderObjClass.remove(): Unit = Unit
fun RenderObjClass.addRenderObject(child: RenderObjClass): Unit = Unit
fun RenderObjClass.enableRemoveOnComplete(enable: Boolean): Unit = Unit
fun RenderObjClass.setHidden(hidden: Boolean): Unit = Unit
fun RenderObjClass.setTransform(tm: Matrix3D): Unit = Unit
fun RenderObjClass.getTranslation(): Vector3 = Vector3()

// ---------------------------------------------------------------------------
// WWMath — wrap function
// ---------------------------------------------------------------------------

fun WWMath.wrap(v: Float, min: Float, max: Float): Float {
    val range = max - min
    if (range <= 0f) return min
    var result = v
    while (result < min) result += range
    while (result > max) result -= range
    return result
}

const val DEG_TO_RADF_NEG_180 = -kotlin.math.PI.toFloat()
const val DEG_TO_RADF_POS_180 =  kotlin.math.PI.toFloat()

// ---------------------------------------------------------------------------
// ConversationMgrClass — getConversation, findObject
// ---------------------------------------------------------------------------

fun ConversationMgrClass.getConversation(id: Int): Any? = null
fun ConversationMgrClass.findObject(id: Int): Any? = null

// ---------------------------------------------------------------------------
// ActiveConversationClass — additional methods
// ---------------------------------------------------------------------------

fun ActiveConversationClass.isBusy(): Boolean = false
fun ActiveConversationClass.play(obj: Any?): Unit = Unit
fun ActiveConversationClass.getDuration(): Float = 0f
fun ActiveConversationClass.getConversation(): Any? = null
fun ActiveConversationClass.getParameters(): Any? = null

// ---------------------------------------------------------------------------
// DynamicSpeechAnimClass — constructor and methods
// ---------------------------------------------------------------------------

fun DynamicSpeechAnimClass.play(sound: AudibleSoundClass?): Unit = Unit
fun DynamicSpeechAnimClass.getDuration(): Float = 0f

// ---------------------------------------------------------------------------
// ScriptZoneGameObj — isInZone
// ---------------------------------------------------------------------------

fun ScriptZoneGameObj.isInZone(obj: PhysicalGameObj, box: Any?): Boolean = false

// ---------------------------------------------------------------------------
// SoldierGameObj — applyControl / generateControl helpers
// ---------------------------------------------------------------------------

fun SoldierGameObj.isOnLadder(): Boolean = false
fun SoldierGameObj.isCrouched(): Boolean = false
fun SoldierGameObj.isSniping(): Boolean = false
fun SoldierGameObj.isHumanControlled(): Boolean = controlOwner > 0
fun SoldierGameObj.getTurnRate(): Float = 1f
fun SoldierGameObj.getBullseyePosition(): Vector3 = getPosition()
fun SoldierGameObj.getTransform(): Matrix3D = Matrix3D.IDENTITY
fun SoldierGameObj.clearControl(): Unit = Unit

// ---------------------------------------------------------------------------
// SoldierGameObj — adjustSkeleton
// ---------------------------------------------------------------------------

fun SoldierGameObj.adjustSkeleton(height: Float, width: Float): Unit = Unit

// ---------------------------------------------------------------------------
// SoldierGameObj helpers — miscellaneous
// ---------------------------------------------------------------------------

fun SoldierGameObj.prepareSpeechFramework(): Unit = Unit
fun SoldierGameObj.resetRenderObjs(): Unit = Unit
fun SoldierGameObj.isControlledByMe(): Boolean = false
fun SoldierGameObj.isSafeToDisableGhostCollision(pos: Vector3): Boolean = true
fun SoldierGameObj.enableGhostCollision(enable: Boolean): Unit = Unit
fun SoldierGameObj.updateLockedFacing(): Unit = Unit
fun SoldierGameObj.handleLegs(): Unit = Unit
fun SoldierGameObj.getState(): Int = HumanStateClass.UPRIGHT
fun SoldierGameObj.isTargetable(): Boolean = true

// ---------------------------------------------------------------------------
// SoldierObserverClass / insertObserver — stub
// ---------------------------------------------------------------------------

fun SoldierGameObj.insertObserver(obs: GameObjObserverClass): Unit = Unit

// ---------------------------------------------------------------------------
// SoldierGameObj — transition completion
// ---------------------------------------------------------------------------

val TransitionCompletionDataStruct.type: Int get() = 0
val TransitionCompletionDataStruct.vehicle: VehicleGameObj? get() = null

// ---------------------------------------------------------------------------
// ScriptableGameObj — additional
// ---------------------------------------------------------------------------

fun ScriptableGameObj.insertObserver(obs: GameObjObserverClass): Unit = Unit

// ---------------------------------------------------------------------------
// PhysicalGameObj — additional helpers
// ---------------------------------------------------------------------------

fun PhysicalGameObj.peekHumanPhys(): HumanPhysClass? = null
fun PhysicalGameObj.peekPhysicalObject(): PhysClass? = null
fun PhysicalGameObj.getPosition(): Vector3 = Vector3()
fun PhysicalGameObj.setWeaponModel(model: RenderObjClass?): Unit = Unit
fun PhysicalGameObj.getTransform(): Matrix3D = Matrix3D.IDENTITY
fun PhysicalGameObj.setTransform(tm: Matrix3D): Unit = Unit

// ---------------------------------------------------------------------------
// CLASSID constants
// ---------------------------------------------------------------------------

const val CLASSID_DEF_EXPLOSION = 0x4004
const val CLASSID_DEF_WEAPON    = 0x3005

// ---------------------------------------------------------------------------
// VehicleGameObj — additional
// ---------------------------------------------------------------------------

fun VehicleGameObj.removeOccupant(soldier: SoldierGameObj): Unit = Unit
fun VehicleGameObj.toggleDriverIsGunner(): Unit = Unit
fun VehicleGameObj.getAnimState(): Int = 0
fun VehicleGameObj.copyFrom(other: VehicleGameObj): Unit = Unit
fun VehicleGameObj.computeApproximateRideHeight(): Float = 0f
fun VehicleGameObj.getObj(): Any? = null
fun VehicleGameObj.translated(): Boolean = false
fun VehicleGameObj.onVehicleDelivered(): Unit = Unit
fun VehicleGameObj.getOrientation(): Float = 0f
fun VehicleGameObj.normalize(): Float = 0f
fun VehicleGameObj.networkInterpolateStateUpdate(): Unit = Unit
fun VehicleGameObj.asStaticAnimPhysClass(): StaticAnimPhysClass? = null
fun VehicleGameObj.getAnimationManager(): Any? = null

// ---------------------------------------------------------------------------
// AirStripGameObj — endTimer field
// ---------------------------------------------------------------------------
// AirStripGameObj uses VehicleFactoryGameObj.endTimer directly (no separate field)

// ---------------------------------------------------------------------------
// HumanPhysClass — peekModel
// ---------------------------------------------------------------------------

fun HumanPhysClass.peekModel(): RenderObjClass? = null

// ---------------------------------------------------------------------------
// SoldierGameObj — getDescription override annotation fix
// ---------------------------------------------------------------------------

fun SoldierGameObj.getDescription(): String = ""

// GameObjManager.removeStar / addStar already exist in GameObjManager object

// PhysicsSceneClass.removeObject(PhysClass?) is already a member method

// ---------------------------------------------------------------------------
// Matrix3D.IDENTITY constant
// ---------------------------------------------------------------------------

val Matrix3D.Companion.IDENTITY: Matrix3D get() = Matrix3D()

// ---------------------------------------------------------------------------
// SurfaceEffectsManager — 4-argument updatePersistantEmitter with Matrix3D
// ---------------------------------------------------------------------------

fun SurfaceEffectsManager.updatePersistantEmitter(
    emitter: PersistantSurfaceEmitterClass?,
    surfaceType: Int,
    hitterType: Int,
    tm: Matrix3D
): Unit = Unit

// ---------------------------------------------------------------------------
// HumanStateClass — additional methods
// ---------------------------------------------------------------------------

// C++: HumanStateClass::startTransitionAnimation(animName, looping)
fun HumanStateClass.startTransitionAnimation(animName: String, looping: Boolean): Unit = Unit

// C++: HumanStateClass::forceAnimation(animName, looping)
fun HumanStateClass.forceAnimation(animName: String, looping: Boolean): Unit = Unit

// C++: HumanStateClass::updateAiming(tilt, roll)
fun HumanStateClass.updateAiming(tilt: Float, roll: Float): Unit = Unit

// C++: HumanStateClass::isLocked()
fun HumanStateClass.isLocked(): Boolean = false

// C++: HumanStateClass::resetLoiterDelay
fun HumanStateClass.resetLoiterDelay(): Unit = Unit

// C++: HumanStateClass::setLoitersAllowed
fun HumanStateClass.setLoitersAllowed(allowed: Boolean): Unit = Unit

// ---------------------------------------------------------------------------
// ArmorWarheadManager — SPECIAL_DAMAGE_TYPE_SUPER_FIRE
// ---------------------------------------------------------------------------

val ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_SUPER_FIRE: Int get() = 6

// ---------------------------------------------------------------------------
// MoveablePhysClass — asElevatorPhysClass
// ---------------------------------------------------------------------------

open class ElevatorPhysClass : MoveablePhysClass()
fun MoveablePhysClass.asElevatorPhysClass(): ElevatorPhysClass? = this as? ElevatorPhysClass
fun PhysClass.peekCarrierObject(): MoveablePhysClass? = null

// ---------------------------------------------------------------------------
// PVS / visibility table — getBit
// ---------------------------------------------------------------------------

class VisTableClass {
    fun getBit(visObjectId: Int): Boolean = false
}

fun PhysicsSceneClass.getVisTable(position: Vector3): VisTableClass? = null

// ---------------------------------------------------------------------------
// CollisionMath — OUTSIDE sentinel value
// ---------------------------------------------------------------------------

val CollisionMath.OUTSIDE: ccr.physics.collision.OverlapType get() = ccr.physics.collision.OverlapType.OUTSIDE

// ---------------------------------------------------------------------------
// HUDInfo — getInfoObject, setActionStatusValue, displayActionStatusBar
// ---------------------------------------------------------------------------

fun HUDInfo.getInfoObject(): DamageableGameObj? = null
fun HUDInfo.setActionStatusValue(percent: Float): Unit = Unit
fun HUDInfo.displayActionStatusBar(show: Boolean): Unit = Unit

// ---------------------------------------------------------------------------
// SmartGameObj — getPlayerData
// ---------------------------------------------------------------------------

fun SmartGameObj.getPlayerData(): Any? = null

// ---------------------------------------------------------------------------
// DefenseObjectClass — isRepair, getSkin
// ---------------------------------------------------------------------------

fun ccr.server.net.DefenseObjectClass.isRepair(offense: OffenseObjectClass): Boolean = false
fun ccr.server.net.DefenseObjectClass.getSkin(): Int = 0

// ---------------------------------------------------------------------------
// OffenseObjectClass — getWarhead
// ---------------------------------------------------------------------------

fun OffenseObjectClass.getWarhead(): Int = 0
fun ArmorWarheadManager.getSpecialDamageType(warhead: Int): Int = 0
fun ArmorWarheadManager.getSpecialDamageProbability(warhead: Int): Float = 0f
fun ArmorWarheadManager.isSkinImpervious(sdType: Int, skin: Int): Boolean = false

// ---------------------------------------------------------------------------
// PlayerDataClass — stats kill methods
// ---------------------------------------------------------------------------

fun PlayerDataClass.statsAddHeadHit(): Unit = Unit
fun PlayerDataClass.statsAddHeadShot(): Unit = Unit
fun PlayerDataClass.statsAddTorsoHit(): Unit = Unit
fun PlayerDataClass.statsAddTorsoShot(): Unit = Unit
fun PlayerDataClass.statsAddArmHit(): Unit = Unit
fun PlayerDataClass.statsAddArmShot(): Unit = Unit
fun PlayerDataClass.statsAddLegHit(): Unit = Unit
fun PlayerDataClass.statsAddLegShot(): Unit = Unit
fun PlayerDataClass.statsAddCrotchHit(): Unit = Unit
fun PlayerDataClass.statsAddCrotchShot(): Unit = Unit
fun PlayerDataClass.statsAddAllyKilled(): Unit = Unit
fun PlayerDataClass.statsAddEnemyKilled(): Unit = Unit
fun PlayerDataClass.statsAddKillFromVehicle(): Unit = Unit

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
// BonesManager — getBoneDamageScale
// ---------------------------------------------------------------------------

fun BonesManager.getBoneDamageScale(boneName: String): Float = 1f

// ---------------------------------------------------------------------------
// CombatMaterialEffectManager — getHealthEffect, getElectrocutionEffect
// ---------------------------------------------------------------------------

// MaterialEffectClass is a concrete subtype of TransitionEffectClass
class MaterialEffectClass : TransitionEffectClass()

fun CombatMaterialEffectManager.getHealthEffect(): TransitionEffectClass? = null
fun CombatMaterialEffectManager.getElectrocutionEffect(): TransitionEffectClass? = null
fun MoveablePhysClass.addEffectToMe(effect: TransitionEffectClass): Unit = Unit
fun MoveablePhysClass.removeEffectFromMe(effect: TransitionEffectClass): Unit = Unit

// ---------------------------------------------------------------------------
// WeaponClass — additional methods
// ---------------------------------------------------------------------------

fun WeaponClass.isModelUpdateNeeded(): Boolean = false
fun WeaponClass.resetModelUpdate(): Unit = Unit
fun WeaponClass.resetAnimUpdate(): Unit = Unit
fun WeaponClass.getAnimState(): Int = 0
fun WeaponClass.getModelName(): String? = null
fun WeaponClass.setModel(model: RenderObjClass): Unit = Unit
fun WeaponClass.getNextWeapon(): WeaponClass? = null
fun WeaponClass.getBackModelName(): String? = null
fun WeaponClass.getName(): String = ""
fun WeaponClass.getConversation(): Int = 0
fun WeaponClass.isReloading(): Boolean = false

// ---------------------------------------------------------------------------
// WeaponBagClass — getNextWeapon
// ---------------------------------------------------------------------------

fun WeaponBagClass.getNextWeapon(): WeaponClass? = null

// ---------------------------------------------------------------------------
// RenderObjClass — getContainer
// ---------------------------------------------------------------------------

fun RenderObjClass.getContainer(): RenderObjClass? = null

// ---------------------------------------------------------------------------
// WWMath — fastAsin, atan2, lerp, sqrt
// ---------------------------------------------------------------------------

fun WWMath.fastAsin(v: Float): Float = kotlin.math.asin(v.coerceIn(-1f, 1f))
fun WWMath.atan2(y: Float, x: Float): Float = kotlin.math.atan2(y, x)
fun WWMath.lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
fun WWMath.sqrt(v: Float): Float = kotlin.math.sqrt(v)

const val DEG_TO_RADF_NEG_90 = -kotlin.math.PI.toFloat() / 2f
const val DEG_20_RAD = kotlin.math.PI.toFloat() * 20f / 180f

// ---------------------------------------------------------------------------
// WeaponDefinitionClass (ccr.server.defs) — aGiveWeaponsWeapon, getName
// ---------------------------------------------------------------------------

val ccr.server.defs.WeaponDefinitionClass.aGiveWeaponsWeapon: Boolean get() = false
fun ccr.server.defs.WeaponDefinitionClass.getName(): String = ""

// ---------------------------------------------------------------------------
// HumanPhysClass — getNormalizedSpeed, setNormalizedSpeed
// ---------------------------------------------------------------------------

fun HumanPhysClass.getNormalizedSpeed(): Float = 1f
fun HumanPhysClass.setNormalizedSpeed(speed: Float): Unit = Unit

// ---------------------------------------------------------------------------
// ActionParamsClass — moveLocation, moveArrivedDistance (now real var fields on ActionParamsStruct)
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// FreeRandom — getFloat(min, max)
// ---------------------------------------------------------------------------

fun FreeRandom.getFloat(min: Float, max: Float): Float = min + kotlin.random.Random.nextFloat() * (max - min)

// ---------------------------------------------------------------------------
// WWAudioClass — createContinuousSound, addToScene, removeFromScene, getDuration
// ---------------------------------------------------------------------------

fun WWAudioClass.createContinuousSound(soundDefId: Int): AudibleSoundClass? = null
fun AudibleSoundClass.addToScene(): Unit = Unit
fun AudibleSoundClass.removeFromScene(): Unit = Unit
fun AudibleSoundClass.getDuration(): Float = 0f
fun AudibleSoundClass.setTransform(tm: Matrix3D): Unit = Unit
fun AudibleSoundClass.play(): Unit = Unit

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
// BaseControllerClass — findBase static method + beacon notifications
// ---------------------------------------------------------------------------

fun BaseControllerClass.onBeaconArmed(beacon: BeaconGameObj): Unit = Unit
fun BaseControllerClass.onBeaconDisarmed(beacon: BeaconGameObj): Unit = Unit
fun BaseControllerClass.onBeaconWarning(beacon: BeaconGameObj): Unit = Unit
fun BaseControllerClass.getBeaconZone(): AABoxClass = AABoxClass()

// ---------------------------------------------------------------------------
// CollisionMath — overlapTest(AABoxClass, Vector3) for point-in-box test
// ---------------------------------------------------------------------------

fun CollisionMath.overlapTest(box: AABoxClass, point: Vector3): ccr.physics.collision.OverlapType =
    ccr.physics.collision.OverlapType.OUTSIDE

fun CollisionMath.overlapTest(box: ccr.server.defs.OBBoxClass, point: Vector3): ccr.physics.collision.OverlapType =
    ccr.physics.collision.OverlapType.OUTSIDE

fun CollisionMath.overlapTest(box: AABoxClass, box2: AABoxClass): ccr.physics.collision.OverlapType =
    ccr.physics.collision.OverlapType.OUTSIDE

// ---------------------------------------------------------------------------
// SimpleGameObj — setPlayerType (delegates to playerType field)
// ---------------------------------------------------------------------------

fun SimpleGameObj.setPlayerType(type: Int) { playerType = type }

// ---------------------------------------------------------------------------
// BuildingGameObj — findClosestPoly with dist2 output array
// ---------------------------------------------------------------------------

fun BuildingGameObj.findClosestPoly(pos: Vector3, dist2Out: FloatArray): Unit { dist2Out[0] = Float.MAX_VALUE }

// ---------------------------------------------------------------------------
// ExplosionDefinitionClass — stub
// ---------------------------------------------------------------------------

class ExplosionDefinitionClass {
    val damageRadius: Float = 0f
    val damageStrength: Float = 0f
    val damageWarhead: Int = 0
}

// ---------------------------------------------------------------------------
// CombatManager — getMessageWindow, isServer, doesBeaconPlacementEndsGame
// ---------------------------------------------------------------------------

fun CombatManager.getMessageWindow(): MessageWindowClass? = null
fun CombatManager.isServer(): Boolean = true
fun CombatManager.doesBeaconPlacementEndsGame(): Boolean = false

// MessageWindowClass — addMessage overload with all params
fun MessageWindowClass.addMessage(message: String, color: Vector3, extra: Any?, duration: Float): Unit = Unit

// ---------------------------------------------------------------------------
// PhysicsSceneClass — addRenderObject
// ---------------------------------------------------------------------------

fun PhysicsSceneClass.addRenderObject(obj: RenderObjClass): Unit = Unit

// ---------------------------------------------------------------------------
// ConversationMgrClass — startConversation with obj, id, immediate
// ---------------------------------------------------------------------------

fun ConversationMgrClass.startConversation(obj: ScriptableGameObj, conversationId: Int, immediate: Boolean): ActiveConversationClass? = null

// ActiveConversationClass — isInterruptable, stopConversation
fun ActiveConversationClass.isInterruptable(): Boolean = false
fun ActiveConversationClass.stopConversation(reason: Int): Unit = Unit

const val ACTION_COMPLETE_CONVERSATION_INTERRUPTED = 3

// ---------------------------------------------------------------------------
// SoldierGameObj — AI state constants
// ---------------------------------------------------------------------------

const val AI_STATE_SEARCH          = 3
const val AI_STATE_SECONDARY_IDLE  = 4

// Soldier innate event bit masks
const val SOLDIER_INNATE_EVENT_BULLET_HEARD    = 0x01
const val SOLDIER_INNATE_EVENT_GUNSHOT_HEARD   = 0x02
const val SOLDIER_INNATE_EVENT_FOOTSTEP_HEARD  = 0x04
const val SOLDIER_INNATE_EVENT_ENEMY_SEEN      = 0x08
const val SOLDIER_INNATE_ACTIONS               = 0x10

const val SOLDIER_HEIGHT_FOR_LOOK = 1.0f
const val SOLDIER_GHOST_COLLISION_GROUP = 5
const val HUMAN_HALF_HEIGHT = 0.9f
val PERSONAL_SPACE_BOX_SIZE = Vector3(0.4f, 0.4f, HUMAN_HALF_HEIGHT)

// Surface type constants
const val SURFACE_TYPE_DEFAULT         = 0
const val SURFACE_TYPE_UNDERWATER_DIRT = 13

// Bullet collision group
const val BULLET_COLLISION_GROUP = 2

// Tilt speed constant
const val TILT_DOWN_SPEED = 2f

// Weapon hold styles
const val WEAPON_HOLD_STYLE_C4     = 3
const val WEAPON_HOLD_STYLE_BEACON = 4

// Dialog triggers
const val DIALOG_ON_TAKE_DAMAGE_FROM_ENEMY  = 1
const val DIALOG_ON_TAKE_DAMAGE_FROM_FRIEND = 2

// Misc math constants
const val WWMATH_EPSILON = 0.001f

// ---------------------------------------------------------------------------
// DialogueClass — getConversation
// ---------------------------------------------------------------------------

fun DialogueClass.getConversation(): Int = 0

// ---------------------------------------------------------------------------
// ActionClass — getParameters, isBusy
// ---------------------------------------------------------------------------

fun ActionClass.getParameters(): ActionParamsStruct = ActionParamsStruct()
fun ActionClass.isBusy(): Boolean = false

// ---------------------------------------------------------------------------
// WW3DAssetManager — createRenderObjFromFilename as SoldierGameObj member
// (SoldierGameObj calls createRenderObjFromFilename as a local method)
// ---------------------------------------------------------------------------

fun SoldierGameObj.createRenderObjFromFilename(filename: String): RenderObjClass? =
    WW3DAssetManager.createRenderObjFromFilename(filename)

// ---------------------------------------------------------------------------
// ObjectLibraryManager — createObject returns ScriptableGameObj
// ---------------------------------------------------------------------------

fun ObjectLibraryManager.createObject(defId: Int): ScriptableGameObj? = null
fun ObjectLibraryManager.createObject(name: String): ScriptableGameObj? = null

// ---------------------------------------------------------------------------
// BeaconGameObj — getControl for SoldierGameObj
// ---------------------------------------------------------------------------

fun SoldierGameObj.getControl(): ControlClass = ControlClass()

// ---------------------------------------------------------------------------
// HumanPhysClass — networkStateUpdate with position/velocity, setPosition, etc.
// ---------------------------------------------------------------------------

fun HumanPhysClass.networkStateUpdate(position: Vector3, velocity: Vector3): Unit = Unit
fun HumanPhysClass.setPosition(pos: Vector3): Unit = Unit
fun HumanPhysClass.setVelocity(vel: Vector3): Unit = Unit
fun HumanPhysClass.canTeleport(tm: Matrix3D): Boolean = false
fun HumanPhysClass.getCollisionBox(): AABoxClass = AABoxClass(Vector3(), Vector3(0.3f, 0.3f, 1f))


// TransitionInstanceClass companion object with end() is defined in the class itself above

// ---------------------------------------------------------------------------
// FreeRandom — zero-arg getFloat()
// ---------------------------------------------------------------------------

fun FreeRandom.getFloat(): Float = kotlin.random.Random.nextFloat()

// ---------------------------------------------------------------------------
// GlobalSettingsDefClass — getDeathSoundId
// ---------------------------------------------------------------------------

fun GlobalSettingsDefClass.getDeathSoundId(): Int = 0

// ---------------------------------------------------------------------------
// SoldierGameObjDef (ccr.server.defs) — killedExplosion property
// ---------------------------------------------------------------------------

val ccr.server.defs.SoldierGameObjDef.killedExplosion: Int get() = 0

// ---------------------------------------------------------------------------
// WeaponBagClass — selectWeaponId, deselect are now methods in WeaponBagClass.kt
// (extension stubs removed to avoid conflicting overloads)
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Matrix3D — times(Vector3) operator for C++ Matrix3D * Vector3
// ---------------------------------------------------------------------------

operator fun Matrix3D.times(v: Vector3): Vector3 = Vector3(
    m00 * v.x + m01 * v.y + m02 * v.z + m03,
    m10 * v.x + m11 * v.y + m12 * v.z + m13,
    m20 * v.x + m21 * v.y + m22 * v.z + m23,
)

// ---------------------------------------------------------------------------
// Vector3 — companion dotProduct
// ---------------------------------------------------------------------------

fun Vector3.Companion.dotProduct(a: Vector3, b: Vector3): Float = a.x * b.x + a.y * b.y + a.z * b.z

// ---------------------------------------------------------------------------
// VehicleGameObj — getSquishVelocity
// ---------------------------------------------------------------------------

fun VehicleGameObj.getSquishVelocity(): Float = 3f

// ---------------------------------------------------------------------------
// SoldierGameObj — reInit overload for SoldierGameObjDef (defs alias)
// ---------------------------------------------------------------------------

fun SoldierGameObj.reInit(def: ccr.server.defs.SoldierGameObjDef): Unit = Unit

// ---------------------------------------------------------------------------
// VehicleGameObj — setPlayerType (already has playerType var; need explicit setter)
// ---------------------------------------------------------------------------

fun VehicleGameObj.setPlayerType(type: Int) { playerType = type }

// ---------------------------------------------------------------------------
// ControlClass — copyFrom
// ---------------------------------------------------------------------------

fun ControlClass.copyFrom(other: ControlClass): Unit = Unit

// ---------------------------------------------------------------------------
// Quaternion — normalize (in-place, returns self for chaining)
// ---------------------------------------------------------------------------

fun ccr.math.Quaternion.normalize(): ccr.math.Quaternion = normalized()

// ---------------------------------------------------------------------------
// BaseControllerClass — onVehicleDelivered(VehicleGameObj)
// ---------------------------------------------------------------------------

fun BaseControllerClass.onVehicleDelivered(vehicle: VehicleGameObj): Unit = Unit

// ---------------------------------------------------------------------------
// AnimControlClass — getNumFrames (for anim.getNumFrames() in SoldierGameObj)
// ---------------------------------------------------------------------------

fun AnimControlClass.getNumFrames(): Int = 0

// ---------------------------------------------------------------------------
// PhysAABoxIntersectionTestClass — constructor with nullable AABoxClass
// ---------------------------------------------------------------------------

fun PhysAABoxIntersectionTestClass(box: AABoxClass?, collisionGroup: Int, collisionType: Int): PhysAABoxIntersectionTestClass =
    PhysAABoxIntersectionTestClass(box ?: AABoxClass(), collisionGroup, collisionType)

// ---------------------------------------------------------------------------
// DamageableGameObj — isEnemy with nullable param
// ---------------------------------------------------------------------------

fun DamageableGameObj.isEnemy(obj: DamageableGameObj?): Boolean =
    if (obj == null) false else isEnemy(obj)

// ---------------------------------------------------------------------------
// SmartGameObj / ActionClass — getAction returning ActionClass
// ---------------------------------------------------------------------------

fun SmartGameObj.getActionObj(): ActionClass? = null

