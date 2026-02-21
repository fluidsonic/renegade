# Phase 2A: Game Object Spawning — MIX/LSD Parser + Soldier Spawn

## Context

Phase 1 is complete (95 tests passing). Clients can connect and the handshake completes
(ACCEPT, TEAMs, GAMEOPTIONSEVENT, PLAYER), but the client stays on "Gameplay is pending"
because:

1. `COMBAT_STAR == NULL` — no soldier object has been spawned
2. The client sets `COMBAT_STAR` only when it receives a GAMEOBJ creation packet with
   `control_owner == my_id` (in `SmartGameObj::Import_Creation`)

To spawn a soldier, the server must send a `NETCLASSID_GAMEOBJ` (1000) creation packet with
position quantized using the level's world extents. The extents live inside the `.lsd` file
embedded in the map `.mix` archive.

**Goal**: Parse the map MIX to extract world extents, configure position encoding, and
spawn a soldier on LOADINGEVENT so the client clears "Gameplay is pending."

---

## Part 1: MIX File Reader

**File**: `server/src/main/kotlin/ccr/server/mix/MixReader.kt`

MIX1 format (from `wwlib/mixfile.cpp`):

```
Bytes 0-3:    "MIX1" signature
Bytes 4-7:    header_offset (int32 LE)
Bytes 8-11:   names_offset (int32 LE)
Bytes 12-15:  unused (int32 LE, = 0)
Bytes 16+:    file data (each file padded to 8-byte alignment)

At header_offset:
  file_count (int32 LE)
  entries[file_count]:  (sorted by CRC as unsigned)
    CRC (uint32 LE)     — CRC_Stringi(filename)
    Offset (uint32 LE)  — absolute from file start
    Size (uint32 LE)
```

**API**:
```kotlin
class MixReader(data: ByteArray) {
    fun readFile(name: String): ByteArray?  // returns null if not found
    fun fileCount(): Int
}
```

File lookup: `crcStringi(name)` → binary search on entries → read `data[offset..offset+size]`.

Reuse: `crcStringi` from `ccr.server.ServerConfig` (move to shared location or call directly).

**Test**: `server/src/test/kotlin/ccr/server/mix/MixReaderTest.kt`
- Construct a minimal MIX1 byte array with known files, verify read-back
- Verify binary search finds correct entry
- Verify missing file returns null

---

## Part 2: Chunk/LSD Reader

**File**: `server/src/main/kotlin/ccr/server/mix/ChunkReader.kt`

W3D/LSD chunk format (from `wwlib/chunkio.h`):

```
Chunk header (8 bytes):
  chunkType: uint32 LE
  chunkSize: uint32 LE  (bit 31 = has_sub_chunks; lower 31 bits = data size)

MicroChunk header (2 bytes):
  type: uint8
  size: uint8
```

**API**:
```kotlin
class ChunkReader(data: ByteArray, offset: Int = 0, length: Int = data.size - offset) {
    // Iterate sub-chunks at current level
    fun forEachChunk(handler: (id: UInt, isContainer: Boolean, reader: ChunkReader) -> Unit)
    // Find first chunk with given ID
    fun findChunk(id: UInt): ChunkReader?
    // Read micro-chunks
    fun findMicroChunk(id: Int): ByteArray?
    // Direct reads
    fun readInt(): Int
    fun readFloat(): Float
}
```

**Test**: `server/src/test/kotlin/ccr/server/mix/ChunkReaderTest.kt`
- Parse hand-crafted chunk hierarchy
- Nested chunk navigation
- Micro-chunk extraction

---

## Part 3: Level Extent Extraction

**File**: `server/src/main/kotlin/ccr/server/mix/LevelExtents.kt`

The .lsd file inside the .mix contains the AABTree root bounding box.

**Chunk hierarchy to navigate** (all IDs are uint32 LE):

```
[0x00020000]  PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM (SaveLoadSystemClass wrapper)
  [0x04433220]  PSDSSC_CHUNKID_SCENE
    [0x00004500]  PSCENE_SD_CHUNK_STATIC_OBJECT_AABTREE
      [0x00000104]  STATICAABTREE_CHUNK_AABTREE_CLASS_DATA
        [0x00000001]  AABTREE_CHUNK_VERSION  → uint32 (expect 0x00010000)
        [0x00000101]  AABTREE_CHUNK_AABNODE  (root node)
          [0x00000104]  AABTREE_CHUNK_AABNODE_VARIABLES  (new format)
            micro[0x00]  AABTREE_VARIABLE_NODESTRUCT → IOAABNodeStruct
          OR
          [0x00000102]  AABTREE_CHUNK_AABNODE_INFO  (legacy format, direct struct)
```

**IOAABNodeStruct** (28 bytes):
```
Center: { X: float32, Y: float32, Z: float32 }   (12 bytes)
Extent: { X: float32, Y: float32, Z: float32 }   (12 bytes)
Attributes: uint32                                  (4 bytes)
```

World bounds: `min = Center - Extent`, `max = Center + Extent`

**API**:
```kotlin
data class WorldExtents(
    val minX: Float, val minY: Float, val minZ: Float,
    val maxX: Float, val maxY: Float, val maxZ: Float,
)

fun extractLevelExtents(lsdData: ByteArray): WorldExtents
```

**Extraction path** (from map name):
1. Open `C&C_Under.mix` → `MixReader`
2. Read `C&C_Under.lsd` (map name with `.mix` → `.lsd`)
3. Navigate chunks to AABTree root → extract Center ± Extent

**Test**: Build a minimal .lsd byte array with the correct chunk nesting and verify extraction.

---

## Part 4: Position Encoder Configuration

**File**: `server/src/main/kotlin/ccr/server/GameServer.kt` (integration)

Existing code to reuse:
- `EncoderRegistry.setPrecision(type, min, max, resolution)` in `ccr.net.bitstream.EncoderRegistry`
- `BitStream.addFloat(value, encoderType)` — already handles quantized encoding

Add BITPACK constants (from `wwbitpack/bitpackids.h`):
```kotlin
const val BITPACK_WORLD_POSITION_X = 0
const val BITPACK_WORLD_POSITION_Y = 1
const val BITPACK_WORLD_POSITION_Z = 2
```

Configuration (from `combatgmode.cpp:1068-1073`):
```kotlin
val margin = 1.0
EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X,
    extents.minX - margin, extents.maxX + margin, 0.2)
EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y,
    extents.minY - margin, extents.maxY + margin, 0.2)
EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z,
    extents.minZ - margin, extents.maxZ + margin, 0.2)
```

**Note**: The Kotlin `EncoderRegistry.setPrecision` bit-width calculation may differ slightly
from C++ (`Calc_Bit_Precision` uses a specific max_units loop and recalculates resolution).
Must verify the bit counts match for C&C_Under dimensions. Fix if they diverge.

---

## Part 5: Soldier Spawn on LOADINGEVENT

**File**: `server/src/main/kotlin/ccr/server/GameServer.kt`

### Soldier Definition ID

The server creates a soldier via `cGod::Create_Commando` with presets:
- NOD: `"CnC_Nod_Minigunner_0"`
- GDI: `"CnC_GDI_MiniGunner_0"`

Definition IDs come from the game's preset database (compiled into `.dat` files inside
`always.mix`). For Phase 2A, **hardcode the definition IDs** after discovering them
experimentally from a real game capture or the preset files. This avoids needing to parse
the full definition manager save format.

TODO: Capture real definition IDs from game files. If unknown, we can log the raw bytes
from a real server and extract them.

### Creation Packet Structure

The soldier is `NETCLASSID_GAMEOBJ = 1000`. Wire format for BIT_CREATION (dirtyBits=0x0F):

```
--- Network object header ---
[networkId: int32]           ← unique ID (e.g. 200000 + rhostId)
[dirtyBits: byte]            ← 0x0F (BIT_CREATION = all tiers)
[isDeletePending: bool]      ← false

--- BIT_CREATION (0x08) ---
[classId: int32]             ← 1000 (NETCLASSID_GAMEOBJ)
[definitionId: int32]        ← factory prep (preset ID from game data)

--- Export_Creation chain ---
# BaseGameObj → ScriptableGameObj → DamageableGameObj: nothing
# PhysicalGameObj::Export_Creation:
[posX: float quantized]      ← BITPACK_WORLD_POSITION_X
[posY: float quantized]      ← BITPACK_WORLD_POSITION_Y
[posZ: float quantized]      ← BITPACK_WORLD_POSITION_Z
[facing: float]              ← uncompressed IEEE 754

# SmartGameObj::Export_Creation:
[control_owner: int32]       ← rhostId (THIS sets COMBAT_STAR on client)

# SoldierGameObj::Export_Creation: nothing extra

--- BIT_RARE (0x04) ---
# PhysicalGameObj::Export_Rare:
[model_name: terminated string]   ← e.g. "s_a_human" (soldier model)
[anim_name: terminated string]    ← "" (empty)
[curr_frame: int32]               ← 0
[target_frame: int32]             ← 0
[anim_mode: int32]                ← 0 (ANIM_MODE_TARGET)
[host_model_id: int32]            ← 0 (no host)
[host_bone: int32]                ← 0
[player_type: int32]              ← 0=NOD, 1=GDI
[hud_pokable: bool]               ← false
# (no hidden field — soldier is not a vehicle)

# SoldierGameObj::Export_Rare:
[definition_id: uint32]           ← same as factory prep definitionId

--- BIT_OCCASIONAL (0x02) ---
# DamageableGameObj::Export_Occasional → DefenseObject::Export
[health/shield data]              ← need to check DefenseObject::Export format

--- BIT_FREQUENT (0x01) ---
# PhysicalGameObj::Export_Frequent (position repeat)
# ArmedGameObj::Export_Frequent (targeting position)
# SmartGameObj::Export_Frequent (control state)
```

### Spawn Position

Phase 2A: hardcode a known spawn position for C&C_Under (e.g. center of map or a known
infantry spawn point). Proper spawn point lookup from level data is Phase 2B.

### Integration

In `GameServer.processInbound`, after LOADINGEVENT (isLoading=false):
```
1. playerInGame.add(rhostId)
2. sendPlayerRareUpdate(isInGame=true)
3. sendGameDataUpdateEvent
4. spawnSoldier(rhostId, team)  ← NEW: send GAMEOBJ creation packet
```

---

## Part 6: Fix EncoderRegistry Bit Precision (if needed)

The C++ `Calc_Bit_Precision` algorithm:
```
units = ceil((max - min) / resolution - epsilon) + 1
BitPrecision = 0, max_units = 0
while (max_units < units):
    max_units += 1 << BitPrecision
    BitPrecision++
    if BitPrecision == 1: max_units++  // special: goes to 2 instead of 1
Resolution = (max - min) / (max_units - 1)  // adjusted!
```

The Kotlin version uses `bits = ceil(log2(steps))` which may differ. Compare outputs for
C&C_Under extents and fix if they don't match.

**Scale**: `scaled = round((value - min) / Resolution)`
**Unscale**: `value = min + scaled * Resolution`

The `Resolution` recalculation is important — the C++ code adjusts it to exactly fit
`max_units - 1` steps into the bit range. Both sides (server encoding, client decoding)
must agree.

---

## Implementation Order

| # | What | File(s) | Depends on |
|---|------|---------|------------|
| 1 | MixReader | `server/.../mix/MixReader.kt` | `crcStringi` (exists) |
| 2 | MixReader test | `server/.../mix/MixReaderTest.kt` | #1 |
| 3 | ChunkReader | `server/.../mix/ChunkReader.kt` | nothing |
| 4 | ChunkReader test | `server/.../mix/ChunkReaderTest.kt` | #3 |
| 5 | LevelExtents | `server/.../mix/LevelExtents.kt` | #1, #3 |
| 6 | LevelExtents test | `server/.../mix/LevelExtentsTest.kt` | #5 |
| 7 | BITPACK constants | `net/.../bitstream/BitPackIds.kt` | nothing |
| 8 | Fix EncoderRegistry | `net/.../bitstream/EncoderRegistry.kt` | verify first |
| 9 | Soldier spawn in GameServer | `server/.../GameServer.kt` | #5, #7, #8 |
| 10 | Integration test with real MIX | manual | #9 + game files |

---

## Verification

1. `./gradlew :server:test` — new MixReader/ChunkReader/LevelExtents tests pass
2. `./gradlew :net:test` — existing 95+ tests still pass
3. `./gradlew test` — full suite green
4. Manual: start server with C&C_Under.mix in data path, connect with Renegade client
   - Verify "Gameplay is pending" clears
   - Verify soldier appears on screen
   - Verify player can look around / move

## Open Questions

1. **Definition IDs**: Need to discover the actual integer IDs for `CnC_GDI_MiniGunner_0`
   and `CnC_Nod_Minigunner_0`. Options: capture from real server, parse `always.dat`,
   or use community knowledge. May need to hardcode temporarily.
2. **DefenseObject::Export format**: Need to trace this for BIT_OCCASIONAL health/shield
   data. May be able to skip BIT_OCCASIONAL/FREQUENT on first creation and send a
   separate update.
3. **Spawn position**: Hardcode for C&C_Under initially. Need map-specific spawn points
   later.
4. **Model name**: Need the correct model name string for the minigunner soldier mesh
   (e.g. `"s_a_human"` or similar). Must match what the client expects.
5. **Map file location**: Where does the server find C&C_Under.mix? Config path or
   working directory? Add a `dataPath` config option.

## Source References

- MIX format: `original/Code/wwlib/mixfile.cpp` lines 49-56 (header), 209-252 (Get_File)
- MIX creation: `original/Code/wwlib/mixfile.cpp` lines 408-495 (MixFileCreator)
- Chunk format: `original/Code/wwlib/chunkio.h`
- AABTree load: `original/Code/WWMath/aabtreecull.cpp` lines 700-800
- Static save: `original/Code/wwphys/staticaabtreecull.cpp` lines 670-698
- Scene save: `original/Code/wwphys/pscene_saveload.cpp` lines 135-179
- Encoder: `original/Code/wwbitpack/encodertypeentry.cpp` lines 69-186
- Soldier export: `original/Code/Combat/soldier.cpp` lines 904-947
- Physical export: `original/Code/Combat/physicalgameobj.cpp` lines 1013-1044
- Smart export: `original/Code/Combat/smartgameobj.cpp` lines 962-971
- God create: `original/Code/Commando/god.cpp` lines 283-396
- Position setup: `original/Code/Commando/combatgmode.cpp` lines 1063-1076
- Bitpack IDs: `original/Code/wwbitpack/bitpackids.h` (WORLD_POSITION_X=0, Y=1, Z=2)
