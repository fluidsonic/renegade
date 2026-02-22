package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// =====================================================================================
// GlobalSettingsDef (Combat/globalsettings.h)
// CLASSID_GLOBAL_SETTINGS_DEF_GENERAL = 0xF003
// =====================================================================================

data class GlobalSettingsDef(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    val deathSoundId: Int = 0,
    val evaObjectivesSoundId: Int = 0,
    val hudHelpTextSoundId: Int = 0,
    val maxConversationDist: Float = 10f,
    val maxCombatConversationDist: Float = 10f,
    val soldierWalkSpeed: Float = 0.25f,
    val soldierCrouchSpeed: Float = 0.25f,
    val encyclopediaEventStringId: Int = 0,
    val fallingDamageMinDistance: Float = 5f,
    val fallingDamageMaxDistance: Float = 20f,
    val fallingDamageWarhead: Int = 15,
    val purchaseGdiCharactersTexture: String = "",
    val purchaseGdiVehiclesTexture: String = "",
    val purchaseGdiEquipmentTexture: String = "",
    val purchaseNodCharactersTexture: String = "",
    val purchaseNodVehiclesTexture: String = "",
    val purchaseNodEquipmentTexture: String = "",
    val purchaseGdiMutCharactersTexture: String = "",
    val purchaseGdiMutVehiclesTexture: String = "",
    val purchaseGdiMutEquipmentTexture: String = "",
    val purchaseNodMutCharactersTexture: String = "",
    val purchaseNodMutVehiclesTexture: String = "",
    val purchaseNodMutEquipmentTexture: String = "",
    val stealthDistanceHuman: Float = 15f,
    val stealthDistanceVehicle: Float = 25f,
    val mpStealthDistanceHuman: Float = 15f,
    val mpStealthDistanceVehicle: Float = 25f,
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040602u  // CHUNKID_GLOBAL_SETTINGS_DEF_GENERAL
    }
}

fun parseGlobalSettingsDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): GlobalSettingsDef? {
    val vars = objDataReader.findChunk(GLOBAL_CHUNKID_DEF_VARIABLES)
        ?: return GlobalSettingsDef(name = name, id = id, chunkId = chunkId)

    return GlobalSettingsDef(
        name = name, id = id, chunkId = chunkId,
        deathSoundId = vars.mcInt(2) ?: 0,
        evaObjectivesSoundId = vars.mcInt(3) ?: 0,
        hudHelpTextSoundId = vars.mcInt(25) ?: 0, // MICROCHUNKID_DEF_HELP_TXT_SOUND
        maxConversationDist = vars.mcFloat(4) ?: 10f,
        maxCombatConversationDist = vars.mcFloat(5) ?: 10f,
        soldierWalkSpeed = vars.mcFloat(6) ?: 0.25f,
        soldierCrouchSpeed = vars.mcFloat(7) ?: 0.25f,
        fallingDamageMinDistance = vars.mcFloat(8) ?: 5f,
        fallingDamageMaxDistance = vars.mcFloat(9) ?: 20f,
        fallingDamageWarhead = vars.mcInt(10) ?: 15,
        purchaseGdiCharactersTexture = vars.mcString(11) ?: "",
        purchaseGdiVehiclesTexture = vars.mcString(12) ?: "",
        purchaseGdiEquipmentTexture = vars.mcString(13) ?: "",
        purchaseNodCharactersTexture = vars.mcString(15) ?: "",
        purchaseNodVehiclesTexture = vars.mcString(16) ?: "",
        purchaseNodEquipmentTexture = vars.mcString(17) ?: "",
        purchaseGdiMutCharactersTexture = vars.mcString(18) ?: "",
        purchaseGdiMutVehiclesTexture = vars.mcString(19) ?: "",
        purchaseGdiMutEquipmentTexture = vars.mcString(20) ?: "",
        purchaseNodMutCharactersTexture = vars.mcString(21) ?: "",
        purchaseNodMutVehiclesTexture = vars.mcString(22) ?: "",
        purchaseNodMutEquipmentTexture = vars.mcString(23) ?: "",
        encyclopediaEventStringId = vars.mcInt(24) ?: 0,
        stealthDistanceHuman = vars.mcFloat(26) ?: 15f,
        stealthDistanceVehicle = vars.mcFloat(27) ?: 25f,
        mpStealthDistanceHuman = vars.mcFloat(28) ?: 15f,
        mpStealthDistanceVehicle = vars.mcFloat(29) ?: 25f,
    )
}

// =====================================================================================
// HumanLoiterGlobalSettingsDef
// CLASSID_GLOBAL_SETTINGS_DEF_HUMAN_LOITER = 0xF002
// =====================================================================================

data class HumanLoiterGlobalSettingsDef(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    val activationDelay: Float = 20f,
    val loiterFrequency: Float = 10f,
    val loiterAnimList: List<String> = emptyList(),
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040601u  // CHUNKID_GLOBAL_SETTINGS_DEF_HUMAN_LOITER
    }
}

fun parseHumanLoiterGlobalSettingsDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): HumanLoiterGlobalSettingsDef? {
    // Chunk IDs: CHUNKID_HL_DEF_PARENT=803001812, CHUNKID_HL_DEF_VARIABLES=803001813
    val vars = objDataReader.findChunk(803001813u)
        ?: return HumanLoiterGlobalSettingsDef(name = name, id = id, chunkId = chunkId)

    val anims = vars.mcStringList(3) // MICROCHUNKID_HL_DEF_LOITER_ANIM_LIST_ENTRY

    return HumanLoiterGlobalSettingsDef(
        name = name, id = id, chunkId = chunkId,
        activationDelay = vars.mcFloat(1) ?: 20f,
        loiterFrequency = vars.mcFloat(2) ?: 10f,
        loiterAnimList = anims,
    )
}

// =====================================================================================
// HUDGlobalSettingsDef
// CLASSID_GLOBAL_SETTINGS_DEF_HUD = 0xF004
//
// This class is very large (client-side HUD layout). We store all fields faithfully.
// Types: Vector3 = 3 floats (12 bytes), Vector2 = 2 floats (8 bytes),
//        RectClass = 4 floats (16 bytes), float (4 bytes), int (4 bytes)
// =====================================================================================

/** 3-component float vector (C++ Vector3). */
data class Vec3(val x: Float, val y: Float, val z: Float)

/** 4-component rect (C++ RectClass: left, top, right, bottom). */
data class Rect4(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** 2-component float vector (C++ Vector2). */
data class Vec2(val x: Float, val y: Float)

data class HUDGlobalSettingsDef(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    // Colors (Vector3)
    val nodColor: Vec3 = Vec3(1f, 0f, 0f),
    val gdiColor: Vec3 = Vec3(1f, 1f, 0f),
    val neutralColor: Vec3 = Vec3(1f, 1f, 1f),
    val mutantColor: Vec3 = Vec3(0f, 1f, 0f),
    val renegadeColor: Vec3 = Vec3(0f, 0f, 1f),
    val primaryObjectiveColor: Vec3 = Vec3(0f, 1f, 0f),
    val secondaryObjectiveColor: Vec3 = Vec3(0f, 0f, 1f),
    val tertiaryObjectiveColor: Vec3 = Vec3(1f, 0f, 1f),
    val healthHighColor: Vec3 = Vec3(0f, 1f, 0f),
    val healthMedColor: Vec3 = Vec3(1f, 1f, 0f),
    val healthLowColor: Vec3 = Vec3(1f, 0f, 0f),
    val enemyColor: Vec3 = Vec3(1f, 0f, 0f),
    val friendlyColor: Vec3 = Vec3(0f, 1f, 0f),
    val noRelationColor: Vec3 = Vec3(1f, 1f, 1f),
    // Star info
    val infoTextureSize: Float = 128f,
    val starBracketSize: Vec2 = Vec2(72f, 56f),
    val starBracketOffset: Vec2 = Vec2(-70f, -58f),
    val starBracketUV: Rect4 = Rect4(55f, 1f, 127f, 57f),
    val starBracketTopSize: Vec2 = Vec2(38f, 16f),
    val starBracketTopOffset: Vec2 = Vec2(-45f, -74f),
    val starBracketTopUV: Rect4 = Rect4(2f, 81f, 40f, 97f),
    val starBracketTopArmedSize: Vec2 = Vec2(38f, 16f),
    val starBracketTopArmedOffset: Vec2 = Vec2(-45f, -74f),
    val starBracketTopArmedUV: Rect4 = Rect4(40f, 81f, 2f, 97f),
    val starBarSize: Vec2 = Vec2(130f, 18f),
    val starBarOffset: Vec2 = Vec2(-198f, -27f),
    val starBarUV: Rect4 = Rect4(55f, 60f, 79f, 78f),
    val starBarEndSize: Vec2 = Vec2(32f, 24f),
    val starBarEndOffset: Vec2 = Vec2(-224f, -29f),
    val starBarEndUV: Rect4 = Rect4(79f, 57f, 111f, 81f),
    val starHealthSize: Vec2 = Vec2(130f, 6f),
    val starHealthOffset: Vec2 = Vec2(-198f, -24f),
    val starHealthUV: Rect4 = Rect4(112f, 66f, 126f, 72f),
    val starShieldSize: Vec2 = Vec2(134f, 6f),
    val starShieldOffset: Vec2 = Vec2(-202f, -17f),
    val starShieldUV: Rect4 = Rect4(112f, 66f, 126f, 72f),
    val starWeaponIconSize: Vec2 = Vec2(64f, 64f),
    val starWeaponIconOffset: Vec2 = Vec2(-70f, -128f),
    // Target info
    val targetBracketSize: Vec2 = Vec2(21f, 52f),
    val targetBracketOffset: Vec2 = Vec2(68f, -60f),
    val targetBracketUV: Rect4 = Rect4(2f, 1f, 23f, 53f),
    val targetIconSize: Vec2 = Vec2(64f, 64f),
    val targetIconOffset: Vec2 = Vec2(2f, -68f),
    val targetNameBarSize: Vec2 = Vec2(120f, 20f),
    val targetNameBarOffset: Vec2 = Vec2(87f, -58f),
    val targetNameBarUV: Rect4 = Rect4(1f, 59f, 31f, 79f),
    val targetNameOffset: Vec2 = Vec2(92f, -52f),
    val targetBarSize: Vec2 = Vec2(130f, 18f),
    val targetBarOffset: Vec2 = Vec2(87f, -27f),
    val targetBarUV: Rect4 = Rect4(55f, 60f, 79f, 78f),
    val targetBarEndSize: Vec2 = Vec2(32f, 24f),
    val targetBarEndOffset: Vec2 = Vec2(211f, -29f),
    val targetBarEndUV: Rect4 = Rect4(111f, 57f, 79f, 81f),
    val targetHealthSize: Vec2 = Vec2(130f, 6f),
    val targetHealthOffset: Vec2 = Vec2(87f, -24f),
    val targetHealthUV: Rect4 = Rect4(126f, 66f, 112f, 72f),
    val targetShieldSize: Vec2 = Vec2(134f, 6f),
    val targetShieldOffset: Vec2 = Vec2(87f, -17f),
    val targetShieldUV: Rect4 = Rect4(126f, 66f, 112f, 72f),
    // Radar
    val radarTextureSize: Float = 128f,
    val radarOffset: Vec2 = Vec2(82f, -124f),
    val radarRadius: Float = 64f,
    val radarFrameSize: Vec2 = Vec2(112f, 128f),
    val radarFrameUV: Rect4 = Rect4(0f, 0f, 112f, 128f),
    val radarCompassOffset: Vec2 = Vec2(-7f, 54f),
    val radarCompassSize: Vec2 = Vec2(16f, 8f),
    val radarCompassBaseUV: Rect4 = Rect4(112f, 64f, 128f, 72f),
    val radarCompassUVOffset: Vec2 = Vec2(0f, 8f),
    val radarHumanBlipUV: Rect4 = Rect4(112f, 0f, 120f, 8f),
    val radarVehicleBlipUV: Rect4 = Rect4(120f, 0f, 128f, 8f),
    val radarStationaryBlipUV: Rect4 = Rect4(112f, 8f, 120f, 16f),
    val radarObjectiveBlipUV: Rect4 = Rect4(120f, 8f, 128f, 16f),
    val radarBlipBracketUV: Rect4 = Rect4(112f, 16f, 120f, 24f),
    val radarSweepUV: Rect4 = Rect4(121f, 24f, 127f, 32f),
    val radarOnSoundId: Int = 0,
    val radarOffSoundId: Int = 0,
    // Sniper
    val sniperTextureSize: Float = 256f,
    val sniperView: Rect4 = Rect4(0.2f, 0.12f, 0.8f, 0.88f),
    val sniperViewUV: Rect4 = Rect4(0f, 0f, 240f, 227f),
    val sniperScanLineUV: Rect4 = Rect4(0.01f, 0.01f, 0.05f, 0.05f),
    val sniperBlackCoverUV: Rect4 = Rect4(0.01f, 0.01f, 0.05f, 0.05f),
    val sniperTiltBar: Rect4 = Rect4(0.20f, 0.25f, 0.225f, 0.75f),
    val sniperTiltBarRate: Float = 1f,
    val sniperTiltBarUV: Rect4 = Rect4(245f, 3f, 250f, 208f),
    val sniperTurnBar: Rect4 = Rect4(0.35f, 0.25f, 0.65f, 0.275f),
    val sniperTurnBarRate: Float = 1f,
    val sniperTurnBarUV: Rect4 = Rect4(1f, 244f, 109f, 253f),
    val sniperDistanceGraph: Rect4 = Rect4(0.175f, 0.3f, 0.2f, 0.65f),
    val sniperDistanceGraphUV: Rect4 = Rect4(1f, 231f, 87f, 239f),
    val sniperDistanceGraphMax: Float = 200f,
    val sniperZoomGraph: Rect4 = Rect4(0.72f, 0.18f, 0.8f, 0.22f),
    val sniperZoomGraphUV: Rect4 = Rect4(218f, 247f, 254f, 254f),
    // Damage indicators
    val damageIndicatorUV: Rect4 = Rect4(31f, 1f, 51f, 59f),
    val damageDiagIndicatorUV: Rect4 = Rect4(1f, 60f, 47f, 106f),
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040603u  // CHUNKID_GLOBAL_SETTINGS_DEF_HUD
    }
}

fun parseHUDGlobalSettingsDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): HUDGlobalSettingsDef? {
    // HUD Save/Load uses CHUNKID_HL_DEF_VARIABLES (803001813) — same as HumanLoiter
    val vars = objDataReader.findChunk(803001813u)
        ?: return HUDGlobalSettingsDef(name = name, id = id, chunkId = chunkId)

    // Micro-chunk IDs from enum (starts at 1)
    return HUDGlobalSettingsDef(
        name = name, id = id, chunkId = chunkId,
        nodColor = vars.mcVec3(1) ?: Vec3(1f, 0f, 0f),
        gdiColor = vars.mcVec3(2) ?: Vec3(1f, 1f, 0f),
        neutralColor = vars.mcVec3(3) ?: Vec3(1f, 1f, 1f),
        primaryObjectiveColor = vars.mcVec3(4) ?: Vec3(0f, 1f, 0f),
        secondaryObjectiveColor = vars.mcVec3(5) ?: Vec3(0f, 0f, 1f),
        tertiaryObjectiveColor = vars.mcVec3(6) ?: Vec3(1f, 0f, 1f),
        starBracketSize = vars.mcVec2(7) ?: Vec2(72f, 56f),
        starBracketOffset = vars.mcVec2(8) ?: Vec2(-70f, -58f),
        starBracketUV = vars.mcRect4(9) ?: Rect4(55f, 1f, 127f, 57f),
        starBracketTopSize = vars.mcVec2(10) ?: Vec2(38f, 16f),
        starBracketTopOffset = vars.mcVec2(11) ?: Vec2(-45f, -74f),
        starBracketTopUV = vars.mcRect4(12) ?: Rect4(2f, 81f, 40f, 97f),
        starBarSize = vars.mcVec2(13) ?: Vec2(130f, 18f),
        starBarOffset = vars.mcVec2(14) ?: Vec2(-198f, -27f),
        starBarUV = vars.mcRect4(15) ?: Rect4(55f, 60f, 79f, 78f),
        starBarEndSize = vars.mcVec2(16) ?: Vec2(32f, 24f),
        starBarEndOffset = vars.mcVec2(17) ?: Vec2(-224f, -29f),
        starBarEndUV = vars.mcRect4(18) ?: Rect4(79f, 57f, 111f, 81f),
        starHealthSize = vars.mcVec2(19) ?: Vec2(130f, 6f),
        starHealthOffset = vars.mcVec2(20) ?: Vec2(-198f, -24f),
        starHealthUV = vars.mcRect4(21) ?: Rect4(112f, 66f, 126f, 72f),
        starShieldSize = vars.mcVec2(22) ?: Vec2(134f, 6f),
        starShieldOffset = vars.mcVec2(23) ?: Vec2(-202f, -17f),
        starShieldUV = vars.mcRect4(24) ?: Rect4(112f, 66f, 126f, 72f),
        targetBracketSize = vars.mcVec2(25) ?: Vec2(21f, 52f),
        targetBracketOffset = vars.mcVec2(26) ?: Vec2(68f, -60f),
        targetBracketUV = vars.mcRect4(27) ?: Rect4(2f, 1f, 23f, 53f),
        targetIconSize = vars.mcVec2(28) ?: Vec2(64f, 64f),
        targetIconOffset = vars.mcVec2(29) ?: Vec2(2f, -68f),
        targetNameBarSize = vars.mcVec2(30) ?: Vec2(120f, 20f),
        targetNameBarOffset = vars.mcVec2(31) ?: Vec2(87f, -58f),
        targetNameBarUV = vars.mcRect4(32) ?: Rect4(1f, 59f, 31f, 79f),
        targetNameOffset = vars.mcVec2(33) ?: Vec2(92f, -52f),
        targetBarSize = vars.mcVec2(34) ?: Vec2(130f, 18f),
        targetBarOffset = vars.mcVec2(35) ?: Vec2(87f, -27f),
        targetBarUV = vars.mcRect4(36) ?: Rect4(55f, 60f, 79f, 78f),
        targetBarEndSize = vars.mcVec2(37) ?: Vec2(32f, 24f),
        targetBarEndOffset = vars.mcVec2(38) ?: Vec2(211f, -29f),
        targetBarEndUV = vars.mcRect4(39) ?: Rect4(111f, 57f, 79f, 81f),
        targetHealthSize = vars.mcVec2(40) ?: Vec2(130f, 6f),
        targetHealthOffset = vars.mcVec2(41) ?: Vec2(87f, -24f),
        targetHealthUV = vars.mcRect4(42) ?: Rect4(126f, 66f, 112f, 72f),
        targetShieldSize = vars.mcVec2(43) ?: Vec2(134f, 6f),
        targetShieldOffset = vars.mcVec2(44) ?: Vec2(87f, -17f),
        targetShieldUV = vars.mcRect4(45) ?: Rect4(126f, 66f, 112f, 72f),
        radarOffset = vars.mcVec2(46) ?: Vec2(82f, -124f),
        radarRadius = vars.mcFloat(47) ?: 64f,
        radarFrameSize = vars.mcVec2(48) ?: Vec2(112f, 128f),
        radarFrameUV = vars.mcRect4(49) ?: Rect4(0f, 0f, 112f, 128f),
        radarCompassOffset = vars.mcVec2(50) ?: Vec2(-7f, 54f),
        radarCompassSize = vars.mcVec2(51) ?: Vec2(16f, 8f),
        radarCompassBaseUV = vars.mcRect4(52) ?: Rect4(112f, 64f, 128f, 72f),
        radarCompassUVOffset = vars.mcVec2(53) ?: Vec2(0f, 8f),
        radarHumanBlipUV = vars.mcRect4(54) ?: Rect4(112f, 0f, 120f, 8f),
        radarVehicleBlipUV = vars.mcRect4(55) ?: Rect4(120f, 0f, 128f, 8f),
        radarStationaryBlipUV = vars.mcRect4(56) ?: Rect4(112f, 8f, 120f, 16f),
        radarObjectiveBlipUV = vars.mcRect4(57) ?: Rect4(120f, 8f, 128f, 16f),
        radarBlipBracketUV = vars.mcRect4(58) ?: Rect4(112f, 16f, 120f, 24f),
        radarSweepUV = vars.mcRect4(59) ?: Rect4(121f, 24f, 127f, 32f),
        starBracketTopArmedSize = vars.mcVec2(60) ?: Vec2(38f, 16f),
        starBracketTopArmedOffset = vars.mcVec2(61) ?: Vec2(-45f, -74f),
        starBracketTopArmedUV = vars.mcRect4(62) ?: Rect4(40f, 81f, 2f, 97f),
        starWeaponIconSize = vars.mcVec2(63) ?: Vec2(64f, 64f),
        starWeaponIconOffset = vars.mcVec2(64) ?: Vec2(-70f, -128f),
        sniperView = vars.mcRect4(65) ?: Rect4(0.2f, 0.12f, 0.8f, 0.88f),
        sniperViewUV = vars.mcRect4(66) ?: Rect4(0f, 0f, 240f, 227f),
        sniperBlackCoverUV = vars.mcRect4(67) ?: Rect4(0.01f, 0.01f, 0.05f, 0.05f),
        sniperTiltBar = vars.mcRect4(68) ?: Rect4(0.20f, 0.25f, 0.225f, 0.75f),
        sniperTiltBarRate = vars.mcFloat(69) ?: 1f,
        sniperTiltBarUV = vars.mcRect4(70) ?: Rect4(245f, 3f, 250f, 208f),
        sniperTurnBar = vars.mcRect4(71) ?: Rect4(0.35f, 0.25f, 0.65f, 0.275f),
        sniperTurnBarRate = vars.mcFloat(72) ?: 1f,
        sniperTurnBarUV = vars.mcRect4(73) ?: Rect4(1f, 244f, 109f, 253f),
        sniperDistanceGraph = vars.mcRect4(74) ?: Rect4(0.175f, 0.3f, 0.2f, 0.65f),
        sniperDistanceGraphUV = vars.mcRect4(75) ?: Rect4(1f, 231f, 87f, 239f),
        sniperDistanceGraphMax = vars.mcFloat(76) ?: 200f,
        sniperScanLineUV = vars.mcRect4(77) ?: Rect4(0.01f, 0.01f, 0.05f, 0.05f),
        infoTextureSize = vars.mcFloat(78) ?: 128f,
        radarTextureSize = vars.mcFloat(79) ?: 128f,
        sniperTextureSize = vars.mcFloat(80) ?: 256f,
        damageIndicatorUV = vars.mcRect4(81) ?: Rect4(31f, 1f, 51f, 59f),
        damageDiagIndicatorUV = vars.mcRect4(82) ?: Rect4(1f, 60f, 47f, 106f),
        sniperZoomGraph = vars.mcRect4(83) ?: Rect4(0.72f, 0.18f, 0.8f, 0.22f),
        sniperZoomGraphUV = vars.mcRect4(84) ?: Rect4(218f, 247f, 254f, 254f),
        radarOnSoundId = vars.mcInt(85) ?: 0,
        radarOffSoundId = vars.mcInt(86) ?: 0,
        mutantColor = vars.mcVec3(87) ?: Vec3(0f, 1f, 0f),
        renegadeColor = vars.mcVec3(88) ?: Vec3(0f, 0f, 1f),
        healthHighColor = vars.mcVec3(89) ?: Vec3(0f, 1f, 0f),
        healthMedColor = vars.mcVec3(90) ?: Vec3(1f, 1f, 0f),
        healthLowColor = vars.mcVec3(91) ?: Vec3(1f, 0f, 0f),
        enemyColor = vars.mcVec3(92) ?: Vec3(1f, 0f, 0f),
        friendlyColor = vars.mcVec3(93) ?: Vec3(0f, 1f, 0f),
        noRelationColor = vars.mcVec3(94) ?: Vec3(1f, 1f, 1f),
    )
}

// =====================================================================================
// HumanAnimOverrideDef
// CLASSID_GLOBAL_SETTINGS_DEF_HUMAN_ANIM_OVERRIDE = 0xF007
// =====================================================================================

data class HumanAnimOverrideDef(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    val runEmptyHands: String = "",
    val walkEmptyHands: String = "",
    val runAtChest: String = "",
    val walkAtChest: String = "",
    val runAtHip: String = "",
    val walkAtHip: String = "",
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040606u  // CHUNKID_GLOBAL_SETTINGS_DEF_HUMAN_ANIM_OVERRIDE
    }
}

fun parseHumanAnimOverrideDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): HumanAnimOverrideDef? {
    // CHUNKID_HAO_DEF_VARIABLES = 726011913
    val vars = objDataReader.findChunk(726011913u)
        ?: return HumanAnimOverrideDef(name = name, id = id, chunkId = chunkId)

    return HumanAnimOverrideDef(
        name = name, id = id, chunkId = chunkId,
        runEmptyHands = vars.mcString(1) ?: "",
        walkEmptyHands = vars.mcString(2) ?: "",
        runAtChest = vars.mcString(3) ?: "",
        walkAtChest = vars.mcString(4) ?: "",
        runAtHip = vars.mcString(5) ?: "",
        walkAtHip = vars.mcString(6) ?: "",
    )
}

// =====================================================================================
// Shared chunk ID for GlobalSettingsDef
// =====================================================================================

// GlobalSettingsDef uses CHUNKID_DEF_PARENT=803001812, CHUNKID_DEF_VARIABLES=803001813
private const val GLOBAL_CHUNKID_DEF_VARIABLES = 803001813u

// =====================================================================================
// Micro-chunk reader helpers
// =====================================================================================

internal fun ChunkReader.mcInt(id: Int): Int? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 4) return null
    return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
}

internal fun ChunkReader.mcFloat(id: Int): Float? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 4) return null
    return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float
}

internal fun ChunkReader.mcBool(id: Int): Boolean? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.isEmpty()) return null
    return bytes[0] != 0.toByte()
}

internal fun ChunkReader.mcString(id: Int): String? {
    val bytes = findMicroChunk(id) ?: return null
    val nullIdx = bytes.indexOfFirst { it == 0.toByte() }
    val len = if (nullIdx < 0) bytes.size else nullIdx
    return String(bytes, 0, len, Charsets.ISO_8859_1)
}

/** Reads all micro-chunks with the given [id] as strings (for lists). */
internal fun ChunkReader.mcStringList(id: Int): List<String> {
    val result = mutableListOf<String>()
    forEachMicroChunk { mId, bytes ->
        if (mId == id) {
            val nullIdx = bytes.indexOfFirst { it == 0.toByte() }
            val len = if (nullIdx < 0) bytes.size else nullIdx
            result += String(bytes, 0, len, Charsets.ISO_8859_1)
        }
    }
    return result
}

internal fun ChunkReader.mcVec2(id: Int): Vec2? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 8) return null
    val b = ByteBuffer.wrap(bytes, 0, 8).order(ByteOrder.LITTLE_ENDIAN)
    return Vec2(b.getFloat(0), b.getFloat(4))
}

internal fun ChunkReader.mcVec3(id: Int): Vec3? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 12) return null
    val b = ByteBuffer.wrap(bytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
    return Vec3(b.getFloat(0), b.getFloat(4), b.getFloat(8))
}

internal fun ChunkReader.mcRect4(id: Int): Rect4? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 16) return null
    val b = ByteBuffer.wrap(bytes, 0, 16).order(ByteOrder.LITTLE_ENDIAN)
    return Rect4(b.getFloat(0), b.getFloat(4), b.getFloat(8), b.getFloat(12))
}

/**
 * Iterates over all micro-chunks in this reader, calling [handler] with the micro-chunk ID
 * and raw data bytes for each one.
 */
internal fun ChunkReader.forEachMicroChunk(handler: (id: Int, data: ByteArray) -> Unit) {
    val allBytes = readBytes()
    var pos = 0
    while (pos + 2 <= allBytes.size) {
        val mType = allBytes[pos].toInt() and 0xFF
        val mSize = allBytes[pos + 1].toInt() and 0xFF
        pos += 2
        if (pos + mSize > allBytes.size) break
        handler(mType, allBytes.copyOfRange(pos, pos + mSize))
        pos += mSize
    }
}
