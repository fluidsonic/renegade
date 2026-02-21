package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of WeaponDefinitionClass (Combat/weaponmanager.h).
 *
 * C++ hierarchy: WeaponDefinitionClass : DefinitionClass
 */
class WeaponDefinitionClass(
    name: String,
    id: UInt,
    classId: UInt,
    val style: Int = 1,
    val model: String = "",
    val idleAnim: String = "",
    val fireAnim: String = "",
    val backModel: String = "",
    val switchTime: Float = 0f,
    val reloadTime: Float = 0f,
    val keyNumber: Float = 0f,
    val canSnipe: Boolean = false,
    val canReceiveGenericCnCAmmo: Boolean = true,
    val rating: Float = 0.1f,
    val ejectPhysDefID: Int = 0,
    val muzzleFlashPhysDefID: Int = 0,
    val firstPersonModel: String = "",
    val firstPersonOffsetX: Float = 0f,
    val firstPersonOffsetY: Float = 0f,
    val firstPersonOffsetZ: Float = 0f,
    val recoilImpulse: Float = 0f,
    val hudIconTextureName: String = "",
    val reloadSoundDefID: Int = 0,
    val emptySoundDefID: Int = 0,
    val primaryAmmoDefID: Int = 0,
    val secondaryAmmoDefID: Int = 0,
    val clipSize: Int = 0,
    val recoilTime: Float = 0.1f,
    val recoilScale: Float = 1f,
    val aGiveWeaponsWeapon: Boolean = false,
    val maxInventoryRounds: Int = 100,
    val iconNameID: Int = 0,
    val iconTextureName: String = "",
    val iconTextureUVLeft: Float = 0f,
    val iconTextureUVTop: Float = 0f,
    val iconTextureUVRight: Float = 0f,
    val iconTextureUVBottom: Float = 0f,
    val iconOffsetX: Float = 0f,
    val iconOffsetY: Float = 0f,
    val humanFiringAnimation: String = "",
) : DefinitionClass(name, id, classId) {

    companion object {
        const val CLASS_ID: UInt = 0xB001u // CLASSID_MUNITIONS + 1
    }
}

private const val CHUNKID_WEAPON_DEF_VARIABLES = 1205091654u

// Micro-chunk IDs (from weaponmanager.cpp local enum, starting at 1)
private const val MCID_STYLE = 1
private const val MCID_MODEL = 2
private const val MCID_IDLE_ANIM = 3
private const val MCID_FIRE_ANIM = 4
private const val MCID_BACK_MODEL = 5
// 6 = XXXXX (unused)
// 7 = XXX RATE_OF_FIRE (unused)
private const val MCID_SWITCH_TIME = 8
private const val MCID_RELOAD_TIME = 9
// 10-15 = unused/legacy
private const val MCID_CAN_SNIPE = 16
// 17 = unused
// 18 = LEGACY_FIRE_SOUND_DEFID (not loaded)
private const val MCID_EJECT_PHYS_DEF_ID = 19
private const val MCID_MUZZLE_FLASH_PHYS_DEF_ID = 20
private const val MCID_RATING = 21
private const val MCID_FIRST_PERSON_MODEL = 22
private const val MCID_FIRST_PERSON_OFFSET = 23
private const val MCID_RECOIL_IMPULSE = 24
// 25 = XXX_HUD_MODEL (unused)
private const val MCID_RELOAD_SOUND_DEFID = 26
private const val MCID_PRIMARY_AMMO_DEF_ID = 27
private const val MCID_SECONDARY_AMMO_DEF_ID = 28
private const val MCID_CLIP_SIZE = 29
private const val MCID_RECOIL_TIME = 30
private const val MCID_RECOIL_SCALE = 31
private const val MCID_A_GIVE_WEAPONS_WEAPON = 32
// 33 = unused
private const val MCID_HUD_ICON_TEXTURE_NAME = 34
private const val MCID_MAX_INVENTORY_ROUNDS = 35
// 36 = unused
private const val MCID_KEY_NUMBER = 37
private const val MCID_ICON_NAME_ID = 38
private const val MCID_ICON_TEXTURE_NAME = 39
private const val MCID_ICON_TEXTURE_UV = 40
private const val MCID_ICON_OFFSET = 41
private const val MCID_HUMAN_FIRING_ANIMATION = 42
private const val MCID_EMPTY_SOUND_DEFID = 43
private const val MCID_GENERIC_AMMO_OK = 44

fun parseWeaponDefinitionClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): WeaponDefinitionClass? {
    val varsReader = objDataReader.findChunk(CHUNKID_WEAPON_DEF_VARIABLES) ?: return null

    return WeaponDefinitionClass(
        name = name,
        id = id,
        classId = classId,
        style = varsReader.readMicroInt(MCID_STYLE) ?: 1,
        model = varsReader.readMicroString(MCID_MODEL) ?: "",
        idleAnim = varsReader.readMicroString(MCID_IDLE_ANIM) ?: "",
        fireAnim = varsReader.readMicroString(MCID_FIRE_ANIM) ?: "",
        backModel = varsReader.readMicroString(MCID_BACK_MODEL) ?: "",
        switchTime = varsReader.readMicroFloat(MCID_SWITCH_TIME) ?: 0f,
        reloadTime = varsReader.readMicroFloat(MCID_RELOAD_TIME) ?: 0f,
        keyNumber = varsReader.readMicroFloat(MCID_KEY_NUMBER) ?: 0f,
        canSnipe = varsReader.readMicroBool(MCID_CAN_SNIPE) ?: false,
        canReceiveGenericCnCAmmo = varsReader.readMicroBool(MCID_GENERIC_AMMO_OK) ?: true,
        rating = varsReader.readMicroFloat(MCID_RATING) ?: 0.1f,
        ejectPhysDefID = varsReader.readMicroInt(MCID_EJECT_PHYS_DEF_ID) ?: 0,
        muzzleFlashPhysDefID = varsReader.readMicroInt(MCID_MUZZLE_FLASH_PHYS_DEF_ID) ?: 0,
        firstPersonModel = varsReader.readMicroString(MCID_FIRST_PERSON_MODEL) ?: "",
        firstPersonOffsetX = varsReader.readMicroFloatAt(MCID_FIRST_PERSON_OFFSET, 0) ?: 0f,
        firstPersonOffsetY = varsReader.readMicroFloatAt(MCID_FIRST_PERSON_OFFSET, 4) ?: 0f,
        firstPersonOffsetZ = varsReader.readMicroFloatAt(MCID_FIRST_PERSON_OFFSET, 8) ?: 0f,
        recoilImpulse = varsReader.readMicroFloat(MCID_RECOIL_IMPULSE) ?: 0f,
        hudIconTextureName = varsReader.readMicroString(MCID_HUD_ICON_TEXTURE_NAME) ?: "",
        reloadSoundDefID = varsReader.readMicroInt(MCID_RELOAD_SOUND_DEFID) ?: 0,
        emptySoundDefID = varsReader.readMicroInt(MCID_EMPTY_SOUND_DEFID) ?: 0,
        primaryAmmoDefID = varsReader.readMicroInt(MCID_PRIMARY_AMMO_DEF_ID) ?: 0,
        secondaryAmmoDefID = varsReader.readMicroInt(MCID_SECONDARY_AMMO_DEF_ID) ?: 0,
        clipSize = varsReader.readMicroInt(MCID_CLIP_SIZE) ?: 0,
        recoilTime = varsReader.readMicroFloat(MCID_RECOIL_TIME) ?: 0.1f,
        recoilScale = varsReader.readMicroFloat(MCID_RECOIL_SCALE) ?: 1f,
        aGiveWeaponsWeapon = varsReader.readMicroBool(MCID_A_GIVE_WEAPONS_WEAPON) ?: false,
        maxInventoryRounds = varsReader.readMicroInt(MCID_MAX_INVENTORY_ROUNDS) ?: 100,
        iconNameID = varsReader.readMicroInt(MCID_ICON_NAME_ID) ?: 0,
        iconTextureName = varsReader.readMicroString(MCID_ICON_TEXTURE_NAME) ?: "",
        iconTextureUVLeft = varsReader.readMicroFloatAt(MCID_ICON_TEXTURE_UV, 0) ?: 0f,
        iconTextureUVTop = varsReader.readMicroFloatAt(MCID_ICON_TEXTURE_UV, 4) ?: 0f,
        iconTextureUVRight = varsReader.readMicroFloatAt(MCID_ICON_TEXTURE_UV, 8) ?: 0f,
        iconTextureUVBottom = varsReader.readMicroFloatAt(MCID_ICON_TEXTURE_UV, 12) ?: 0f,
        iconOffsetX = varsReader.readMicroFloatAt(MCID_ICON_OFFSET, 0) ?: 0f,
        iconOffsetY = varsReader.readMicroFloatAt(MCID_ICON_OFFSET, 4) ?: 0f,
        humanFiringAnimation = varsReader.readMicroString(MCID_HUMAN_FIRING_ANIMATION) ?: "",
    )
}
