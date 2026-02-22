package ccr.server.level.ldd

import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.defs.readMicroWideString
import ccr.server.mix.ChunkReader

object LevelInfoLoader {

    fun load(reader: ChunkReader): LevelInfo {
        val mapFilename = reader.readMicroString(1) ?: ""
        val missionDescriptionId = reader.readMicroInt(2) ?: 0
        // C++ writes micro 3 with WRITE_MICRO_CHUNK_WIDESTRING (UTF-16LE)
        val description = reader.readMicroWideString(3) ?: ""
        return LevelInfo(mapFilename, missionDescriptionId, description)
    }
}
