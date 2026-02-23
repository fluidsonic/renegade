package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of EvaSettingsDefClass (Combat/evasettings.h).
 *
 * All fields are RectClass (4 floats) or Vector2 (2 floats) for UI layout.
 */
class EvaSettingsDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    // Objectives viewer
    val objectivesScreenRect: Rect4 = Rect4(0.063f, 0.25f, 0.938f, 0.75f),
    val objectivesTextRect: Rect4 = Rect4(0.1f, 0.260f, 0.906f, 0.555f),
    val objectivesEndcapUVRect: Rect4 = Rect4(0f, 67f, 8f, 127f),
    val objectivesFadeoutUVRect: Rect4 = Rect4(41f, 34f, 127f, 65f),
    val objectivesBackgroundUVRect: Rect4 = Rect4(2f, 1f, 126f, 32f),
    val objectivesTextureSize: Vec2 = Vec2(128f, 128f),
    // Messages window
    val messagesScreenRect: Rect4 = Rect4(0.116f, 0.021f, 0.938f, 0.208f),
    val messagesTextRect: Rect4 = Rect4(0.147f, 0.031f, 0.906f, 0.198f),
    val messagesEndcapUVRect: Rect4 = Rect4(0f, 67f, 8f, 127f),
    val messagesFadeoutUVRect: Rect4 = Rect4(41f, 34f, 127f, 65f),
    val messagesBackgroundUVRect: Rect4 = Rect4(2f, 1f, 126f, 32f),
    val messagesTextureSize: Vec2 = Vec2(128f, 128f),
    val messagesIconPos: Vec2 = Vec2(0.016f, 0.021f),
) : DefinitionClass(name, id, chunkId) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040604u  // CHUNKID_GLOBAL_SETTINGS_DEF_EVA
    }
}

// Chunk IDs from evasettings.cpp enum (line 30)
private const val CHUNKID_EVA_VARIABLES = 803001813u // CHUNKID_PARENT(803001812) + 1

// Micro-chunk IDs (enum starting at 1)
private const val VARID_OBJECTIVESSCREENRECT = 1
private const val VARID_OBJECTIVESTEXTRECT = 2
private const val VARID_OBJECTIVESENDCAPUVRECT = 3
private const val VARID_OBJECTIVESFADEOUTUVRECT = 4
private const val VARID_OBJECTIVESBACKGROUNDUVRECT = 5
private const val VARID_OBJECTIVESTEXTURESIZE = 6
private const val VARID_MESSAGESSCREENRECT = 7
private const val VARID_MESSAGESTEXTRECT = 8
private const val VARID_MESSAGESENDCAPUVRECT = 9
private const val VARID_MESSAGESFADEOUTUVRECT = 10
private const val VARID_MESSAGESBACKGROUNDUVRECT = 11
private const val VARID_MESSAGESTEXTURESIZE = 12
private const val VARID_MESSAGESICONPOS = 13

fun parseEvaSettingsDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): EvaSettingsDefClass? {
    val vars = objDataReader.findChunk(CHUNKID_EVA_VARIABLES)
        ?: return EvaSettingsDefClass(name = name, id = id, chunkId = chunkId)

    return EvaSettingsDefClass(
        name = name, id = id, chunkId = chunkId,
        objectivesScreenRect = vars.mcRect4(VARID_OBJECTIVESSCREENRECT)
            ?: Rect4(0.063f, 0.25f, 0.938f, 0.75f),
        objectivesTextRect = vars.mcRect4(VARID_OBJECTIVESTEXTRECT)
            ?: Rect4(0.1f, 0.260f, 0.906f, 0.555f),
        objectivesEndcapUVRect = vars.mcRect4(VARID_OBJECTIVESENDCAPUVRECT)
            ?: Rect4(0f, 67f, 8f, 127f),
        objectivesFadeoutUVRect = vars.mcRect4(VARID_OBJECTIVESFADEOUTUVRECT)
            ?: Rect4(41f, 34f, 127f, 65f),
        objectivesBackgroundUVRect = vars.mcRect4(VARID_OBJECTIVESBACKGROUNDUVRECT)
            ?: Rect4(2f, 1f, 126f, 32f),
        objectivesTextureSize = vars.mcVec2(VARID_OBJECTIVESTEXTURESIZE)
            ?: Vec2(128f, 128f),
        messagesScreenRect = vars.mcRect4(VARID_MESSAGESSCREENRECT)
            ?: Rect4(0.116f, 0.021f, 0.938f, 0.208f),
        messagesTextRect = vars.mcRect4(VARID_MESSAGESTEXTRECT)
            ?: Rect4(0.147f, 0.031f, 0.906f, 0.198f),
        messagesEndcapUVRect = vars.mcRect4(VARID_MESSAGESENDCAPUVRECT)
            ?: Rect4(0f, 67f, 8f, 127f),
        messagesFadeoutUVRect = vars.mcRect4(VARID_MESSAGESFADEOUTUVRECT)
            ?: Rect4(41f, 34f, 127f, 65f),
        messagesBackgroundUVRect = vars.mcRect4(VARID_MESSAGESBACKGROUNDUVRECT)
            ?: Rect4(2f, 1f, 126f, 32f),
        messagesTextureSize = vars.mcVec2(VARID_MESSAGESTEXTURESIZE)
            ?: Vec2(128f, 128f),
        messagesIconPos = vars.mcVec2(VARID_MESSAGESICONPOS)
            ?: Vec2(0.016f, 0.021f),
    )
}
