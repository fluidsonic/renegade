package ccr.server.net

import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import ccr.server.combat.ArmorWarheadManager
import kotlin.math.max
import kotlin.math.roundToInt

// C++: DefenseObjectClass (damage.h / damage.cpp)
class DefenseObjectClass(health: Float = 100f, var skin: Int = 0) {

    // C++: safe_float Health, HealthMax
    // @JvmName avoids clash with fun setHealth() / fun setHealthMax() below
    @get:JvmName("healthField") @set:JvmName("setHealthField")
    var health: Float = health
    @get:JvmName("healthMaxField") @set:JvmName("setHealthMaxField")
    var healthMax: Float = health

    // C++: safe_float ShieldStrength, ShieldStrengthMax
    // @JvmName avoids clash with fun setShieldStrength() / fun setShieldStrengthMax() below
    @get:JvmName("shieldStrengthField") @set:JvmName("setShieldStrengthField")
    var shieldStrength: Float = 0f
    @get:JvmName("shieldStrengthMaxField") @set:JvmName("setShieldStrengthMaxField")
    var shieldStrengthMax: Float = 0f

    // C++: SafeArmorType ShieldType
    // @JvmName avoids clash with fun setShieldType() below
    @get:JvmName("shieldTypeField") @set:JvmName("setShieldTypeField")
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
    fun setShieldType(type: Int) {
        shieldType = type
        markOwnerDirty()
    }

    // C++: void Mark_Owner_Dirty()
    private fun markOwnerDirty() {
        owner?.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    // C++: DefenseObjectClass::Is_Soft (damage.cpp:596-607)
    fun isSoft(): Boolean {
        if (!ArmorWarheadManager.isArmorSoft(skin)) return false
        if (shieldStrength > 0f && !ArmorWarheadManager.isArmorSoft(shieldType)) return false
        return true
    }

    // C++: bool Is_Repair(const OffenseObjectClass& offense, float scale)
    // Returns true if this offense+scale combination would repair (negative damage) rather than damage.
    fun isRepair(offense: OffenseObjectClass, scale: Float = 1.0f): Boolean {
        val damage = offense.damage * scale
        val wh = ArmorWarheadManager.findWarheadSaveID(offense.warhead)
        val damageScale = ArmorWarheadManager.getDamageMultiplier(skin, wh)
        val shieldDamageScale = ArmorWarheadManager.getDamageMultiplier(shieldType, wh)
        return (damage * damageScale < 0) || (damage * shieldDamageScale < 0)
    }

    // C++: bool Would_Damage(const OffenseObjectClass& offense, float scale)
    // Returns true if this offense+scale combination would actually reduce health or shield.
    fun wouldDamage(offense: OffenseObjectClass, scale: Float = 1.0f): Boolean {
        val damage = offense.damage * scale
        val wh = ArmorWarheadManager.findWarheadSaveID(offense.warhead)
        val damageScale = ArmorWarheadManager.getDamageMultiplier(skin, wh)
        val shieldDamageScale = ArmorWarheadManager.getDamageMultiplier(shieldType, wh)
        // TODO (CombatManager): friendly fire check — if teammate && !isFriendlyFirePermitted() return false
        if (damage * damageScale > 0 && health > 0) return true
        if (damage * shieldDamageScale > 0 && shieldStrength > 0) return true
        return false
    }

    // C++: float Apply_Damage(const OffenseObjectClass& offense, float scale, int alternate_skin)
    // On the server we always call Do_Damage directly (no client-side damage path).
    // C++: damage.cpp:684 — Do_Damage
    //
    // skin, shieldType, and alternateSkin are armor type INDICES (converted from save IDs at
    // def load time by DefenseObjectDef.toDefenseObjectDefClass() and building-def loaders).
    // offense.warhead is treated as a warhead save ID and converted here via findWarheadSaveID.
    fun applyDamage(offense: OffenseObjectClass, scale: Float = 1.0f, alternateSkin: Int = -1): Float {

        var damage = offense.damage * scale

        // Convert warhead save ID → warhead type index
        val wh = ArmorWarheadManager.findWarheadSaveID(offense.warhead)

        // Determine armor multipliers — C++: damage.cpp:730-738
        val damageScale: Float
        var shieldDamageScale: Float
        if (alternateSkin != -1) {
            // MCT hit: alternate_skin is already an armor type index; bypass shields entirely
            damageScale = ArmorWarheadManager.getDamageMultiplier(alternateSkin, wh)
            shieldDamageScale = 0f
        } else {
            damageScale = ArmorWarheadManager.getDamageMultiplier(skin, wh)
            shieldDamageScale = ArmorWarheadManager.getDamageMultiplier(shieldType, wh)
        }

        // Repair branch: negative damage = healing — C++: damage.cpp:751-776
        // check for repair on either health or shield
        if ((damage * damageScale < 0) || (damage * shieldDamageScale < 0)) {

            markOwnerDirty()

            // Apply first to health, then to shield — C++: damage.cpp:762-775
            if (health < healthMax && damageScale != 0f) {
                damage *= damageScale
                val minDamage = health - healthMax  // negative
                damage = damage.coerceIn(minDamage, 0f)
                health -= damage
            } else {
                damage *= shieldDamageScale
                var shieldDamage = damage
                val minShieldDamage = shieldStrength - shieldStrengthMax  // negative
                shieldDamage = shieldDamage.coerceIn(minShieldDamage, 0f)
                shieldStrength -= shieldDamage
            }

        } else {

            // Damage branch — C++: damage.cpp:778-833

            // TODO (CombatManager): friendly fire check — C++ returns Health if teammate && !IsFriendlyFirePermitted()

            if (damage != 0f) {
                markOwnerDirty()
            }

            // If we have a shield, redirect a fraction of damage to it — C++: damage.cpp:795-816
            // If alternate skin (MCT), ignore shield entirely (shieldDamageScale == 0)
            if (shieldStrength > 0f && alternateSkin == -1) {
                var shieldDamage = damage * ArmorWarheadManager.getShieldAbsorption(shieldType, wh)
                damage -= shieldDamage

                shieldDamage *= shieldDamageScale
                var shieldDamageToApply = shieldDamage  // scaled amount we're trying to apply

                if (shieldDamage > shieldStrength) {
                    shieldDamage = shieldStrength
                }
                shieldStrength -= shieldDamage
                if (shieldDamageScale != 0f) {
                    shieldDamage /= shieldDamageScale          // how much unscaled damage did we apply
                    shieldDamageToApply /= shieldDamageScale  // how much scaled damage did we try to apply
                }

                // Add back the overflow (amount we tried to apply minus what was absorbed)
                damage += shieldDamageToApply - shieldDamage

                // Clamp shield strength — C++: damage.cpp:815
                shieldStrength = shieldStrength.coerceIn(0f, shieldStrengthMax)
            }

            // Scale remaining damage by armor multiplier — C++: damage.cpp:819-821
            damage *= damageScale

            // Cap damage at current health — C++: damage.cpp:822-824
            if (health < damage) {
                damage = health
            }

            health -= damage

            // Don't allow object to die if canObjectDie == false — C++: damage.cpp:829-831
            if (!canObjectDie) {
                health = max(health, 1.0f)
            }
        }

        // Clamp health to max — C++: damage.cpp:876
        health = health.coerceIn(0f, healthMax)

        // TODO (PlayerData): Apply_Damage_Points / Apply_Death_Points on smart->Get_Player_Data()

        return health
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

    // C++: bool Is_Repair(const OffenseObjectClass& offense) — returns true when offense is a heal (negative damage)
    fun isRepair(offense: OffenseObjectClass): Boolean {
        val scaledDamage = ArmorWarheadManager.scaleDamage(offense.damage, offense.warhead, skin)
        return scaledDamage < 0f
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
