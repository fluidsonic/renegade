# Phase 10 Implementation Plan: Bandwidth + Polish

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Wire up the existing-but-unused bandwidth infrastructure (PacketCombiner batching, FlowController, BandwidthBudget, packet-loss tracking) so the server coalesces packets per tick and throttles unreliable traffic on slow connections.

**Architecture:** Three sequential improvements — (1) batch all outgoing packets per host into fewer UDP datagrams, (2) wire the per-host flow controller to gate BIT_FREQUENT sends under load, (3) populate `host.packetLossPc` from reliable-channel resend statistics.

**Tech Stack:** Kotlin, `ccr.net.flow`, `ccr.net.reliability`, `ccr.net.protocol.PacketCombiner`, `ccr.server.GameServer`

---

## Context

The infrastructure is mostly in place but not wired:
- `PacketCombiner.combine()` accepts a list of packets and groups them into fewer datagrams — but is called with a single-element list every time.
- `BandwidthBudget` divides total server bandwidth among hosts — never instantiated in `GameServer`.
- `FlowController.adjust()` and `shouldSend()` exist — never called.
- `RemoteHost.packetLossPc` is sent in keepalives to clients — never populated on the server.

## Key Files

| File | Purpose |
|------|---------|
| `server/src/main/kotlin/ccr/server/GameServer.kt` | Main server — `sendGameNetObj`, `sendUnreliable`, `replicationTick`, `networkTickLoop` |
| `net/src/main/kotlin/ccr/net/protocol/PacketCombiner.kt` | Groups packets into combined datagrams |
| `net/src/main/kotlin/ccr/net/flow/BandwidthBudget.kt` | Divides total bw among connected hosts |
| `net/src/main/kotlin/ccr/net/flow/FlowController.kt` | Per-host: `adjust(targetBps, actualBps, sampleMs)` + `shouldSend(priority)` |
| `net/src/main/kotlin/ccr/net/reliability/ReliableChannel.kt` | Reliable send queue — needs send/resend counters |
| `net/src/main/kotlin/ccr/net/connection/RemoteHost.kt` | Has `packetLossPc: Float` (needs to be populated) |
| `net/src/main/kotlin/ccr/net/connection/ConnectionManager.kt` | `getKeepalives()` sends `host.packetLossPc` — already wired to clients |
| `server/src/main/kotlin/ccr/server/ServerConfig.kt` | Has `bandwidthBps: Int` (INI `BandwidthBps`) |

## Current Behavior (sendGameNetObj / sendUnreliable)

```kotlin
// TODAY: one UDP datagram per call
fun sendGameNetObj(host: RemoteHost, writePayload: (BitStream) -> Unit) {
    ...
    enqueueWithCrc(PacketCombiner.combine(listOf(host.address to wireData)))  // single-element list
}
```

During one `replicationTick()` with 16 objects × 4 clients = 64 immediate datagrams.

**Goal:** buffer into `pendingOutbox`, flush once after the tick → as few datagrams as MTU allows.

---

## Task 1: Per-tick packet batching + byte counting

**Files:**
- Modify: `server/src/main/kotlin/ccr/server/GameServer.kt`
- Test: `server/src/test/kotlin/ccr/server/PacketBatchingTest.kt` (NEW)

### Step 1: Add outbox fields to GameServer

Insert near the other per-tick state fields (around the `lastGameDataUpdateMs` declaration area):

```kotlin
// Per-tick outbox: buffered packets awaiting end-of-tick flush (Task 1: batching)
private val pendingOutbox = mutableMapOf<Int, MutableList<Pair<InetSocketAddress, ByteArray>>>()
private val bytesSentThisTick = mutableMapOf<Int, Int>()
```

### Step 2: Buffer instead of send immediately in sendGameNetObj

Replace the last line of `sendGameNetObj` (the `enqueueWithCrc` call):

```kotlin
// BEFORE:
enqueueWithCrc(PacketCombiner.combine(listOf(host.address to wireData)))

// AFTER:
pendingOutbox.getOrPut(host.id) { mutableListOf() }.add(host.address to wireData)
```

### Step 3: Buffer in sendUnreliable too

Replace the `enqueueWithCrc` call in `sendUnreliable`:

```kotlin
// BEFORE:
enqueueWithCrc(PacketCombiner.combine(listOf(host.address to wireData)))

// AFTER:
pendingOutbox.getOrPut(host.id) { mutableListOf() }.add(host.address to wireData)
```

### Step 4: Add flushOutbox()

Add a new private function after `sendUnreliable`:

```kotlin
private fun flushOutbox() {
    for ((rhostId, packets) in pendingOutbox) {
        val datagrams = PacketCombiner.combine(packets)
        enqueueWithCrc(datagrams)
        bytesSentThisTick[rhostId] = (bytesSentThisTick[rhostId] ?: 0) + datagrams.sumOf { it.data.size }
    }
    pendingOutbox.clear()
}
```

### Step 5: Call flushOutbox() at end of networkTickLoop

In `networkTickLoop`, after the C4 cleanup block (the last existing block before `delay()`), add:

```kotlin
// Flush per-tick packet outbox: combines buffered packets into fewer datagrams per host
flushOutbox()
```

### Step 6: Write failing test

Create `server/src/test/kotlin/ccr/server/PacketBatchingTest.kt`:

```kotlin
package ccr.server

import ccr.net.protocol.PacketCombiner
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PacketBatchingTest {

    private val addr = InetSocketAddress("127.0.0.1", 4848)

    @Test
    fun `three same-size packets to same host combine into one datagram`() {
        val pkt1 = ByteArray(20) { it.toByte() }
        val pkt2 = ByteArray(20) { (it + 1).toByte() }
        val pkt3 = ByteArray(20) { (it + 2).toByte() }

        val datagrams = PacketCombiner.combine(listOf(addr to pkt1, addr to pkt2, addr to pkt3))

        assertEquals(1, datagrams.size, "three small same-size packets should fit in one datagram")
        // Each datagram contains 2-byte header + 3 × 20 bytes = 62 bytes
        assertEquals(62, datagrams[0].data.size)
    }

    @Test
    fun `single packet still produces one datagram`() {
        val pkt = ByteArray(30) { it.toByte() }
        val datagrams = PacketCombiner.combine(listOf(addr to pkt))
        assertEquals(1, datagrams.size)
    }

    @Test
    fun `packets exceeding MTU split across multiple datagrams`() {
        // 540-byte MTU; 2-byte header per group; 31 packets × 17 bytes = 527 bytes + 2 = 529 fits
        // Add enough packets to overflow
        val large = (1..50).map { addr to ByteArray(20) { i -> i.toByte() } }
        val datagrams = PacketCombiner.combine(large)
        assertTrue(datagrams.size > 1, "50 × 20-byte packets should not all fit in one datagram")
        for (dg in datagrams) {
            assertTrue(dg.data.size <= 540, "each datagram must be ≤ MTU")
        }
    }
}
```

### Step 7: Run tests

```
kotlin-server/gradlew -p kotlin-server test
```

Expected: all tests pass including the 3 new ones.

### Step 8: Commit

```
git add kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/PacketBatchingTest.kt
git commit -m "feat: buffer per-tick packets in pendingOutbox, flush with PacketCombiner at end of tick"
```

---

## Task 2: Wire FlowController + BandwidthBudget

**Files:**
- Modify: `server/src/main/kotlin/ccr/server/GameServer.kt`
- Test: `net/src/test/kotlin/ccr/net/flow/FlowControllerTest.kt` (NEW — none exists yet)

### Step 1: Write failing tests for FlowController

Create `net/src/test/kotlin/ccr/net/flow/FlowControllerTest.kt`:

```kotlin
package ccr.net.flow

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlowControllerTest {

    @Test
    fun `shouldSend returns true initially (threshold starts at 100)`() {
        val fc = FlowController()
        assertTrue(fc.shouldSend(50.0f), "fresh controller should allow sends")
        assertTrue(fc.shouldSend(0.0f), "priority 0 passes when threshold is 100")
    }

    @Test
    fun `threshold increases toward 100 after adjust with low traffic`() {
        val fc = FlowController()
        // Start with a manipulated low threshold
        repeat(200) { fc.adjust(targetBps = 100_000, actualBpsSent = 10, sampleTimeMs = 50f) }
        // After many under-budget ticks, threshold should remain at or near 0 (or rise to 100)
        // The key invariant: shouldSend(0.0f) always returns true (0 >= 0)
        assertTrue(fc.shouldSend(0.0f))
    }

    @Test
    fun `shouldSend can return false when threshold is raised by repeated overruns`() {
        val fc = FlowController()
        // Simulate heavy overrun: actual >> target, many ticks
        // Threshold rises toward TP_MAX (100.0), eventually blocking priority-0 objects
        // In practice this is hard to trigger in a unit test since TP_INCREMENT=0.01 and
        // we start at 100.0 already. So test the inverse: if we manually observe that
        // threshold=100.0 means shouldSend(99.9f) returns true and shouldSend(100.1f) false
        // is NOT possible (doubles). Actually shouldSend is >= so at threshold=100.0 only
        // priority >= 100.0 passes. Test an object with priority exactly 50:
        assertTrue(fc.shouldSend(100.0f))  // equal to threshold
        // shouldSend(99.9f) also true since threshold starts at 100.0 → 99.9 < 100.0 → false!
        assertFalse(fc.shouldSend(99.9f), "priority below initial threshold 100.0 should NOT send")
    }

    @Test
    fun `adjust with overrun raises threshold and lowers multiplier`() {
        val fc = FlowController()
        // Force a massive overrun
        fc.adjust(targetBps = 1000, actualBpsSent = 100_000, sampleTimeMs = 50f)
        // Bandwidth multiplier should have been reduced
        assertTrue(fc.bandwidthMultiplier < 1.0f)
    }
}
```

### Step 2: Run tests to confirm new tests pass (FlowController logic already correct)

```
kotlin-server/gradlew -p kotlin-server :net:test
```

Expected: `FlowControllerTest` tests pass (the controller logic is already implemented).

### Step 3: Add bandwidth fields to GameServer

Insert near the `flowControllers` would naturally go — after `bandwidthBudget`:

```kotlin
// C++: cConnection bandwidth management — divide total BPS among connected hosts
private val bandwidthBudget = BandwidthBudget(if (config.bandwidthBps > 0) config.bandwidthBps else BandwidthBudget(0).totalBps)
// Per-host flow controllers (C++: Adjust_Flow_If_Necessary in rhost.cpp)
private val flowControllers = mutableMapOf<Int, FlowController>()
```

Simpler — just use BandwidthBudget's default when config is 0:
```kotlin
private val bandwidthBudget = BandwidthBudget(config.bandwidthBps.let { if (it > 0) it else 1_500_000 })
private val flowControllers = mutableMapOf<Int, FlowController>()
```

Add imports at top of GameServer.kt:
```kotlin
import ccr.net.flow.BandwidthBudget
import ccr.net.flow.FlowController
```

### Step 4: Create/destroy FlowController with player lifecycle

In the BIOEVENT handler section where a player joins `playerInGame` (look for where `god.playerInGame.add(rhostId)` or similar is called), add:
```kotlin
flowControllers[rhostId] = FlowController()
```

In the disconnect handler (where `god.removePlayer(rhostId)` is called), add:
```kotlin
flowControllers.remove(rhostId)
```

### Step 5: Call adjust() after flushOutbox in networkTickLoop

In `networkTickLoop`, after `flushOutbox()`, add:

```kotlin
// Adjust per-host flow controllers with bytes sent this tick
val connectedCount = connectionManager.getConnectedCount()
val targetBps = bandwidthBudget.perHostBps(connectedCount)
for ((rhostId, bytesSent) in bytesSentThisTick) {
    flowControllers[rhostId]?.adjust(targetBps, bytesSent, tickDeltaMs.toFloat())
}
bytesSentThisTick.clear()
```

### Step 6: Gate BIT_FREQUENT sends with shouldSend in replicationTick

In `replicationTick()`, the BIT_FREQUENT-only branch (lines ~460–465), change to:

```kotlin
} else if ((bits and 0x01) != 0) {
    // BIT_FREQUENT only — skip own soldier; gate others through FlowController
    if (!isOwnSoldier) {
        val fc = flowControllers[clientId]
        if (fc == null || fc.shouldSend(50.0f)) {
            sendUnreliable(host) { bs -> NetworkObjectPacketWriter.writeFrequentUpdate(bs, obj, obj.networkId) }
        }
    }
    obj.setObjectDirtyBits(clientId, 0)
}
```

Note: `50.0f` is the fixed priority for frequent updates (matches C++ where soldier position updates use priority 50). Objects with priority >= threshold are sent; with threshold starting at 100.0, ALL objects pass initially (50 < 100 → FALSE... wait).

**Important check:** `FlowController.shouldSend(priority)` returns `priority >= thresholdPriority`. Initial threshold is 100.0. So `shouldSend(50.0f)` returns `50.0 >= 100.0 = false`. That would block everything at start!

Looking at the C++ more carefully: `Adjust_Flow_If_Necessary` is only called when bandwidth is exceeded. Initially, with no calls to `adjust()`, the controller should allow all sends. The fix: start threshold at 0.0 (allow all), only raise it when needed.

Actually re-reading `FlowController.kt`: threshold starts at `INITIAL_THRESHOLD_PRIORITY = 100.0`. And `shouldSend(priority)` checks `priority >= thresholdPriority`. So `shouldSend(50.0f)` is `50.0 >= 100.0 = false` — blocks immediately!

This is a bug in the existing FlowController design. The initial state must allow all sends. Two options:
1. Change initial threshold to 0.0 (allow all, raise when overloaded)
2. Invert the logic: threshold is a minimum; start at 0 meaning "send everything"; raise to block lower-priority objects

Option 1 matches C++ semantics: threshold starts at 0 (no filtering), increases toward 100 when overloaded to block low-priority objects. The `TP_TOLERANCE_DOWN` / `TP_TOLERANCE_UP` values control when to raise/lower the threshold.

Fix `FlowController.kt`: change `INITIAL_THRESHOLD_PRIORITY = 100.0` → `INITIAL_THRESHOLD_PRIORITY = 0.0`.

This means `shouldSend(50.0f)` returns `50.0 >= 0.0 = true` by default. When overloaded, threshold rises toward 100.0, eventually blocking priority-50 objects.

### Step 7: Fix FlowController initial threshold

In `FlowController.kt`:
```kotlin
// BEFORE:
private const val INITIAL_THRESHOLD_PRIORITY = 100.0

// AFTER:
private const val INITIAL_THRESHOLD_PRIORITY = 0.0
```

Update `FlowControllerTest` — the test `shouldSend returns true initially` needs adjustment since now `shouldSend(50.0f)` returns true AND `shouldSend(99.9f)` also returns true (0.0 is threshold). Update the test accordingly:

```kotlin
@Test
fun `shouldSend returns true initially (threshold starts at 0)`() {
    val fc = FlowController()
    assertTrue(fc.shouldSend(50.0f), "fresh controller should allow sends")
    assertTrue(fc.shouldSend(0.0f), "priority 0 passes when threshold is 0")
    assertTrue(fc.shouldSend(99.9f), "priority 99.9 passes when threshold is 0")
}

@Test
fun `shouldSend returns false for negative priority when threshold is 0`() {
    val fc = FlowController()
    assertFalse(fc.shouldSend(-1.0f), "priority -1 does not pass threshold 0")
}

@Test
fun `threshold rises under sustained overrun and blocks low-priority objects`() {
    val fc = FlowController()
    // Simulate 600 ticks of heavy overrun (each tick raises threshold by TP_INCREMENT=0.01)
    repeat(600) { fc.adjust(targetBps = 1000, actualBpsSent = 5000, sampleTimeMs = 50f) }
    // Threshold should now be > 0 (has risen from 0.0), blocking priority-0 objects
    assertFalse(fc.shouldSend(0.0f), "after sustained overrun, low-priority objects should be blocked")
}

@Test
fun `adjust with overrun raises threshold and lowers multiplier`() {
    val fc = FlowController()
    fc.adjust(targetBps = 1000, actualBpsSent = 100_000, sampleTimeMs = 50f)
    assertTrue(fc.bandwidthMultiplier < 1.0f)
}
```

### Step 8: Run all tests

```
kotlin-server/gradlew -p kotlin-server test
```

Expected: all tests pass including the updated `FlowControllerTest`.

### Step 9: Commit

```
git add kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt \
        kotlin-server/net/src/main/kotlin/ccr/net/flow/FlowController.kt \
        kotlin-server/net/src/test/kotlin/ccr/net/flow/FlowControllerTest.kt
git commit -m "feat: wire FlowController and BandwidthBudget — gate BIT_FREQUENT sends, adjust per tick"
```

---

## Task 3: Packet loss tracking

**Files:**
- Modify: `net/src/main/kotlin/ccr/net/reliability/ReliableChannel.kt`
- Modify: `net/src/main/kotlin/ccr/net/connection/ConnectionManager.kt`
- Test: add cases to `net/src/test/kotlin/ccr/net/reliability/ReliableChannelTest.kt`

### Step 1: Write failing tests in ReliableChannelTest

Open `net/src/test/kotlin/ccr/net/reliability/ReliableChannelTest.kt` and add:

```kotlin
@Test
fun `computeAndResetLoss returns 0 when no packets sent`() {
    val ch = ReliableChannel()
    assertEquals(0f, ch.computeAndResetLoss())
}

@Test
fun `computeAndResetLoss returns 0 when all packets acked`() {
    val ch = ReliableChannel()
    val p = Packet()
    ch.enqueue(p, ByteArray(4))
    ch.ack(p.id)
    assertEquals(0f, ch.computeAndResetLoss())
}

@Test
fun `computeAndResetLoss returns resend fraction`() {
    val ch = ReliableChannel()
    val p1 = Packet(); ch.enqueue(p1, ByteArray(4))
    val p2 = Packet(); ch.enqueue(p2, ByteArray(4))
    val p3 = Packet(); ch.enqueue(p3, ByteArray(4))
    val p4 = Packet(); ch.enqueue(p4, ByteArray(4))
    // Resend p1 once
    ch.markResent(p1.id, System.currentTimeMillis())
    // Loss = 1 resend / 4 sent = 0.25
    assertEquals(0.25f, ch.computeAndResetLoss())
}

@Test
fun `computeAndResetLoss resets counters after call`() {
    val ch = ReliableChannel()
    val p = Packet(); ch.enqueue(p, ByteArray(4))
    ch.markResent(p.id, System.currentTimeMillis())
    ch.computeAndResetLoss()  // first call
    // Second call should return 0 (counters reset)
    assertEquals(0f, ch.computeAndResetLoss())
}
```

### Step 2: Run tests to confirm they fail

```
kotlin-server/gradlew -p kotlin-server :net:test
```

Expected: new tests fail with "unresolved reference: computeAndResetLoss".

### Step 3: Add counters and computeAndResetLoss to ReliableChannel

In `ReliableChannel.kt`:

1. Add fields after `nextReceiveId`:
```kotlin
// C++: packet loss stats for keepalive reporting
var sentSinceReset: Int = 0
    private set
var resentSinceReset: Int = 0
    private set
```

2. In `enqueue()`, after `sendQueue[id] = ...`:
```kotlin
sentSinceReset++
```

3. In `markResent()`, after updating `sendQueue[packetId]`:
```kotlin
resentSinceReset++
```

4. Add new function after `drainDeliverable()`:
```kotlin
// C++: packet loss percentage for keepalive — ratio of resends to total sends
// Resets counters so each keepalive interval reflects only that interval.
fun computeAndResetLoss(): Float {
    val loss = if (sentSinceReset > 0) resentSinceReset.toFloat() / sentSinceReset else 0f
    sentSinceReset = 0
    resentSinceReset = 0
    return loss.coerceIn(0f, 1f)
}
```

### Step 4: Run tests to confirm they pass

```
kotlin-server/gradlew -p kotlin-server :net:test
```

Expected: all `ReliableChannelTest` cases pass.

### Step 5: Update packetLossPc in ConnectionManager.getKeepalives()

In `ConnectionManager.getKeepalives()`, before building the keepalive packet, add:

```kotlin
// Update packet loss from the reliable channel's per-interval stats
host.packetLossPc = host.reliable.computeAndResetLoss()
```

The line:
```kotlin
p.payload.addFloat(host.packetLossPc)
```
already sends it to the client — no other changes needed.

The full diff to `getKeepalives()` in the host loop:
```kotlin
// BEFORE:
host.lastServiceCount = serviceCount
host.markKeepaliveSent(nowMs)
val packetId = host.reliable.nextSendId
val p = serverPacket(PacketType.KEEPALIVE, packetId)
p.payload.addFloat(host.packetLossPc)

// AFTER:
host.lastServiceCount = serviceCount
host.markKeepaliveSent(nowMs)
host.packetLossPc = host.reliable.computeAndResetLoss()  // ← new line
val packetId = host.reliable.nextSendId
val p = serverPacket(PacketType.KEEPALIVE, packetId)
p.payload.addFloat(host.packetLossPc)
```

### Step 6: Run all tests

```
kotlin-server/gradlew -p kotlin-server test
```

Expected: all 17+ tests pass.

### Step 7: Commit

```
git add kotlin-server/net/src/main/kotlin/ccr/net/reliability/ReliableChannel.kt \
        kotlin-server/net/src/main/kotlin/ccr/net/connection/ConnectionManager.kt \
        kotlin-server/net/src/test/kotlin/ccr/net/reliability/ReliableChannelTest.kt
git commit -m "feat: track reliable send/resend counts, populate packetLossPc in keepalive"
```

---

## Verification

After all 3 tasks:

```
kotlin-server/gradlew -p kotlin-server test
```

All tests pass. Manual verification:
- Server starts, clients connect — packets arrive normally (no throttle on localhost)
- With `BandwidthBps=28800` (28.8K modem) in config — frequent updates should be gated after sustained overrun
- Keepalive packets carry nonzero `packetLossPc` when resends occur
- RCON status shows connected players without regression

## Later Phases

| Phase | Name | Key Deliverables |
|-------|------|-----------------|
| 11 | Physics Integration | Full collision, projectile flight, vehicle physics |
