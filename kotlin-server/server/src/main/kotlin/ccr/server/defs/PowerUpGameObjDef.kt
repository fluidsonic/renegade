package ccr.server.defs

import ccr.server.net.ChunkLoadClass
import ccr.server.net.ChunkSaveClass
import ccr.server.net.CombatManager

// C++: PowerUpGameObjDef : public SimpleGameObjDef (powerup.h / powerup.cpp)
open class PowerUpGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    // Optional constructor params for test convenience — mirror the var fields below
    grantHealth: Float = 0f,
    grantWeapon: Boolean = true,
    grantWeaponId: Int = 0,
    grantWeaponRounds: Int = 0,
    grantShieldType: Int = 0,
    grantShieldStrength: Float = 0f,
    grantShieldStrengthMax: Float = 0f,
    grantHealthMax: Float = 0f,
    persistent: Boolean = false,
    alwaysAllowGrant: Boolean = false,
    physDefId: Int = 0,                      // C++: int PhysDefID (inherited via PhysicalGameObjDef)
) : SimpleGameObjDef(name, id, chunkId, physDefId = physDefId) {

    // C++: int GrantShieldType (initialized to 0)
    var grantShieldType: Int = grantShieldType

    // C++: float GrantShieldStrength (initialized to 0)
    var grantShieldStrength: Float = grantShieldStrength

    // C++: float GrantShieldStrengthMax (initialized to 0)
    var grantShieldStrengthMax: Float = grantShieldStrengthMax

    // C++: float GrantHealth (initialized to 0)
    var grantHealth: Float = grantHealth

    // C++: float GrantHealthMax (initialized to 0)
    var grantHealthMax: Float = grantHealthMax

    // C++: int GrantWeaponID (initialized to 0)
    var grantWeaponId: Int = grantWeaponId

    // C++: bool GrantWeapon (initialized to true)
    var grantWeapon: Boolean = grantWeapon

    // C++: int GrantWeaponRounds (initialized to 0)
    var grantWeaponRounds: Int = grantWeaponRounds

    // C++: bool GrantWeaponClips (initialized to false)
    var grantWeaponClips: Boolean = false

    // C++: bool Persistent (initialized to false)
    var persistent: Boolean = persistent

    // C++: int GrantKey (initialized to 0)
    var grantKey: Int = 0

    // C++: bool AlwaysAllowGrant (initialized to false)
    var alwaysAllowGrant: Boolean = alwaysAllowGrant

    // C++: int GrantSoundID (initialized to 0)
    var grantSoundId: Int = 0

    // C++: StringClass GrantAnimationName (initialized to "")
    var grantAnimationName: String = ""

    // C++: int IdleSoundID (initialized to 0)
    var idleSoundId: Int = 0

    // C++: StringClass IdleAnimationName (initialized to "")
    var idleAnimationName: String = ""

    // C++: bool Grant(SmartGameObj*, PowerUpGameObj*, bool) const
    // Returns true if anything was granted that the grantee didn't already have.
    fun grant(obj: ccr.server.net.SmartGameObj, pPowerup: ccr.server.net.PowerUpGameObj? = null, hudDisplay: Boolean = true): Boolean {
        var noGrantMessage = 0
        var granted = false

        // WWASSERT(CombatManager::I_Am_Server()) — always server, omit guard

        val defense = obj.defenseObject

        // Grant the shield type
        if (grantShieldType != 0) {
            if (grantShieldType > defense.shieldType) {
                defense.shieldType = grantShieldType
                granted = true
            } else {
                noGrantMessage = IDS_M00DSGN_DSGN1015I1DSGN_TXT // "You are already at full shield."
            }
        }

        // Grant shield strength max (scales off object's base max)
        if (grantShieldStrengthMax != 0f) {
            val baseDef = obj.definition as? DamageableGameObjDef
            var add = grantShieldStrengthMax * (baseDef?.defenseObjectDef?.shieldStrengthMax ?: 0f)

            when (CombatManager.getDifficultyLevel()) {
                0 -> add *= 2.0f
                2 -> add *= 0.75f
            }

            // Round up to next int
            add = (add + 0.95f).toInt().toFloat()

            defense.shieldStrengthMax += add
            granted = true

            // hud_display && obj == COMBAT_STAR: HUDClass::Add_Shield_Upgrade_Grant
            // FIXME: wire HUDClass::Add_Shield_Upgrade_Grant when ported
        }

        // Grant shield strength
        if (grantShieldStrength != 0f) {
            if (defense.shieldStrength < defense.shieldStrengthMax) {
                defense.addShieldStrength(grantShieldStrength)
                granted = true

                // FIXME: DIAG_LOG for COMBAT_STAR position tracking
            } else {
                noGrantMessage = IDS_M00DSGN_DSGN1015I1DSGN_TXT // "You are already at full shield."
            }
        }

        if (granted && hudDisplay) {
            // if obj == COMBAT_STAR && grantShieldStrengthMax == 0:
            //   HUDClass::Add_Shield_Grant(grantShieldStrength)
            // FIXME: wire HUDClass when ported
        }

        // Grant health max (scales off object's base max)
        if (grantHealthMax != 0f) {
            val baseDef = obj.definition as? DamageableGameObjDef
            var add = grantHealthMax * (baseDef?.defenseObjectDef?.healthMax ?: 0f)

            when (CombatManager.getDifficultyLevel()) {
                0 -> add *= 2.0f
                2 -> add *= 0.75f
            }

            // Round up to next int
            add = (add + 0.95f).toInt().toFloat()

            defense.healthMax += add
            granted = true

            // FIXME: wire HUDClass::Add_Health_Upgrade_Grant when ported
        }

        // Grant health
        if (grantHealth != 0f) {
            if (defense.health < defense.healthMax) {
                defense.addHealth(grantHealth)
                granted = true

                // FIXME: wire HUDClass::Add_Health_Grant and DIAG_LOG when ported
            } else {
                noGrantMessage = IDS_M00DSGN_DSGN1014I1DSGN_TXT // "You are already at full health."
            }
        }

        // Grant weapon
        if (grantWeaponId != 0) {
            val bag = obj.weaponBag
            if ((grantWeapon && !bag.isWeaponOwned(grantWeaponId)) ||
                (!bag.isAmmoFull(grantWeaponId))) {

                // FIXME: wire HUDClass::Add_Powerup_Weapon/Ammo when ported

                bag.addWeapon(grantWeaponId, grantWeaponRounds, grantWeapon)
                granted = true

                // FIXME: wire DIAG_LOG for COMBAT_STAR tracking when ported
            } else {
                noGrantMessage = IDS_M00DSGN_DSGN1016I1DSGN_TXT // "Your weapon is full."
            }
        } else if (grantWeaponClips) {
            // Loop over all weapons in the bag — grant rounds to those that accept generic CnC ammo
            val bag = obj.weaponBag
            for (i in 0 until bag.getCount()) {
                val weapon = bag.peekWeapon(i)
                if (weapon.canReceiveGenericCncAmmo) {
                    val clipRounds = weapon.getClipSize()
                    weapon.addRounds(clipRounds * grantWeaponRounds)
                }
            }
        }

        // Grant the key
        if (grantKey != 0) {
            val soldier = obj as? ccr.server.net.SoldierGameObj
            if (soldier != null && soldier.isHumanControlled()) {
                if (!soldier.hasKey(grantKey)) {
                    soldier.giveKey(grantKey)
                    granted = true
                }
            }
            // FIXME: DIAG_LOG for COMBAT_STAR key pickup tracking when ported
            // FIXME: wire HUDClass::Add_Key_Grant when ported
        }

        if (alwaysAllowGrant) {
            granted = true
        }

        if (granted && pPowerup != null) {
            pPowerup.setState(ccr.server.net.PowerUpGameObj.STATE_GRANTING)

            // if COMBAT_STAR == obj: EncyclopediaMgrClass::Reveal_Object(pPowerup)
            // FIXME: wire EncyclopediaMgrClass::Reveal_Object when ported
        }

        // Stats
        if (granted) {
            obj.playerData?.statsAddPowerup()
        }

        // FIXME: if !granted && COMBAT_STAR == obj && noGrantMessage != 0:
        //   HUDInfo::Set_HUD_Help_Text(TRANSLATE(noGrantMessage), Vector3(0,1,0))
        // Suppress unused warning — no_grant_message is intentionally kept for future HUD wiring
        @Suppress("UNUSED_EXPRESSION")
        noGrantMessage

        return granted
    }

    companion object {
        const val CHUNK_ID: UInt = 0x00040107u  // CHUNKID_GAME_OBJECT_DEF_POWERUP

        // C++: string_ids.h — IDS values for no-grant HUD messages
        private const val IDS_M00DSGN_DSGN1015I1DSGN_TXT = 5273  // "You are already at full shield."
        private const val IDS_M00DSGN_DSGN1014I1DSGN_TXT = 5272  // "You are already at full health."
        private const val IDS_M00DSGN_DSGN1016I1DSGN_TXT = 5274  // "Your weapon is full."

        // C++: PowerUpGameObjDef chunk IDs (powerup.cpp enum)
        const val CHUNKID_DEF_PARENT    = 909991656  // PowerUpGameObjDef parent wrapper
        const val CHUNKID_DEF_VARIABLES = 909991657  // CHUNKID_DEF_PARENT + 1

        // C++: micro-chunk IDs (powerup.cpp enum)
        const val XXXMICROCHUNKID_DEF_PARAMETERS               = 1   // legacy
        const val MICROCHUNKID_DEF_PERSISTENT                  = 2
        const val MICROCHUNKID_DEF_GRANT_SHIELD_TYPE           = 3
        const val MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH       = 4
        const val XXXMICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX = 5  // legacy
        const val MICROCHUNKID_DEF_GRANT_HEALTH                = 6
        const val XXXMICROCHUNKID_DEF_GRANT_HEALTH_MAX         = 7   // legacy
        const val MICROCHUNKID_DEF_GRANT_WEAPON_ID             = 8
        const val MICROCHUNKID_DEF_GRANT_WEAPON                = 9
        const val MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS         = 10
        const val XXXMICROCHUNKID_DEF_IS_CAPTURE_THE_FLAG      = 11  // legacy (commented out in C++)
        const val XXXMICROCHUNKID_DEF_GRANT_KEY_MASK           = 12  // legacy
        const val MICROCHUNKID_DEF_GRANT_ANIMATION_NAME        = 13
        const val MICROCHUNKID_DEF_GRANT_SOUNDID               = 14
        const val MICROCHUNKID_DEF_IDLE_ANIMATION_NAME         = 15
        const val MICROCHUNKID_DEF_IDLE_SOUNDID                = 16
        const val MICROCHUNKID_DEF_GRANT_KEY                   = 17
        const val MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT          = 18
        const val MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS          = 19
        const val MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX   = 20
        const val MICROCHUNKID_DEF_GRANT_HEALTH_MAX            = 21

        // C++: SimpleGameObjDef parent wrapper chunk ID used for physDefId lookup
        private const val CHUNKID_SIMPLE_DEF_PARENT        = 930991656   // SimpleGameObjDef CHUNKID_DEF_PARENT
        private const val CHUNKID_PHYSICAL_DEF_VARIABLES   = 909991657   // PhysicalGameObjDef CHUNKID_DEF_VARIABLES
        private const val MICROCHUNKID_PHYS_ID              = 18          // PhysicalGameObjDef MICROCHUNKID_DEF_PHYS_ID

        fun load(objDataReader: ccr.server.mix.ChunkReader, name: String, id: UInt, chunkId: UInt): PowerUpGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES.toUInt())

            // C++: PowerUpGameObjDef::Load opens CHUNKID_DEF_PARENT which contains SimpleGameObjDef::Load,
            // which in turn opens its own CHUNKID_DEF_PARENT(930991656) containing PhysicalGameObjDef::Load.
            // PhysicalGameObjDef stores physDefId inside its CHUNKID_DEF_VARIABLES(909991657) micro-chunk 18.
            var physDefId = 0
            val powerUpParent = objDataReader.findChunk(CHUNKID_DEF_PARENT.toUInt())
            if (powerUpParent != null) {
                val simpleParent = powerUpParent.findChunk(CHUNKID_SIMPLE_DEF_PARENT.toUInt())
                if (simpleParent != null) {
                    val physVars = simpleParent.findChunk(CHUNKID_PHYSICAL_DEF_VARIABLES.toUInt())
                    if (physVars != null) {
                        physDefId = physVars.readMicroInt(MICROCHUNKID_PHYS_ID) ?: 0
                    }
                }
            }

            val def = PowerUpGameObjDef(name = name, id = id, chunkId = chunkId, physDefId = physDefId)
            if (vars != null) {
                def.grantShieldType = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_SHIELD_TYPE) ?: 0
                def.grantShieldStrength = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH) ?: 0f
                def.grantShieldStrengthMax = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX) ?: 0f
                def.grantHealth = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_HEALTH) ?: 0f
                def.grantHealthMax = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_HEALTH_MAX) ?: 0f
                def.grantWeaponId = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_WEAPON_ID) ?: 0
                def.grantWeapon = vars.readMicroBool(MICROCHUNKID_DEF_GRANT_WEAPON) ?: true
                def.grantWeaponClips = vars.readMicroBool(MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS) ?: false
                def.grantWeaponRounds = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS) ?: 0
                def.persistent = vars.readMicroBool(MICROCHUNKID_DEF_PERSISTENT) ?: false
                def.grantKey = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_KEY) ?: 0
                def.grantSoundId = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_SOUNDID) ?: 0
                def.idleSoundId = vars.readMicroInt(MICROCHUNKID_DEF_IDLE_SOUNDID) ?: 0
                def.grantAnimationName = vars.readMicroString(MICROCHUNKID_DEF_GRANT_ANIMATION_NAME) ?: ""
                def.idleAnimationName = vars.readMicroString(MICROCHUNKID_DEF_IDLE_ANIMATION_NAME) ?: ""
                def.alwaysAllowGrant = vars.readMicroBool(MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT) ?: false
            }
            return def
        }
    }

    // C++: bool PowerUpGameObjDef::Save(ChunkSaveClass& csave)
    open fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_DEF_PARENT)
        // C++: SimpleGameObjDef::Save(csave) — parent chain save
        // FIXME: wire SimpleGameObjDef::Save when SimpleGameObjDef is ported to the def hierarchy
        csave.endChunk()

        csave.beginChunk(CHUNKID_DEF_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_SHIELD_TYPE,         grantShieldType)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH,     grantShieldStrength)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX, grantShieldStrengthMax)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_HEALTH,              grantHealth)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_HEALTH_MAX,          grantHealthMax)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_WEAPON_ID,           grantWeaponId)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_WEAPON,              grantWeapon)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS,        grantWeaponClips)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS,       grantWeaponRounds)
        csave.writeMicroChunk(MICROCHUNKID_DEF_PERSISTENT,                persistent)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_KEY,                 grantKey)
        csave.writeMicroChunk(MICROCHUNKID_DEF_GRANT_SOUNDID,             grantSoundId)
        csave.writeMicroChunk(MICROCHUNKID_DEF_IDLE_SOUNDID,              idleSoundId)
        csave.writeMicroChunkWwString(MICROCHUNKID_DEF_GRANT_ANIMATION_NAME, grantAnimationName)
        csave.writeMicroChunkWwString(MICROCHUNKID_DEF_IDLE_ANIMATION_NAME,  idleAnimationName)
        csave.writeMicroChunk(MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT,        alwaysAllowGrant)
        csave.endChunk()

        return true
    }

    // C++: bool PowerUpGameObjDef::Load(ChunkLoadClass& cload)
    open fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId.toInt()) {
                CHUNKID_DEF_PARENT -> {
                    // C++: SimpleGameObjDef::Load(cload)
                    // FIXME: wire SimpleGameObjDef::Load when SimpleGameObjDef is ported to the def hierarchy
                }
                CHUNKID_DEF_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_DEF_GRANT_SHIELD_TYPE           -> grantShieldType          = cload.readInt()
                            MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH       -> grantShieldStrength       = cload.readFloat()
                            MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX   -> grantShieldStrengthMax    = cload.readFloat()
                            MICROCHUNKID_DEF_GRANT_HEALTH                -> grantHealth               = cload.readFloat()
                            MICROCHUNKID_DEF_GRANT_HEALTH_MAX            -> grantHealthMax            = cload.readFloat()
                            MICROCHUNKID_DEF_GRANT_WEAPON_ID             -> grantWeaponId             = cload.readInt()
                            MICROCHUNKID_DEF_GRANT_WEAPON                -> grantWeapon               = cload.readBool()
                            MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS          -> grantWeaponClips          = cload.readBool()
                            MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS         -> grantWeaponRounds         = cload.readInt()
                            MICROCHUNKID_DEF_PERSISTENT                  -> persistent                = cload.readBool()
                            MICROCHUNKID_DEF_GRANT_KEY                   -> grantKey                  = cload.readInt()
                            MICROCHUNKID_DEF_GRANT_SOUNDID               -> grantSoundId              = cload.readInt()
                            MICROCHUNKID_DEF_IDLE_SOUNDID                -> idleSoundId               = cload.readInt()
                            MICROCHUNKID_DEF_GRANT_ANIMATION_NAME        -> grantAnimationName        = cload.readWwString()
                            MICROCHUNKID_DEF_IDLE_ANIMATION_NAME         -> idleAnimationName         = cload.readWwString()
                            MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT          -> alwaysAllowGrant          = cload.readBool()
                            // Legacy micro-chunks — present in old save files but no longer written
                            XXXMICROCHUNKID_DEF_PARAMETERS               -> cload.skip()             // legacy
                            XXXMICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX -> grantShieldStrengthMax   = cload.readFloat() // legacy slot, same field
                            XXXMICROCHUNKID_DEF_GRANT_HEALTH_MAX         -> grantHealthMax            = cload.readFloat() // legacy slot, same field
                            XXXMICROCHUNKID_DEF_IS_CAPTURE_THE_FLAG      -> cload.skip()             // legacy (commented out in C++)
                            XXXMICROCHUNKID_DEF_GRANT_KEY_MASK           -> cload.skip()             // legacy
                            else -> error("Unrecognized PowerUpGameObjDef variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                else -> error("Unrecognized PowerUpGameObjDef chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }
}
