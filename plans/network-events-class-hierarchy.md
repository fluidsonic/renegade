# Network Events Class Hierarchy + Unit Tests

## Status: Implemented ✓

## Context

GameServer.kt previously encoded all network events (GameOptionsEvent, GameDataUpdateEvent, Teams, Player)
inline in lambda closures passed to `sendGameNetObj()`. This made the code hard to test and
didn't match the C++ class hierarchy. Proper Kotlin classes now mirror C++:

```
NetworkObjectClass (wwnet/) — already exists as ccr.net.replication.NetworkObject
  └→ cNetEvent — empty Export_Creation base for one-shot events
       ├→ cGameOptionsEvent — sends Tier1 + Tier2 + event-specific fields
       └→ cGameDataUpdateEvent — sends TimeRemainingSeconds(INT) + HostedGameNumber

cGameData — game settings container (NOT a NetworkObject)
  └→ cGameDataCnc — CnC-specific Tier2 extension (always active in this server)
```

**Bug fixed**: `sendGameDataUpdateEvent` previously used `addFloat(0f)` for TimeRemainingSeconds, but C++
declares it as `int`. Invisible when value=0 (same bit pattern), but broken for any non-zero value.

## Files Created

- `server/src/main/kotlin/ccr/server/net/GameData.kt` — mutable settings container with exportTier1/exportTier2
- `server/src/main/kotlin/ccr/server/net/NetEvent.kt` — abstract base for one-shot events
- `server/src/main/kotlin/ccr/server/net/NetworkObjectPacketWriter.kt` — wire envelope writer
- `server/src/main/kotlin/ccr/server/net/GameOptionsEvent.kt` — classId=1008, exportCreation with tier1+tier2+float time
- `server/src/main/kotlin/ccr/server/net/GameDataUpdateEvent.kt` — classId=1012, exportCreation with int time (bug fix)
- `server/src/test/kotlin/ccr/server/net/GameDataTest.kt` — bit count + round-trip tests
- `server/src/test/kotlin/ccr/server/net/GameOptionsEventTest.kt` — 807 bit count + field verification
- `server/src/test/kotlin/ccr/server/net/GameDataUpdateEventTest.kt` — 137 bit count + int vs float validation

## Files Modified

- `server/src/main/kotlin/ccr/server/GameServer.kt` — replaced inline lambdas with event classes
- `server/src/main/kotlin/ccr/server/mix/LevelExtents.kt` — added AABTree version validation (bug fix)

## Design Notes

### networkId visibility
`NetworkObject.networkId` has `internal set` scoped to the `net` Gradle module. Since `server` module
code cannot set it directly, `NetworkObjectPacketWriter.writeCreation()` accepts `networkId` as an
explicit parameter. This matches C++ architecture where `Send_Object_Update` in messages.cpp controls
framing, not the objects themselves.

### Bit Count Reference

**Header** (written by NetworkObjectPacketWriter for creation):
networkId(32) + dirtyBits(8) + isDeletePending(1) + classId(32) = **73 bits**

**Tier 1** (owner="Server", gameTitle="", compression on → bools are 1 bit):
ipAddr(32) + owner(16+96=112) + gameTitle(16) + port(32) + currentPlayers(32)
+ maxPlayers(32) + version(32) + exeCrc(32) + stringsCrc(32) + 5×bool(5)
+ mapCrc(32) + modCrc(32) = **421 bits**

**Tier 2** (motd="", Is_Cnc=true):
4×int(128) + 4×bool(4) + 3×cncBool(3) + motd(16) + 2×cncBool(2) + credits(32) = **185 bits**

**GameOptionsEvent event-specific**:
timeRemaining_float(32) + hostedGameNumber(32) + modCrc(32) + mapCrc(32) = **128 bits**

**Total GameOptionsEvent**: 73 + 421 + 185 + 128 = **807 bits**
**Total GameDataUpdateEvent**: 73 + 32 + 32 = **137 bits**

## C++ Source References

- `netevent.h:11` — `class cNetEvent : public NetworkObjectClass`
- `gameoptionsevent.cpp:62-85` — `Export_Creation`: tier1 + tier2 + float time + int hosted + crcs
- `gamedataupdateevent.h:27` — `int TimeRemainingSeconds` (INT, not float)
- `gamedataupdateevent.cpp:62-70` — `Export_Creation`: int time + int hosted
- `gamedata.cpp:776-803` — `Export_Tier_1_Data`
- `gamedata.cpp:917-936` — `Export_Tier_2_Data` (includes Is_Cnc conditional)
- `gdcnc.cpp:164-171` — `cGameDataCnc::Export_Tier_2_Data` (calls parent + 3 fields)
