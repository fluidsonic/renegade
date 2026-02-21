# Plan: Remove WOL & GameSpy Compat Files and Code

## Context

The WOL (Westwood Online) and GameSpy online services are defunct. The codebase had their implementations replaced with no-op stub headers in `original/compat/`. This plan fully removes all WOL and GameSpy traces while preserving generic internet connectivity features (`+Connect`, port config, NIC selection, nicknames, bandwidth).

## Scope

- **Remove:** All WOL and GameSpy compat stub files, stub headers, dead code branches, service-specific logic
- **Preserve:** Generic internet game features currently routed through GameSpy APIs (`+Connect <ip>:<port>`, `+NETPLAYERNAME`, `+PASSWORD`, game port, NIC binding, internet bandwidth, nicknames)
- **NAT.cpp:** Delete entirely (WOL-specific NAT traversal)
- **Player packets:** Keep wire format (serialize zeros), add comments explaining removed fields

---

## Phase 1: Delete Compat Stub Files

### WOL files to delete (24 files)
- `original/compat/wolapi.h`
- `original/compat/natter.h`
- `original/compat/commando/natter.h`
- `original/compat/WWOnline/` — entire directory (21 files)

### GameSpy files to delete (9 files)
- `original/compat/GameSpy/` — entire directory (6 files: `gcdkeyclient.h`, `gcdkeyserver.h`, `gqueryreporting.h`, `gs_md5.h`, `gs_patch_usage.h`, `nonport.h`)
- `original/compat/GameSpy_QnR.h`
- `original/compat/gamespyadmin.h`
- `original/compat/gamespyauthmgr.h`
- `original/compat/gamespybanlist.h`

### WOL stubs in Code/Commando/ to delete (17 files)
- `wolgmode.h`, `WOLGameInfo.h`, `WOLLogonMgr.h`, `woldiags.h`, `wolloginprofile.h`
- `DlgWOLWait.h`, `dlgmpwolmain.h`, `dlgmpwolbuddies.h`, `dlgmpwolpagebuddy.h`
- `dlgmpwolchat.h`, `dlgmpwolgamelist.h`, `dlgmpwolquickmatchoptions.h`, `dlgwolsettings.h`
- `gamechannel.h`, `gamechannel.cpp`, `gamechanlist.h`, `gamechanlist.cpp`

### NAT files to delete (2 files)
- `Code/Commando/NAT.cpp`
- `Code/Commando/nat.h`

**Total: 52 files deleted**

---

## Phase 2: Remove WOL Infrastructure & Includes

### Remove all WOL `#include` statements (~45 files)

Remove lines matching `#include.*wol`, `#include.*WWOnline`, `#include.*natter`, `#include.*wolapi` from all files listed in the exploration (combatgmode.cpp, console.cpp, init.cpp, gamedata.cpp, consolefunction.cpp, dlgmplanhostoptions.cpp, renegadedialogmgr.cpp, DlgMPTeamSelect.cpp/.h, mpsettingsmgr.cpp, GameResSend.cpp, bandwidthcheck.cpp/.h, FirewallWait.cpp/.h, ServerSettings.cpp/.h, dialogtests.cpp/.h, DlgDownload.h, dlgmainmenu.cpp, dlgmultiplayoptions.cpp, DlgMPConnect.cpp, dlgmpingamechat.cpp, messages.cpp, cnetwork.cpp, god.cpp, dlgcncbattleinfo.cpp, dlgcncteaminfo.cpp, dlgcncserverinfo.cpp, dlgcncwinscreen.cpp, dlgevaencyclopedia.cpp, dlghelpscreen.cpp, dlgmplangamelist.cpp, diagnostics.cpp, netgraphs.cpp, gamemenu.cpp, gameinitmgr.cpp, slavemaster.cpp, commando_stubs.cpp, dlgmplanhostoptions.h, nat.h [deleted], multihud.cpp, player.cpp, playermanager.cpp, dlgcncreference.cpp).

### Remove `MODE_WOL` enum and WOL init/shutdown

**`gameinitmgr.h`:** Remove `Initialize_WOL()`, `Shutdown_WOL()`, `Is_WOL_Initialized()`, `Set_WOL_Return_Dialog()`, `MODE_WOL` enum value, `WOLReturnDialog` member.

**`gameinitmgr.cpp`:** Remove `WolGameModeClass` registration (`GameModeManager::Add(new WolGameModeClass)`), `Initialize_WOL()`/`Shutdown_WOL()` functions, all `MODE_WOL` switch cases and WOL blocks.

**`ServerSettings.h`:** Remove `MODE_WOL` from `GameModeTypeEnum`. Remove `WWOnline::IRCServerList` params from `Get_Preferred_Server()`/`Write_Server_List()`.

**`ServerSettings.cpp`:** Remove WOL game type parsing, WOL registry settings, `Get_Preferred_Server()`/`Write_Server_List()` functions.

### Remove WOL NAT global

**`commando_stubs.cpp`:** Remove `WOLNATInterfaceClass WOLNATInterface;` global and `#include "natter.h"`.

---

## Phase 3: Remove GameSpy Infrastructure

### Remove GameSpy `#include` statements (~30 files)

Remove `#include "gamespyadmin.h"`, `#include "GameSpy_QnR.h"`, `#include "gamespybanlist.h"`, `#include "gamespyauthmgr.h"` from: combatgmode.cpp, dlgcncreference.cpp, lanchat.cpp, consolefunction.cpp, DlgMPConnect.cpp, bioevent.cpp, gamedata.cpp, netinterface.cpp, cnetwork.cpp, DlgMPConnectionRefused.cpp, mainloop.cpp, player.cpp/.h, playermanager.cpp, bandwidthcheck.cpp, ServerSettings.cpp, dlgcncwinscreen.cpp, dlgmainmenu.cpp, team.cpp, dialogtests.cpp, dlgmplanhostoptions.cpp, gameinitmgr.cpp, svrgoodbyeevent.cpp, teammanager.cpp, init.cpp, useroptions.cpp, nicenum.cpp, commando_stubs.cpp, slavemaster.cpp.

### Remove `MODE_GAMESPY` enum — merge into `MODE_LAN`

**`ServerSettings.h`:** Remove `MODE_GAMESPY` from enum. After WOL+GameSpy removal, enum becomes: `MODE_NONE, MODE_LAN`.

**`ServerSettings.cpp`:** Remove GameSpy game type parsing. Default/only mode becomes LAN. Remove `cGameSpyAdmin::Set_Is_Server_Gamespy_Listed()` and `GameSpyQnR.Enable_Reporting()` calls.

### Remove GameSpy globals

**`commando_stubs.cpp`:** Remove `GameSpyBanListClass GameSpyBanList;` and `cGameSpyQnR GameSpyQnR;`.

### Remove GameSpy dialog classes

**`dialogtests.h` / `dialogtests.cpp`:** Remove `GameSpyMainDialogClass` and `GameSpyOptionsDialogClass` and all their methods. Remove `GameModeManager::Find("GameSpy")` registrations.

### Remove GameSpy-specific code from source files

In each file, remove `cGameSpyAdmin::*` calls, `GameSpyQnR.*` calls, `GameSpyBanList.*` calls, and related conditional blocks. Key files:

- **`mainloop.cpp`:** Remove `GameSpyQnR.Think()` and `cGameSpyAdmin::Think()` calls.
- **`consolefunction.cpp`:** Remove GameSpy kick/ban command implementations.
- **`init.cpp`:** Remove GameSpy mode initialization.
- **`gameinitmgr.cpp`:** Remove GameSpy menu routing, GameSpy port config.
- **`combatgmode.cpp`:** Remove `Get_Is_Launched_From_Gamespy()` exit-to-Arcade blocks.
- **`DlgMPConnect.cpp`:** Remove GameSpy exit-on-cancel block.
- **`DlgMPConnectionRefused.cpp`:** Remove GameSpy exit behavior.
- **`svrgoodbyeevent.cpp`:** Remove GameSpy exit behavior.
- **`dlgcncwinscreen.cpp`:** Remove GameSpy exit behavior.
- **`dlgcncreference.cpp`:** Remove GameSpy exit behavior.
- **`lanchat.cpp`:** Remove GameSpy broadcast-disable blocks.
- **`netinterface.cpp`:** Remove GameSpy nickname length logic (simplify to single nickname).
- **`bioevent.cpp`:** Remove GameSpy nickname collision handling.
- **`cnetwork.cpp`:** Remove GameSpy password/bandwidth overrides.

### Preserve generic features — inline from `cGameSpyAdmin`

The following `cGameSpyAdmin` state is used for generic internet connectivity and needs to be preserved in a simpler form (e.g., statics in `useroptions` or local variables):

- **`+Connect <ip>:<port>` parsing** in `useroptions.cpp:186-209` — keep the parsing, store IP/port directly (e.g., in `cUserOptions` static fields or a simple struct)
- **`+NETPLAYERNAME`** in `useroptions.cpp:211-250` — keep, store in existing nickname field
- **`+PASSWORD`** in `useroptions.cpp:252-290` — keep, store as simple static
- **Game port** (`GameSpyGamePort`, default 4848) — keep as `cUserOptions` field, rename to `InternetGamePort` or similar
- **NIC selection** (`PreferredGameSpyNic`) — keep, rename
- **Bandwidth type** (`GameSpyBandwidthType`) — keep, rename
- **Nickname** (`GameSpyNickname`) — keep, rename

### Remove GameSpy network event classes

**`Combat/netclassids.h`:** Remove `NETCLASSID_GAMESPYSCCHALLENGEEVENT`, `NETCLASSID_GAMESPYCSCHALLENGERESPONSEEVENT`.

**`Combat/apppackettypes.h`:** Remove `APPPACKETTYPE_GAMESPYSCCHALLENGEEVENT`, `APPPACKETTYPE_GAMESPYCSCHALLENGERESPONSEEVENT`.

### Remove GameSpy player fields

**`player.h`:** Remove `GameSpyAuthState`, `GameSpyAuthStateEntryTimeMs`, `GameSpyChallengeString`, `GameSpyHashId`, `GameSpyKickState`, `GameSpyKickStateEntryTimeMs` and their accessors. Remove `#include "gamespyauthmgr.h"`, `#include "gamespybanlist.h"`.

**`player.cpp`:** Remove GameSpy field initialization and methods.

---

## Phase 4: Player Packet Serialization (Wire-Compatible)

**`player.h` / `player.cpp`:** Remove `WolRank`, `WOLPoints`, `NumWolGames`, `ClanID` member variables and their getter/setter methods.

**`player.cpp` `Export_Rare` / `Import_Rare`:** Replace field serialization with zero-writes and dummy reads, with comments:

```cpp
// Export_Rare:
packet.Add((int)0);    // was: WolRank (removed — WOL service defunct)
packet.Add((int)0);    // was: NumWolGames (removed — WOL service defunct)

// Import_Rare:
(void)packet.Get(int_placeholder);  // was: WolRank (removed — WOL service defunct)
(void)packet.Get(int_placeholder);  // was: NumWolGames (removed — WOL service defunct)
```

**`playermanager.h` / `playermanager.cpp`:** Remove `Get_Average_WOL_Points()`. Remove WOL rank/clan references in `Construct_Heading()` and display methods.

---

## Phase 5: Remove Resources and String IDs

### `resource.h`
Remove all `IDD_MP_WOL_*`, `IDC_MP_WOL_*`, `IDC_WOLAPI_VER` defines (~40 IDs).
Remove `IDD_MENU_GAMESPY_MAIN`, `IDD_MENU_GAMESPY_OPTIONS`, `IDC_MENU_GAMESPY_*`, `IDC_GAMESPY_ICON` defines.

### `chat.rc`
Remove all WOL dialog definitions (26+ dialogs).
Remove GameSpy dialog definitions.

### `Scripts/string_ids.h` and `Combat/string_ids.h`
Remove all `IDS_WOL_*` and `IDS_MP_WOL_*` defines (~130 each).

---

## Phase 6: Clean Up Build Files and Globals

### `Code/wwnet/CMakeLists.txt`
Remove `"${ORIG_SRC}/wolapi"` and `"${ORIG_SRC}/WWOnline"` include directories.

### `Code/Commando/commando.dsp`
Remove all WOL/GameSpy/WWOnline/wolapi source file references and include paths.

### `Code/Commando/_globals.h`
Remove WOL registry key defines: `APPLICATION_SUB_KEY_NAME_WOLSETTINGS`, `APPLICATION_SUB_KEY_NAME_URL`, `APPLICATION_SUB_KEY_NAME_LOGINS`, `APPLICATION_SUB_KEY_NAME_QUICKMATCH`, `APPLICATION_SUB_KEY_NAME_IGNORE_LIST`, `APPLICATION_SUB_KEY_NAME_SERVER_LIST`.

### `Code/Launcher/main.cpp`
Remove `APPLICATION_SUB_KEY_NAME_WOLSETTINGS` define and usage.

### `Code/Launcher/Launcher.lcf`
Remove `SOFTWARE\Westwood\WOLAPI` line.

### `Code/Launcher/winblows.cpp`
Remove `GAMESPYSERVER=` argument handling (or rename to generic server config).

### `Code/commando.dsw`
Remove `GameSpy` project reference.

---

## Verification

1. After each phase, attempt a CMake configure + build to catch compilation errors
2. Search for remaining references: `grep -ri "wol\|gamespy\|wwonline\|natter\|wolapi" original/` — should return zero hits (except comments explaining removed fields)
3. Verify `+Connect` CLI argument still has a code path (preserved in Phase 3)
4. Verify player packet Export_Rare/Import_Rare still serializes the correct number of fields (zeros + comments)

---

## Commit Strategy

1. **Commit 1 (Phases 1-2):** "Remove WOL compat stubs and infrastructure"
2. **Commit 2 (Phase 3):** "Remove GameSpy compat stubs and infrastructure, preserve generic internet features"
3. **Commit 3 (Phase 4):** "Remove WOL player data, preserve wire format with zero padding"
4. **Commit 4 (Phases 5-6):** "Remove WOL/GameSpy resource IDs, string IDs, and build file references"
