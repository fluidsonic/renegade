# Implementation Plan: C++ Feature Parity for Kotlin Server

See the full plan in the conversation history (2026-02-22 session).

## Branch: `feature/cpp-parity`
## Worktree: `.worktrees/cpp-parity`

## Steps (in execution order)

1. **Step 2** — GameContext container
2. **Step 1** — GameObjManager (Think loop infrastructure)
3. **Step 16** — frameDeltaSeconds on GameContext
4. **Step 4** — DamageableGameObj damage system
5. **Step 3** — BaseControllerClass mutable + setters
6. **Step 5** — Building state transitions, cncInitialize, onDestroyed
7. **Step 6** — Building init sequence (call cncInitialize in GameServer)
8. **Step 13** — Player-soldier binding (playerData on SoldierGameObj)
9. **Step 9** — Game loop reorder
10. **Step 7** — CHANGETEAMEVENT fix (delete soldier on team change)
11. **Step 8** — Core restart building reset
12. **Step 11** — GameState timer precision (Int → Float)
13. **Step 10** — ServerFps dynamic update
14. **Step 12** — Spawn facing / Transform
15. **Step 14** — Static network objects (BackgroundMgr, WeatherMgr registration)
16. **Step 15** — Reconnect fix (delete old objects if active)
17. **Step 17** — Purchase system (initial)
