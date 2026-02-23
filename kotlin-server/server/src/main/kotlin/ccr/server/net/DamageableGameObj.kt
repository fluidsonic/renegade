package ccr.server.net

import ccr.net.bitstream.*
import ccr.server.defs.DamageableGameObjDef

// C++: playertype.h
const val PLAYERTYPE_SPECTATOR = -4
const val PLAYERTYPE_MUTANT    = -3
const val PLAYERTYPE_NEUTRAL   = -2
const val PLAYERTYPE_RENEGADE  = -1
const val PLAYERTYPE_NOD       =  0
const val PLAYERTYPE_GDI       =  1

// C++: Player_Types_Are_Enemies (playertype.h inline)
fun playerTypesAreEnemies(t1: Int, t2: Int): Boolean {
    require(t1 in PLAYERTYPE_SPECTATOR..PLAYERTYPE_GDI)
    require(t2 in PLAYERTYPE_SPECTATOR..PLAYERTYPE_GDI)
    if (t1 == PLAYERTYPE_NEUTRAL   || t2 == PLAYERTYPE_NEUTRAL)   return false
    if (t1 == PLAYERTYPE_SPECTATOR || t2 == PLAYERTYPE_SPECTATOR) return false
    if (t1 == PLAYERTYPE_RENEGADE  || t2 == PLAYERTYPE_RENEGADE)  return true
    return t1 != t2
}

// C++: Get_Color_For_Team (colors.cpp) — WWASSERT(team == NOD || team == GDI)
// FIXME: COLOR_TEAM_0/1 should come from colors.h constants
fun getColorForTeam(team: Int): ccr.math.Vector3 {
    require(team == PLAYERTYPE_NOD || team == PLAYERTYPE_GDI)
    return if (team == PLAYERTYPE_NOD) ccr.math.Vector3(1f, 0f, 0f)   // COLOR_TEAM_0
           else                        ccr.math.Vector3(1f, 0.8f, 0f)  // COLOR_TEAM_1
}

// C++: DamageableGameObj (damageablegameobj.cpp)
// C++ hierarchy: ScriptableGameObj → DamageableGameObj → PhysicalGameObj
abstract class DamageableGameObj : ScriptableGameObj() {

    // C++: DefenseObjectClass DefenseObject
    val defenseObject = DefenseObjectClass()

    // Delegating accessors for DefenseObject fields — keeps subclass code working
    var health: Float         get() = defenseObject.health;         set(v) { defenseObject.health = v }
    var healthMax: Float      get() = defenseObject.healthMax;      set(v) { defenseObject.healthMax = v }
    var shieldStrength: Float get() = defenseObject.shieldStrength; set(v) { defenseObject.shieldStrength = v }
    var shieldStrengthMax: Float get() = defenseObject.shieldStrengthMax; set(v) { defenseObject.shieldStrengthMax = v }
    var shieldType: Int       get() = defenseObject.shieldType;     set(v) { defenseObject.shieldType = v }

    // C++: int PlayerType — Set_Player_Type also sets BIT_RARE dirty
    open var playerType: Int = PLAYERTYPE_NEUTRAL
        set(value) {
            field = value
            setObjectDirtyBit(BIT_RARE, true)
        }

    // C++: bool IsHealthBarDisplayed
    var isHealthBarDisplayed: Boolean = true

    init {
        playerType = PLAYERTYPE_NEUTRAL  // triggers setter → BIT_RARE dirty
    }

    // C++: void Init(const DamageableGameObjDef&)
    fun init(definition: DamageableGameObjDef) {
        super.init(definition)
        copySettings(definition)
    }

    // C++: void Copy_Settings(const DamageableGameObjDef&)
    fun copySettings(definition: DamageableGameObjDef) {
        playerType = definition.defaultPlayerType
        defenseObject.init(definition.defenseObjectDef, this)
    }

    // C++: void Re_Init(const DamageableGameObjDef&)
    fun reInit(definition: DamageableGameObjDef) {
        val oldPlayerType = playerType
        super.reInit(definition)
        copySettings(definition)
        playerType = oldPlayerType
    }

    // C++: const DamageableGameObjDef & Get_Definition() const
    fun getDamageableDefinition(): DamageableGameObjDef = definition as DamageableGameObjDef

    // C++: const StringClass & Get_Info_Icon_Texture_Filename()
    fun getInfoIconTextureFilename(): String = getDamageableDefinition().infoIconTextureFilename

    // C++: int Get_Translated_Name_ID() const
    fun getTranslatedNameId(): Int = getDamageableDefinition().translatedNameId

    // C++: virtual bool Save(ChunkSaveClass&)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_PLAYER_TYPE, playerType)
        csave.writeMicroChunk(MICROCHUNKID_IS_HEALTH_BAR_DISPLAYED, isHealthBarDisplayed)
        csave.endChunk()

        csave.beginChunk(CHUNKID_DEFENSEOBJECT)
        defenseObject.save(csave)
        csave.endChunk()

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass&)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT        -> super.load(cload)
                CHUNKID_VARIABLES     -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_PLAYER_TYPE              -> playerType          = cload.readInt()
                            MICROCHUNKID_IS_HEALTH_BAR_DISPLAYED  -> isHealthBarDisplayed = cload.readBool()
                            else -> error("Unrecognized DamageableGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_DEFENSEOBJECT -> defenseObject.load(cload)
                else -> error("Unrecognized DamageableGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    companion object {
        private const val CHUNKID_PARENT        = 207011212
        private const val CHUNKID_DEFENSEOBJECT = 207011213
        private const val CHUNKID_VARIABLES     = 207011214

        private const val MICROCHUNKID_PLAYER_TYPE             = 1
        private const val MICROCHUNKID_IS_HEALTH_BAR_DISPLAYED = 2
    }

    // C++: DefenseObjectClass::IsDead
    val isDead: Boolean get() = defenseObject.health <= 0f

    // C++: virtual bool Is_Targetable() const
    open fun isTargetable(): Boolean = !getDamageableDefinition().notTargetable

    // C++: bool Is_Team_Player()
    fun isTeamPlayer(): Boolean = playerType == PLAYERTYPE_NOD || playerType == PLAYERTYPE_GDI

    // C++: Vector3 Get_Team_Color() → Get_Color_For_Team(PlayerType) (colors.cpp)
    fun getTeamColor(): ccr.math.Vector3 = getColorForTeam(playerType)

    // C++: bool Is_Teammate(DamageableGameObj* p_obj) — WWASSERT(p_obj != NULL)
    fun isTeammate(obj: DamageableGameObj): Boolean =
        (obj === this) || (isTeamPlayer() && playerType == obj.playerType)

    // C++: bool Is_Enemy(DamageableGameObj* p_obj) — WWASSERT(p_obj != NULL)
    fun isEnemy(obj: DamageableGameObj): Boolean =
        (obj !== this) && playerTypesAreEnemies(playerType, obj.playerType)

    // C++: virtual void Apply_Damage(const OffenseObjectClass& damager, float scale=1.0, int alternate_skin=-1)
    open fun applyDamage(damager: OffenseObjectClass, scale: Float = 1.0f, alternateSkin: Int = -1) {
        if (defenseObject.health <= 0f) return
        if (isDeletePending) return
        val oldHealth = defenseObject.health
        val oldShield = defenseObject.shieldStrength
        defenseObject.applyDamage(damager, scale, alternateSkin)
        val diff = oldHealth + oldShield - defenseObject.health - defenseObject.shieldStrength
        for (observer in observers) observer.damaged(this, damager.owner, diff)
        if (defenseObject.health <= 0f) {
            for (observer in observers) observer.killed(this, damager.owner)
            completelyDamaged(damager)
        }
    }

    // Convenience: create OffenseObjectClass from a raw damage float
    fun applyDamage(damage: Float) = applyDamage(OffenseObjectClass(damage = damage))

    // C++: virtual void Completely_Damaged(const OffenseObjectClass& damager)
    open fun completelyDamaged(damager: OffenseObjectClass) {}

    // C++: DamageableGameObj::Export_Occasional → DefenseObjectClass::Export
    override fun exportOccasional(packet: BitStream) {
        super.exportOccasional(packet)
        defenseObject.export(packet)
    }

    // C++: DamageableGameObj::Import_Occasional → DefenseObjectClass::Import
    override fun importOccasional(packet: BitStream) {
        super.importOccasional(packet)
        val oldHealth = defenseObject.health
        defenseObject.import(packet)
        val newHealth = defenseObject.health
        if (oldHealth > 0f && oldHealth > newHealth) {
            for (observer in observers) observer.damaged(this, null, oldHealth - newHealth)
        }
        if (oldHealth > 0f && newHealth <= 0f) {
            for (observer in observers) observer.killed(this, null)
            completelyDamaged(OffenseObjectClass())
        }
    }
}
