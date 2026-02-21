# Game Object Spawning Design — MIX/LSD Parser + Soldier Spawn

## Problem

After Phase 1, clients connect and complete the handshake but stay on "Gameplay is pending"
because:
1. `cGameDataCnc::Is_Gameplay_Permitted()` needs both NOD+GDI players (fixed: MaxPlayers=1)
2. `COMBAT_STAR == NULL` — no soldier object has been spawned for the player

The client sets `COMBAT_STAR` in `SmartGameObj::Import_Creation` when `control_owner == my_id`.

## Solution Overview

1. **Parse map MIX archive** to extract the `.lsd` (Level Static Data) file
2. **Navigate chunk hierarchy** in the `.lsd` to find the AABTree root bounding box
3. **Configure position encoders** using world extents (min/max + margin=1, resolution=0.2)
4. **Spawn a soldier** on LOADINGEVENT with `control_owner = rhostId`

## MIX1 File Format

From `wwlib/mixfile.cpp`:

```
Offset  Size  Field
0       4     "MIX1" signature
4       4     header_offset (int32 LE) — points to file index
8       4     names_offset (int32 LE) — points to filename list
12      4     unused (zero)
16+           file data (8-byte aligned)

At header_offset:
  4     file_count (int32 LE)
  12×N  entries sorted by CRC (unsigned):
    4     CRC (uint32 LE) — CRC_Stringi(filename)
    4     Offset (uint32 LE) — absolute from file start
    4     Size (uint32 LE)
```

File lookup: compute `crcStringi(filename)`, binary search entries, read data.

## LSD Chunk Hierarchy

Level data is saved via `SaveLoadSystemClass::Save(csave, _PhysStaticDataSaveSystem)`.
The chunk nesting path to the AABTree root:

```
[0x00020000]  PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM
  [0x04433220]  PSDSSC_CHUNKID_SCENE
    [0x00004500]  PSCENE_SD_CHUNK_STATIC_OBJECT_AABTREE
      [0x00000104]  STATICAABTREE_CHUNK_AABTREE_CLASS_DATA
        [0x00000001]  AABTREE_CHUNK_VERSION (data: uint32 = 0x00010000)
        [0x00000101]  AABTREE_CHUNK_AABNODE (root)
          [0x00000104] or [0x00000102]  AABNODE_VARIABLES or AABNODE_INFO
            → IOAABNodeStruct { Center[3f], Extent[3f], Attributes[u32] }
```

`WorldExtents: min = Center - Extent, max = Center + Extent`

## Chunk Format

Each chunk: `[type: u32][size: u32]` where size bit 31 = has sub-chunks.
MicroChunks: `[type: u8][size: u8]`.

## Position Encoding

From `combatgmode.cpp:1068-1073`:
```
Set_Precision(BITPACK_WORLD_POSITION_X, min.X - 1, max.X + 1, 0.2)
Set_Precision(BITPACK_WORLD_POSITION_Y, min.Y - 1, max.Y + 1, 0.2)
Set_Precision(BITPACK_WORLD_POSITION_Z, min.Z - 1, max.Z + 1, 0.2)
```

BITPACK_WORLD_POSITION_X/Y/Z = enum values 0/1/2 (from `bitpackids.h`).

## Soldier Creation Packet

`NETCLASSID_GAMEOBJ = 1000` with BIT_CREATION (dirtyBits=0x0F):

```
[networkId] [dirtyBits=0x0F] [isDeletePending=false]
[classId=1000] [definitionId]  ← factory prep: preset ID
[posX quantized] [posY quantized] [posZ quantized] [facing float]
[control_owner = rhostId]  ← triggers COMBAT_STAR on client
+ BIT_RARE: model_name, anim, player_type, definition_id
+ BIT_OCCASIONAL: defense object (health/shield)
+ BIT_FREQUENT: position + targeting + control state
```

Preset names: `"CnC_GDI_MiniGunner_0"` (GDI), `"CnC_Nod_Minigunner_0"` (NOD).
