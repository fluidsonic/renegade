# C++ vs Kotlin Server: Init & Game Loop Discrepancy Analysis

## Context
Side-by-side comparison of the C++ Renegade server (original/Code/) and Kotlin server (kotlin-server/) initialization sequences and main game loops. The goal is to identify all missing or divergent functionality to prioritize next work items.

---

## Category 1: Game Timer & Time Management

### 1.1 No game timer countdown
- **C++**: `cGameData::Think()` decrements `TimeRemainingSeconds` by frame delta every tick. `GameDurationS` is tracked. `FrameCount` increments.
- **Kotlin**: `sendGameDataUpdateEvent()` sends a static `timeRemaining = config.timeLimitMinutes * 60`. No countdown logic. No `GameDurationS` or `FrameCount` tracking.
- **Impact**: Clients see a frozen timer. Time-based game-over never triggers.

### 1.2 No game start time tracking
- **C++**: `On_Game_Begin()` records `GameStartTime` (SYSTEMTIME) and `GameStartTimeMs`. Used for MVP qualifying time, ladder points.
- **Kotlin**: No equivalent. No join-time or total-time tracking per player.

---

## Category 2: Game Over / Win Conditions

### 2.1 No game-over detection
- **C++**: `End_Game_Test()` runs every server tick in `Server_Think()`. Checks `Is_Game_Over()` which evaluates: time limit expired, manual restart, base destruction (CnC), beacon placement (CnC).
- **Kotlin**: No game-over check anywhere. Game runs indefinitely.

### 2.2 No intermission system
- **C++**: When game ends, `Game_Over_Processing()` → `Begin_Intermission()` sets `IsIntermission=true`, starts 15-second countdown. During intermission: gameplay stops, God::Think skips spawning, scoring blocked. When timer expires: `Intermission_Over_Processing()` deletes all players, triggers map reload.
- **Kotlin**: No intermission concept. No `cWinEvent` sent. No win screen.

### 2.3 No map rotation
- **C++**: `cGameData::Rotate_Map()` cycles through `MapCycle[MAX_MAPS=100]` using `MapCycleIndex`. `Core_Restart()` unloads and reloads the next map.
- **Kotlin**: Single map loaded at startup. No map cycle, no reload.

---

## Category 3: Game Mode / Game Data

### 3.1 No cGameData Think() equivalent
- **C++**: `Shared_Client_And_Server_Think()` calls `The_Game()->Think()` every tick (timer countdown, current players update, frame count).
- **Kotlin**: No per-tick game data update. `currentPlayers` only updated on connection in `sendConnectionObjects()`.

### 3.2 No `On_Game_Begin()` logic
- **C++**: Configures combat layer flags (friendly fire, building repair, driver-is-gunner, beacon placement), filters spawners based on `SpawnWeapons`, performs team remix/swap/rebalance.
- **Kotlin**: None of these. No spawner filtering. No team remix/swap/rebalance at game start.

### 3.3 No `cGameDataCnc` specifics
- **C++**: CnC mode has `BaseControllerClass` (BaseGDI, BaseNOD) that think every tick, tracking building status and power. `StartingCredits` assigned to soldiers via `Soldier_Added()`. `Is_Gameplay_Permitted()` requires both teams to have >= 1 player.
- **Kotlin**: No base controllers. No starting credits. No gameplay-permitted check.

### 3.4 No `GameDataUpdateEvent` timer updates
- **C++**: `cGameDataUpdateEvent` is re-sent when time changes (e.g., after `Reset_Time_Remaining_Seconds()`).
- **Kotlin**: `GameDataUpdateEvent` sent once at join. Never re-sent for timer updates.

---

## Category 4: Player Management

### 4.1 No player reconnect handling
- **C++**: `cGod::Create_Player()` looks for existing active player by name (crash/rejoin → deletes old), then looks for inactive player by name (reconnect → reactivates with preserved stats).
- **Kotlin**: `createPlayer()` always creates a new Player. No reconnect detection. No inactive player pool.

### 4.2 No player IsActive / deactivation
- **C++**: Players have `IsActive` flag. On disconnect, players are set `IsActive=false` but not immediately deleted. `Remove_Inactive()` cleans them up on game reset.
- **Kotlin**: Players are fully deleted on disconnect (`removePlayer()` removes from all maps + unregisters).

### 4.3 No player scoring
- **C++**: `cPlayer::Increment_Score()/Increment_Kills()/Increment_Deaths()/Increment_Money()` track all stats, propagate score to team, set `BIT_OCCASIONAL` dirty for replication.
- **Kotlin**: `Player` has `kills`, `deaths`, `score`, `money` fields but they always export 0. No increment logic. No team score propagation.

### 4.4 No kill/death event handling
- **C++**: `cPlayerKill` network event broadcasts killer/victim IDs. Triggers score increments, death increments, kill messages.
- **Kotlin**: No `PlayerKill` event. No damage system to trigger kills.

### 4.5 No player sort / ranking
- **C++**: `cPlayerManager::Sort_Players()` sorts by LadderPoints > Score > Kills > Deaths > PlayerType > Name. Assigns rungs (ranks). Used for MVP, ladder points, scoreboard.
- **Kotlin**: No sorting. No ranking.

### 4.6 No MVP / ladder points
- **C++**: `Determine_Mvp_Name()` finds top scorer with minimum qualifying time. `Compute_Ladder_Points()` awards position-based points post-game.
- **Kotlin**: None.

---

## Category 5: Team Management

### 5.1 No team scoring
- **C++**: `cTeam::Increment_Score()`, `Increment_Kills()`, `Increment_Deaths()` track team stats. Score flows from player increments. Sets BIT_OCCASIONAL/BIT_RARE dirty.
- **Kotlin**: `Team` has `kills`, `deaths`, `score` fields that always export 0. No increment logic.

### 5.2 No team sort / leader detection
- **C++**: `cTeamManager::Sort_Teams()` sorts by score > kills. `Get_Leaders_Id()` returns winning team.
- **Kotlin**: No sorting. No leader detection.

### 5.3 No team balance enforcement
- **C++**: `Rebalance_Team_Sides()` (moves players if `|nod - gdi| > 1`), `Remix_Team_Sides()` (random shuffle), `Swap_Team_Sides()` (50% chance swap) at game start.
- **Kotlin**: `choosePlayerType()` does auto-balance on join only. No rebalancing mid-game or at game start.

### 5.4 No `IsTeamChangingAllowed` enforcement
- **C++**: `cGameDataCnc` sets `IsTeamChangingAllowed = false` by default. `CHANGETEAMEVENT` is supposed to check this.
- **Kotlin**: Team change always allowed. No server-side validation.

---

## Category 6: Server Think / Tick Structure

### 6.1 Missing `Hibernation_Think()`
- **C++**: `Shared_Client_And_Server_Think()` calls `Hibernation_Think()` which resets hibernation on all PhysicalGameObj to keep updates flowing.
- **Kotlin**: No hibernation system. (Currently irrelevant since no physics, but will matter when physics is wired in.)

### 6.2 Missing `SpawnManager::Update()`
- **C++**: `CombatManager::Think()` calls `SpawnManager::Update()` every tick to manage spawner timers (item/weapon/vehicle respawn timers).
- **Kotlin**: SpawnManager only provides `getMultiplayerSpawnLocation()`. No timer-based respawn of weapons/vehicles/items.

### 6.3 Missing `GameObjManager::Think()` / `Post_Think()`
- **C++**: Every tick iterates ALL game objects calling `Think()` and then `Post_Think()` (post-physics updates).
- **Kotlin**: No game object think loop. Objects are static after creation.

### 6.4 Missing `BulletManager::Update()`
- **C++**: Updates all active bullet projectiles every tick.
- **Kotlin**: No bullet system.

### 6.5 Missing bandwidth-aware replication
- **C++**: `Tell_Client_About_Dynamic_Objects()` uses per-client bandwidth budgets, distance-based priority (0-1 scale mapped to 140ms-5000ms update intervals), PVS visibility filtering, client FPS throttling, and client hints.
- **Kotlin**: `replicationTick()` sends ALL dirty objects to ALL clients every tick. No prioritization, no bandwidth management, no distance filtering.

### 6.6 Missing delete-pending broadcast
- **C++**: `Server_Send_Delete_Notifications()` runs every tick and broadcasts pending deletions to all clients.
- **Kotlin**: Deletions are sent explicitly in `deleteSoldier()` / `removePlayer()`. No centralized delete-pending system via `NetworkObjectManager`.

### 6.7 Missing `cNetwork::End_Game_Test()` per-tick sort
- **C++**: Sorts teams and players once per second during gameplay.
- **Kotlin**: No periodic sorting.

---

## Category 7: Network Events Not Handled

### 7.1 Missing `DONATEVENT` (classId 1019)
- **C++**: Player donates money to another player.
- **Kotlin**: Not handled.

### 7.2 Missing `EVICTIONEVENT` (classId 1023)
- **C++**: Server kicks a player.
- **Kotlin**: Not handled.

### 7.3 Missing `PURCHASEREQUESTEEVENT` (classId 1024)
- **C++**: Player purchases a character/vehicle/item.
- **Kotlin**: Not handled. No purchase system.

### 7.4 Missing `ANNOUNCEEVENT` / chat events
- **C++**: Chat messages, radio commands, announcements.
- **Kotlin**: Not handled.

### 7.5 Missing `LOADINGEVENT` processing
- **C++**: `cLoadingEvent::Act()` sets player loading state, used to defer replication.
- **Kotlin**: Reads but ignores the event.

### 7.6 Missing `CLIENTFPS` processing
- **C++**: `cClientFps::Import_Creation` stores client FPS. Used by bandwidth-aware replication to throttle send rate.
- **Kotlin**: Reads but discards.

### 7.7 Missing `GAMEOPTIONSEVENT` re-send
- **C++**: `cGameOptionsEvent` is re-sent when game settings change.
- **Kotlin**: Sent once at connection. Never re-sent.

---

## Category 8: Combat & Physics

### 8.1 No damage system
- **C++**: `ArmorWarheadManager`, weapon fire, bullet hit detection, damage application, health/shield modification.
- **Kotlin**: `DamageableGameObj` exports static health/shield values. No damage processing.

### 8.2 No physics integration
- **C++**: `COMBAT_SCENE->Update(dt)` runs PhysicsSceneClass every tick. Sub-steps at MAX_TIMESTEP=1/15s. Collision detection, gravity, movement.
- **Kotlin**: `physicsTickLoop()` is an empty stub. The `ccr.physics` Gradle module exists with collision math, ODE integrators, and spatial structures but is not wired into the game loop.

### 8.3 No weapon system
- **C++**: Weapon firing, ammo tracking, reload, weapon switching all managed per-soldier.
- **Kotlin**: Soldiers spawn with a pistol (definition only). No firing/ammo/reload logic.

---

## Category 9: Buildings & Base System

### 9.1 No building spawning
- **C++**: Buildings are loaded from LDD and created as `BuildingGameObj` instances. They have health, power status, and are replicated.
- **Kotlin**: Building definitions are parsed. Building network object classes exist (`BuildingGameObj`, `PowerPlantGameObj`, etc.) with export code. But buildings are never instantiated from LDD data or registered as network objects.

### 9.2 No base controllers
- **C++**: `BaseControllerClass` (BaseGDI/BaseNOD) manage base health, power status, building destruction tracking. Think every tick.
- **Kotlin**: No base controller concept.

---

## Category 10: Object Lifecycle

### 10.1 No vehicle spawning
- **C++**: Vehicles spawn from spawners, with physics and driver/gunner mechanics.
- **Kotlin**: Vehicle definitions parsed. `VehicleGameObj` export code exists. No vehicle instantiation.

### 10.2 No `Soldier_Added()` callback
- **C++**: `cGameDataCnc::Soldier_Added()` gives free weapons if `IsFreeWeapons`, gives starting credits if game time == 0.
- **Kotlin**: Soldiers get a pistol at creation. No starting credits. No free weapons option.

### 10.3 No game object creation from LDD
- **C++**: `GameObjManager` instantiates all game objects from LDD data (buildings, vehicles, static objects, etc.).
- **Kotlin**: LDD is parsed but game objects (other than spawners) are not instantiated.

---

## Category 11: Connection Flow Differences

### 11.1 No `cNetwork::Connection_Handler()` team/options re-send
- **C++**: On new client connect, sends team updates AND game options. If game is in progress, also sends existing state.
- **Kotlin**: Similar — sends Teams + GameOptionsEvent. But no game-in-progress state sync beyond what BIOEVENT handler does.

### 11.2 No server FPS tracking
- **C++**: `cServerFps` singleton tracks and reports server FPS to clients.
- **Kotlin**: No server FPS reporting. (ServerFps classId=1010 — clients may expect this.)

### 11.3 No bandwidth negotiation
- **C++**: Bandwidth check system (`cBandwidthCheck`), per-client BPS targets, bandwidth graph.
- **Kotlin**: No bandwidth management. All clients get all updates.

---

## Priority Summary (by gameplay impact)

### High Priority (breaks core gameplay loop)
| # | Discrepancy | Why |
|---|---|---|
| 1.1 | No game timer countdown | Clients see frozen timer |
| 2.1 | No game-over detection | Games never end |
| 2.2 | No intermission system | No win/loss cycle |
| 2.3 | No map rotation | Server stuck on one map |
| 4.3 | No player scoring | Scoreboard always shows 0 |
| 5.1 | No team scoring | Team scores always 0 |
| 9.1 | No building spawning | No bases = no CnC gameplay |

### Medium Priority (gameplay quality)
| # | Discrepancy | Why |
|---|---|---|
| 3.2 | No On_Game_Begin logic | No spawner filtering, no team rebalance |
| 3.3 | No base controllers | Buildings don't track power/status |
| 4.1 | No player reconnect | Crash = lose all stats |
| 4.4 | No kill/death events | No kill feed |
| 5.4 | No team change enforcement | Teams can become unbalanced |
| 6.2 | No SpawnManager timers | Weapons/vehicles don't respawn |
| 6.5 | No bandwidth-aware replication | Scales poorly with players |
| 7.3 | No purchase system | Can't buy characters/vehicles |
| 8.1 | No damage system | Can't kill anything |
| 10.2 | No Soldier_Added callback | No starting credits |

### Lower Priority (polish / scale)
| # | Discrepancy | Why |
|---|---|---|
| 1.2 | No game start time tracking | MVP/ladder needs it |
| 3.4 | No GameDataUpdateEvent re-send | Timer desync |
| 4.2 | No player deactivation pool | Reconnect support |
| 4.5 | No player sort/ranking | Scoreboard ordering |
| 4.6 | No MVP/ladder | Post-game stats |
| 6.1 | No hibernation think | Physics prep |
| 6.3 | No game object think loop | Object behavior |
| 6.6 | No centralized delete-pending | Edge cases |
| 7.1-7.7 | Various unhandled events | Feature completeness |
| 8.2 | No physics integration | Movement validation |
| 11.2 | No server FPS reporting | Client diagnostics |
