# Plan: Mirror C++ Server Network Architecture

## Context

The Renegade client freezes because the Kotlin server doesn't send DELETE_PENDING for incoming C→S events. Beyond that, the entire event handling architecture deviates from C++: events are handled inline in a giant `when` block, S→C events are sent inline instead of through replication, and CClientControl/CClientFps are not proper NetworkObjects. This plan restructures everything to mirror C++ exactly.

## Steps

### Step 1: Add `act()` to NetEvent base class
### Step 2: Create `dispatchCsPacket()` — mirrors Server_Packet_Handler
### Step 3: Register proper factories for all C→S classes
### Step 4: Move all handler logic into event `act()` methods
### Step 5: CClientControl + CClientFps as proper persistent NetworkObjects
### Step 6: S→C events via replication (not inline sends)
### Step 7: Add `NetworkObjectManager.deletePending()` to tick loop
### Step 8: Make GameServer members `internal`
### Step 9: Add `findObject()` to NetworkObjectManager (already exists)
### Step 10: Remove `handleFrequentUpdate()` and `handleGamePacket()`
### Step 11: Re-investigate building packet size

See conversation transcript for full details of each step.
