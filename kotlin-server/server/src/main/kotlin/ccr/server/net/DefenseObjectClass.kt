package ccr.server.net

import ccr.net.bitstream.*
import ccr.server.combat.ArmorWarheadManager
import kotlin.math.roundToInt

// C++: DefenseObjectClass (damage.h / damage.cpp)
class DefenseObjectClass(health: Float = 100f, var skin: Int = 0) {

    // C++: safe_float Health, HealthMax
    var health: Float = health
    var healthMax: Float = health

    // C++: safe_float ShieldStrength, ShieldStrengthMax
    var shieldStrength: Float = 0f
    var shieldStrengthMax: Float = 0f

    // C++: SafeArmorType ShieldType
    var shieldType: Int = 0

    // C++: safe_float DamagePoints, DeathPoints
    var damagePoints: Float = 0f
    var deathPoints: Float = 0f

    // C++: bool CanObjectDie
    var canObjectDie: Boolean = true

    // C++: GameObjReference Owner
    var owner: DamageableGameObj? = null

    // C++: void Init(const DefenseObjectDefClass& def, DamageableGameObj* owner)
    fun init(def: ccr.server.defs.DefenseObjectDefClass, owner: DamageableGameObj) {
        this.owner          = owner
        health              = def.health
        healthMax           = def.healthMax
        skin                = def.skin
        shieldStrength      = def.shieldStrength
        shieldStrengthMax   = def.shieldStrengthMax
        shieldType          = def.shieldType
        damagePoints        = def.damagePoints
        deathPoints         = def.deathPoints
    }

    // C++: void Set_Health(float)
    fun setHealth(h: Float) { health = h.coerceIn(0f, healthMax) }

    // C++: void Add_Health(float)
    fun addHealth(add: Float) { setHealth(health + add) }

    // C++: void Set_Health_Max(float)
    fun setHealthMax(max: Float) { healthMax = max.coerceAtLeast(0f) }

    // C++: void Set_Shield_Strength(float)
    fun setShieldStrength(s: Float) { shieldStrength = s.coerceIn(0f, shieldStrengthMax) }

    // C++: void Add_Shield_Strength(float)
    fun addShieldStrength(add: Float) { setShieldStrength(shieldStrength + add) }

    // C++: void Set_Shield_Strength_Max(float)
    fun setShieldStrengthMax(max: Float) { shieldStrengthMax = max.coerceAtLeast(0f) }

    // C++: void Set_Shield_Type(ArmorType)
    fun setShieldType(type: Int) { shieldType = type }

    // C++: float Apply_Damage(const OffenseObjectClass& offense, float scale, int alternate_skin)
    fun applyDamage(offense: OffenseObjectClass, scale: Float = 1.0f, alternateSkin: Int = -1): Float {
        val effectiveSkin = if (alternateSkin >= 0) alternateSkin else skin
        val scaledDamage = ArmorWarheadManager.scaleDamage(offense.damage * scale, offense.warhead, effectiveSkin)
        return if (scaledDamage >= 0f) {
            val shieldDmg = scaledDamage.coerceAtMost(shieldStrength)
            shieldStrength -= shieldDmg
            val healthDmg = (scaledDamage - shieldDmg).coerceAtMost(health)
            health -= healthDmg
            healthDmg + shieldDmg
        } else {
            val repair = -scaledDamage
            val healthRepair = repair.coerceAtMost(healthMax - health)
            health += healthRepair
            val shieldRepair = (repair - healthRepair).coerceAtMost(shieldStrengthMax - shieldStrength)
            shieldStrength += shieldRepair
            -(healthRepair + shieldRepair)
        }
    }

    // C++: bool Save(ChunkSaveClass&)
    fun save(csave: ccr.server.net.ChunkSaveClass): Boolean {
        csave.writeMicroChunk(MICROCHUNKID_HEALTH,               health)
        csave.writeMicroChunk(MICROCHUNKID_HEALTH_MAX,           healthMax)
        csave.writeMicroChunk(MICROCHUNKID_SKIN,                 skin)
        csave.writeMicroChunk(MICROCHUNKID_SHIELD_STRENGTH,      shieldStrength)
        csave.writeMicroChunk(MICROCHUNKID_SHIELD_STRENGTH_MAX,  shieldStrengthMax)
        csave.writeMicroChunk(MICROCHUNKID_SHIELD_TYPE,          shieldType)
        csave.writeMicroChunk(MICROCHUNKID_DAMAGE_POINTS,        damagePoints)
        csave.writeMicroChunk(MICROCHUNKID_DEATH_POINTS,         deathPoints)
        csave.writeMicroChunk(MICROCHUNKID_CAN_OBJECT_DIE,       canObjectDie)
        return true
    }

    // C++: bool Load(ChunkLoadClass&)
    fun load(cload: ccr.server.net.ChunkLoadClass): Boolean {
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                MICROCHUNKID_HEALTH              -> health              = cload.readFloat()
                MICROCHUNKID_HEALTH_MAX          -> healthMax           = cload.readFloat()
                MICROCHUNKID_SKIN                -> skin                = cload.readInt()
                MICROCHUNKID_SHIELD_STRENGTH     -> shieldStrength      = cload.readFloat()
                MICROCHUNKID_SHIELD_STRENGTH_MAX -> shieldStrengthMax   = cload.readFloat()
                MICROCHUNKID_SHIELD_TYPE         -> shieldType          = cload.readInt()
                MICROCHUNKID_DAMAGE_POINTS       -> damagePoints        = cload.readFloat()
                MICROCHUNKID_DEATH_POINTS        -> deathPoints         = cload.readFloat()
                MICROCHUNKID_CAN_OBJECT_DIE      -> canObjectDie        = cload.readBool()
                else -> error("Unrecognized DefenseObjectClass micro chunk ID: ${cload.curMicroChunkId}")
            }
            cload.closeMicroChunk()
        }
        return true
    }

    companion object {
        private const val MICROCHUNKID_HEALTH              = 1
        private const val MICROCHUNKID_HEALTH_MAX          = 2
        private const val MICROCHUNKID_SKIN                = 3
        private const val MICROCHUNKID_SHIELD_STRENGTH     = 4
        private const val MICROCHUNKID_SHIELD_STRENGTH_MAX = 5
        private const val MICROCHUNKID_SHIELD_TYPE         = 6
        private const val MICROCHUNKID_DAMAGE_POINTS       = 7
        private const val MICROCHUNKID_DEATH_POINTS        = 8
        private const val MICROCHUNKID_CAN_OBJECT_DIE      = 9
    }

    // C++: void Export(BitStreamClass& packet)
    fun export(packet: BitStream) {
        packet.addBool(health == 0f)
        packet.addInt(health.roundToInt(), BITPACK_HEALTH)
        packet.addInt(shieldStrength.roundToInt(), BITPACK_SHIELD_STRENGTH)
        packet.addInt(shieldType, BITPACK_SHIELD_TYPE)
    }

    // C++: void Import(BitStreamClass& packet)
    fun import(packet: BitStream) {
        @Suppress("UNUSED_VARIABLE") val dead = packet.getBool()
        health = packet.getInt(BITPACK_HEALTH).toFloat()
        shieldStrength = packet.getInt(BITPACK_SHIELD_STRENGTH).toFloat()
        shieldType = packet.getInt(BITPACK_SHIELD_TYPE)
    }
}
