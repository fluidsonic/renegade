package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.OBBox as MathOBBox
import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.static.StaticPhysClass
import ccr.physics.scene.PhysicsScene
import ccr.physics.Animatable3DObjClass
import ccr.physics.HAnimClass
import ccr.physics.HAnimComboClass
import ccr.physics.HTreeClass
import ccr.physics.MeshClass
import ccr.physics.RenderObjClass
import ccr.server.defs.Dialogue
import ccr.server.level.ChunkIds

// Audio types — real implementations live in ccr.server.audio; import here for net-package usage.
typealias AudibleSoundClass  = ccr.server.audio.AudibleSoundClass
typealias Sound3DClass       = ccr.server.audio.Sound3DClass
typealias WWAudioClass       = ccr.server.audio.WWAudioClass

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
    fun readSphere(): ccr.math.Sphere = TODO("stub")
    fun readOBBox(): ccr.server.defs.OBBoxClass = TODO("stub")
    fun skip(): Unit = TODO("stub")
    fun skipChunk(): Unit = TODO("stub")
    fun load(target: Any): Boolean = TODO("stub")
}

// ---------------------------------------------------------------------------
// RenderObjClass, MeshClass, Animatable3DObjClass, CompositeRenderObjClass
// are defined in ccr.physics (imported above).
// ---------------------------------------------------------------------------

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
    fun peekAnimation(): HAnimClass? = null
    fun peekModel(): RenderObjClass? = null
    fun resetAnimUpdate(): Unit = Unit
    /** C++: char AnimControlClass::Get_Skeleton() — returns skeleton type character ('V' = vehicle, etc). */
    open fun getSkeleton(): Char = ' '
    /** C++: virtual void AnimControlClass::Set_Anim_Speed_Scale(float speed) — multiplier on playback rate. */
    open fun setAnimSpeedScale(speed: Float): Unit = Unit
    /** C++: virtual float AnimControlClass::Get_Progress() — returns 0..1 playback completion. */
    open fun getProgress(): Float = 0f
    /** C++: void AnimControlClass::Get_Information(StringClass& string) — appends debug info. */
    open fun getInformation(sb: StringBuilder): Unit = Unit
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
    fun save(csave: ChunkSaveClass): Unit = Unit
    fun load(cload: ChunkLoadClass): Unit = Unit
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
    // C++: TransitionManager::Check(SoldierGameObj*, bool) — server-side action/transition check
    fun check(soldier: SoldierGameObj, action: Boolean): Boolean = false
}

// C++: DialogueOptionClass — single option within a DialogueClass
class DialogueOptionClass(
    var weight: Float = 1f,
    var conversationId: Int = 0,
) {
    fun save(csave: ChunkSaveClass) {
        csave.beginChunk(CHUNKID_OPTION_VARIABLES)
        csave.writeMicroChunk(VARID_WEIGHT, weight)
        csave.writeMicroChunk(VARID_CONVERSATION_ID, conversationId)
        csave.endChunk()
    }

    fun load(cload: ChunkLoadClass) {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_OPTION_VARIABLES.toInt() -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            VARID_WEIGHT -> weight = cload.readFloat()
                            VARID_CONVERSATION_ID -> conversationId = cload.readInt()
                        }
                        cload.closeMicroChunk()
                    }
                }
            }
            cload.closeChunk()
        }
    }

    companion object {
        private const val CHUNKID_OPTION_VARIABLES = 0x08040528u
        private const val VARID_WEIGHT = 0
        private const val VARID_CONVERSATION_ID = 2
    }
}

// C++: DialogueClass — NPC dialogue entry with weighted conversation options
class DialogueClass {
    val optionList: MutableList<DialogueOptionClass> = mutableListOf()
    var silenceWeight: Float = 1f

    fun copyFrom(src: DialogueClass) {
        silenceWeight = src.silenceWeight
        optionList.clear()
        for (option in src.optionList) {
            optionList.add(DialogueOptionClass(option.weight, option.conversationId))
        }
    }

    fun getConversation(): Int {
        var convId = 0
        var total = silenceWeight
        for (option in optionList) {
            total += option.weight
        }
        var value = kotlin.random.Random.nextFloat() * total
        var count = silenceWeight
        for (option in optionList) {
            if (value <= count) break
            convId = option.conversationId
            count += option.weight
        }
        return convId
    }

    fun save(csave: ChunkSaveClass) {
        csave.beginChunk(CHUNKID_DIALOGUE_VARIABLES)
        csave.writeMicroChunk(VARID_DIALOGUE_SILENCE, silenceWeight)
        csave.endChunk()
        for (option in optionList) {
            csave.beginChunk(CHUNKID_DIALOGUE_OPTION)
            option.save(csave)
            csave.endChunk()
        }
    }

    fun load(cload: ChunkLoadClass) {
        optionList.clear()
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_DIALOGUE_VARIABLES.toInt() -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            VARID_DIALOGUE_SILENCE -> silenceWeight = cload.readFloat()
                        }
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_DIALOGUE_OPTION.toInt() -> {
                    val option = DialogueOptionClass()
                    option.load(cload)
                    optionList.add(option)
                }
            }
            cload.closeChunk()
        }
    }

    companion object {
        private const val CHUNKID_DIALOGUE_VARIABLES = 0x08040529u
        private const val CHUNKID_DIALOGUE_OPTION = 0x0804052Au
        private const val VARID_DIALOGUE_SILENCE = 0

        fun fromData(data: Dialogue): DialogueClass {
            val dlg = DialogueClass()
            dlg.silenceWeight = data.silenceWeight
            for (opt in data.options) {
                dlg.optionList.add(DialogueOptionClass(opt.weight, opt.conversationId))
            }
            return dlg
        }
    }
}

// C++: DynamicSpeechAnimClass — animated speech/emote on soldier
class DynamicSpeechAnimClass(skeletonName: String = "") {
    fun update(dt: Float): Unit = TODO("stub")
    fun isActive(): Boolean = TODO("stub")
    fun generateIdleAnimation(param1: Int, param2: Float): Unit = Unit
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
    // C++: bool Is_Interruptable() — true if conversation can be interrupted by state changes
    fun isInterruptable(): Boolean = false
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

// C++: ConversationMgrClass (conversationmgr.h)
object ConversationMgrClass {
    fun findConversation(name: String): Any? = null
    // C++: Start_Conversation(SmartGameObj* obj, int conversation_id, bool is_interruptable)
    fun startConversation(obj: Any?, conversationId: Int, isInterruptable: Boolean = true): ActiveConversationClass? = null
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

// C++: BonesManager
object BonesManager {
    fun findBone(boneId: Int): Any? = null
    // C++: BonesManager::Get_Bone_Damage_Scale(boneName) — returns damage multiplier for a bone zone
    fun getBoneDamageScale(boneName: String): Float = 1f
}

// C++: CombatMaterialEffectManager
object CombatMaterialEffectManager {
    fun getSpawnEffect(): MaterialEffectClass? = null
    fun getDeathEffect(): MaterialEffectClass = MaterialEffectClass()
    // C++: CombatMaterialEffectManager::Get_Electrocution_Effect()
    fun getElectrocutionEffect(): MaterialEffectClass = MaterialEffectClass()
    // C++: CombatMaterialEffectManager::Get_Health_Effect()
    fun getHealthEffect(): MaterialEffectClass = MaterialEffectClass()
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

// C++: WWMath utilities
object WWMath {
    fun clamp(v: Float, min: Float, max: Float): Float = v.coerceIn(min, max)
    // C++: WWMath::Fast_Asin — fast approximation of asin(x)
    fun fastAsin(x: Float): Float = kotlin.math.asin(x.coerceIn(-1f, 1f))
    // C++: WWMath::Atan2 — atan2(y, x)
    fun atan2(y: Float, x: Float): Float = kotlin.math.atan2(y, x)
    // C++: WWMath::Wrap — wraps 'val' into [min..max]
    fun wrap(v: Float, min: Float, max: Float): Float {
        val range = max - min
        if (range <= 0f) return min
        var r = v
        while (r < min) r += range
        while (r >= max) r -= range
        return r
    }
    // C++: WWMath::Lerp — linear interpolation
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    // C++: WWMath::Lerp (Vector3 overload)
    fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 = a + (b - a) * t
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

// C++: HUDInfo (hudinfo.h)
object HUDInfo {
    // C++: static bool DisplayActionBar = false
    var displayActionBar: Boolean = false
    // C++: static float ActionStatusValue = 0
    var actionStatusValue: Float = 0f

    fun getInformation(obj: Any?): String = ""
    // C++: HUDInfo::Get_Info_Object() — returns the object currently targeted by the player's crosshair
    fun getInfoObject(): DamageableGameObj? = null

    // C++: static void Display_Action_Status_Bar(bool onoff) { DisplayActionBar = onoff; }
    fun displayActionStatusBar(onoff: Boolean) { displayActionBar = onoff }
    // C++: static bool Display_Action_Status_Bar() { return DisplayActionBar; }
    fun displayActionStatusBar(): Boolean = displayActionBar
    // C++: Get_Action_Status_Value / Set_Action_Status_Value — use .actionStatusValue property directly
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
    var tiberiumRegion: ccr.server.defs.OBBoxClass = ccr.server.defs.OBBoxClass()
    fun goHarvest(target: Any? = null): Unit = TODO("stub")
    fun setDropOffRadius(radius: Float): Unit = TODO("stub")
    fun addMoney(amount: Float): Unit = TODO("stub")
    fun getVehicle(): VehicleGameObj? = TODO("stub")
    fun setRefinery(building: BuildingGameObj?): Unit = TODO("stub")
    fun setDockLocation(pos: Vector3): Unit = TODO("stub")
    fun setDockEntrance(pos: Vector3): Unit = TODO("stub")
    fun setHarvestAnim(name: String): Unit = TODO("stub")
    fun setTiberiumRegion(region: Any?): Unit = TODO("stub")
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
    fun getWaypathStartingInBox(box: AABox, lastIndex: Int): WaypathClass? = null
    fun countWaypathsStartingInBox(box: AABox): Int = 1

    companion object {
        fun getInstance(): PathfindClass? = null
    }
}

// C++: NonRefPhysListClass — unowned list of physics objects (iterable via delegation)
class NonRefPhysListClass : MutableList<PhysClass> by mutableListOf()

// C++: WaypathClass — waypath for vehicle delivery routing
class WaypathClass {
    fun getId(): Int = 0
}

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
    val box: AABox = AABox(),
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
    override val factoryChunkId: UInt = ChunkIds.PHYSICS_CHUNKID_BUILDINGAGGREGATE
    fun isMct(): Boolean = TODO("stub")
    fun isRepair(): Boolean = TODO("stub")
    fun getGroupId(): Int = TODO("stub")
    fun getDamageRadius(): Float = TODO("stub")
    fun getDamageStrength(): Float = TODO("stub")
    fun getDamageWarhead(): Int = TODO("stub")
    fun isDestroyed(): Boolean = TODO("stub")
    // setDisabled(Boolean) is provided by PhysClass.isDisabled setter — no stub needed here
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
// RenderObjClass — additional extension methods
// (controlBone, getBoneTransform, setSubObjectsMatchLod, getFactory, getBoundingBox,
// getTransform are now real methods on RenderObjClass in RenderObjClass.kt)
// ---------------------------------------------------------------------------

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

    // C++: CollisionMath::Overlap_Test(AABox, AABox) — simple AABB overlap test
    fun overlapTest(a: AABox, b: AABox): ccr.physics.collision.OverlapType {
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

// ---------------------------------------------------------------------------
// CombatMaterialEffectManager — getHealthEffect, getElectrocutionEffect
// ---------------------------------------------------------------------------

// MaterialEffectClass is a concrete subtype of TransitionEffectClass
class MaterialEffectClass : TransitionEffectClass()

fun CombatMaterialEffectManager.getHealthEffect(): TransitionEffectClass? = null
fun CombatMaterialEffectManager.getElectrocutionEffect(): TransitionEffectClass? = null
// ---------------------------------------------------------------------------
// WeaponClass — additional methods
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// ActionParamsClass — moveLocation, moveArrivedDistance (now real var fields on ActionParamsStruct)
// ---------------------------------------------------------------------------

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
// ExplosionDefinitionClass — stub
// ---------------------------------------------------------------------------

class ExplosionDefinitionClass {
    val damageRadius: Float = 0f
    val damageStrength: Float = 0f
    val damageWarhead: Int = 0
}

// C++: VisTableClass — server-side vis table (stub)
class VisTableClass {
    fun getBit(visObjectId: Int): Boolean = true
}

// ---------------------------------------------------------------------------
// AnimCollisionManagerClass — networkStateUpdate
// ---------------------------------------------------------------------------

// C++: AnimCollisionManagerClass::Network_State_Update() used on SoldierGameObj
fun SoldierGameObj.networkStateUpdate(): Unit = Unit

// ---------------------------------------------------------------------------
// ActiveConversationClass.stopConversation — alias
// ---------------------------------------------------------------------------

// C++: ActiveConversationClass::Stop(int reason = 0)
fun ActiveConversationClass.stopConversation(reason: Int = 0): Unit = stop()

// ---------------------------------------------------------------------------
// PhysicalGameObj.setModel() — sets the render model by name
// ---------------------------------------------------------------------------

// C++: PhysicalGameObj::Set_Model(const char*) — loads and attaches a render object
fun PhysicalGameObj.setModel(modelName: String?) {
    if (modelName != null) physObj?.setModelByName(modelName)
}

// ---------------------------------------------------------------------------
// PhysicalGameObjDef.getSquishVelocity() — minimum impact speed to squish a soldier
// ---------------------------------------------------------------------------

// C++: PhysicalGameObjDef::SquishVelocity — minimum impact speed to squish a soldier
val ccr.server.defs.PhysicalGameObjDef.squishVelocity: Float get() = 10f

// C++: PhysicalGameObj::Get_Squish_Velocity() — stub
fun PhysicalGameObj.getSquishVelocity(): Float = getPhysicalDefinition().squishVelocity

// ---------------------------------------------------------------------------
// SoldierGameObj.getBoneDamageScale() — damage multiplier for the hit bone
// ---------------------------------------------------------------------------

// C++: SoldierGameObj::Get_Bone_Damage_Scale() — returns damage scale for the given ouch type
fun SoldierGameObj.getBoneDamageScale(ouchType: Int): Float = 1f

// ---------------------------------------------------------------------------
// CollisionMath — overload for ccr.math.OBBox
// ---------------------------------------------------------------------------

// C++: CollisionMath::Overlap_Test(OBBoxClass&, Vector3&) — point-in-OBBox test
fun CollisionMath.overlapTest(box: MathOBBox, pos: Vector3): ccr.physics.collision.OverlapType {
    // Simple AABB test using center+extent (ignores basis rotation — server-side approximation)
    val ax0 = box.center.x - box.extent.x; val ax1 = box.center.x + box.extent.x
    val ay0 = box.center.y - box.extent.y; val ay1 = box.center.y + box.extent.y
    val az0 = box.center.z - box.extent.z; val az1 = box.center.z + box.extent.z
    return if (pos.x in ax0..ax1 && pos.y in ay0..ay1 && pos.z in az0..az1)
        ccr.physics.collision.OverlapType.INSIDE
    else
        ccr.physics.collision.OverlapType.OUTSIDE
}

// ---------------------------------------------------------------------------
// PhysRayCollisionTestClass — cast-result bridge
// ---------------------------------------------------------------------------

// C++: Physics cast-ray test used in SoldierGameObj::Can_See()
// The server PhysRayCollisionTestClass wraps a ccr.physics RayCollisionTest-compatible object.
// CombatManager.getScene()?.castRay() expects ccr.physics.collision.RayCollisionTest, but
// the code passes a PhysRayCollisionTestClass which is a different stub type.
// Add an extension to allow the scene to accept PhysRayCollisionTestClass.
fun PhysicsScene.castRay(test: PhysRayCollisionTestClass) {
    // no-op stub: real ray cast not implemented server-side for now
}

// ---------------------------------------------------------------------------
// InnateControllerClass.getInformation() — stub
// ---------------------------------------------------------------------------

// C++: InnateControllerClass::Get_Information() — appends debug info to StringBuilder
fun Any.getInformation(sb: StringBuilder): Unit = Unit

// ---------------------------------------------------------------------------
// ScriptableGameObj.getObserver() — typed helper for observer by index/type
// ---------------------------------------------------------------------------

// C++: ScriptableGameObj::Find_Observer(const char* name) — returns observer by name
fun ScriptableGameObj.findObserver(name: String): GameObjObserverClass? =
    getObservers().firstOrNull { it.getName() == name }

// ---------------------------------------------------------------------------
// PhysClass helper extensions
// ---------------------------------------------------------------------------

// C++: HumanPhysClass::Peek_Carrier_Object() — object this is riding on (e.g. elevator); stub
fun ccr.physics.moveable.HumanPhysClass.peekCarrierObject(): ccr.physics.PhysClass? = null

// ---------------------------------------------------------------------------
// PhysicsScene render/vis extensions (server stubs for client-only operations)
// ---------------------------------------------------------------------------

// C++: SceneClass::Add_Render_Object() — adds a render object to the scene; server no-op
fun PhysicsScene.addRenderObject(obj: RenderObjClass): Unit = Unit

// C++: VisTableClass* PhysicsSceneClass::Get_Vis_Table(Vector3&) — vis-table for PVS; server stub
fun PhysicsScene.getVisTable(position: ccr.math.Vector3): VisTableClass = VisTableClass()

// ---------------------------------------------------------------------------
// PhysicsSceneClass — singleton accessor shim
// C++: PhysicsSceneClass::Get_Instance() — global physics scene singleton
// Server: accessed via CombatManager.getScene(); provide a type alias + companion for C++-style calls.
// ---------------------------------------------------------------------------
object PhysicsSceneClass {
    fun getInstance(): PhysicsScene? = CombatManager.getScene()
}
