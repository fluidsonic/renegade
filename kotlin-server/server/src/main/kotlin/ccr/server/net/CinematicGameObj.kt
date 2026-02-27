package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: CinematicGameObj (cinematicgameobj.cpp) — extends ArmedGameObj.
// Export_Rare: calls ArmedGameObj::Export_Rare (→ PhysicalGameObj chain), then appends:
//   animationName (terminated string, permitEmpty=true)
//   animMode (int) — AnimCollisionManagerClass::AnimModeType (0 = ANIMATE_TARGET)
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj
//                         → ArmedGameObj → CinematicGameObj
class CinematicGameObj() : ArmedGameObj() {

    // C++: char AnimationName[32] — stored name of the animation being played
    var cinematicAnimName: String = ""

    // C++: int AnimMode — AnimCollisionManagerClass::AnimModeType
    var animMode: Int = ANIM_MODE_TARGET

    init {
        // C++: Set_App_Packet_Type(APPPACKETTYPE_CINEMATIC)
        appPacketType = APPPACKETTYPE_CINEMATIC.toByte()
    }

    companion object {
        // AnimCollisionManagerClass::AnimModeType
        const val ANIM_MODE_TARGET = 0
        const val ANIM_MODE_LOOP   = 1

        // C++: apppackettypes.h — APPPACKETTYPE_CINEMATIC = 27 (sequential enum)
        const val APPPACKETTYPE_CINEMATIC = 27
    }

    // Secondary constructor for tests — bypasses Init() / definition pipeline.
    constructor(
        definitionId: Int,
        position: Vector3 = Vector3(),
        modelName: String = "",
        animName: String = "",
        cinematicAnimName: String = "",
        animMode: Int = ANIM_MODE_TARGET,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "cinematic_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.modelName = modelName
        this.animName = animName
        this.cinematicAnimName = cinematicAnimName
        this.animMode = animMode
    }

    // C++: CinematicGameObj::Init() — delegates to Init(Get_Definition())
    override fun init() {
        init(getDefinition())
    }

    // C++: void CinematicGameObj::Init(const CinematicGameObjDef&)
    fun init(definition: ccr.server.defs.CinematicGameObjDef) {
        super.init(definition)
    }

    // C++: const CinematicGameObjDef& Get_Definition() const
    fun getDefinition(): ccr.server.defs.CinematicGameObjDef = definition as ccr.server.defs.CinematicGameObjDef

    // C++: CinematicGameObj does not override Get_Position; delegates to PhysicalGameObj
    override fun getPosition(): Vector3 = physObj?.getPosition() ?: Vector3()

    // C++: CinematicGameObj::Export_Rare — calls super (ArmedGameObj → PhysicalGameObj chain),
    // then appends cinematicAnimName and animMode.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // PhysicalGameObj: model + anim + host + player_type + hud_pokable
        packet.addTerminatedString(cinematicAnimName, permitEmpty = true)
        packet.addInt(animMode)
    }

    // C++: CinematicGameObj::Import_Rare — reads cinematicAnimName + animMode
    override fun importRare(packet: BitStream) {
        super.importRare(packet)
        cinematicAnimName = packet.getTerminatedString(permitEmpty = true)
        animMode = packet.getInt()
        // C++: passes to DynamicAnimPhysClass anim manager — physics not available server-side
    }

    // C++: CinematicGameObj::Think — auto-fire at enemies if AutoFireWeapon, camera-relative transform
    override fun think() {
        // C++: AutoFireWeapon and CameraRelative logic omitted (singleplayer/client-only)
        super.think()
    }

    // C++: CinematicGameObj::Post_Think — destroy after animation reaches target
    override fun postThink() {
        super.postThink()
        // C++: if (Get_Definition().DestroyAfterAnimation && animation Is_At_Target()) → Set_Delete_Pending()
        // Server-side: animation state not tracked; deletion handled by game logic externally
    }

    // C++: CinematicGameObj::Completely_Damaged — spawn killed explosion + set delete pending
    override fun completelyDamaged(damager: OffenseObjectClass) {
        // C++: ExplosionManager::Create_Explosion_At(Get_Definition().KilledExplosion, pos, owner)
        setDeletePending()
    }

    // C++: CinematicGameObj::Takes_Explosion_Damage — returns false
    override fun takesExplosionDamage(): Boolean = false
}
