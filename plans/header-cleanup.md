# Plan: Header Cleanup — pragma once, global.h first, delete dead headers

## Context

The C++ port under `original/` has accumulated inconsistent header hygiene:
- 884 headers use `#ifndef`/`#define` include guards, 64 use both guards + `#pragma once`, only 11 use `#pragma once` alone
- 8 headers are single-include forwarders that add indirection with no value
- 59 headers are never included by any file — dead code
- `global.h` is the project preamble but ~700+ files include it only transitively via `always.h` or other headers

This plan standardizes all headers to `#pragma once`, ensures `global.h` is explicitly first everywhere, removes forwarder indirection, and deletes unused headers.

## Work is done in worktree at `.worktrees/header-cleanup` (branch `header-cleanup`)

---

## Phase 1: Delete 59 unused header files

Delete these files outright — none are `#include`d by any source file:

**Code/Commando/** (4): `commandoids.h`, `DlgQuickmatch.h`, `lev_file.h`, `nettgas.h`

**Code/Launcher/** (4): `Toolkit/Support/RefPtr.h`, `Util/mboxd.h`, `Util/stderrd.h`, `Util/stdoutd.h`

**Code/Scripts/** (7): `bool.h`, `Mission12.h`, `Mission4.h`, `PRDemo.h`, `ScriptNode.h`, `ScriptPublisher.h`, `Test_RAD.h`, `watcom.h`

**Code/ww3d2/** (4): `hueshift/hueshift.h`, `skeleton/d3dfvf.h`, `skeleton/skeleton.h`, `sorttest/sorttest.h`, `ww3dtrig.h`

**Code/WWAudio/** (3): `AABTreeSoundCullClass.h`, `SoundDefinition.h`, `StaticSoundCullObj.h`

**Code/wwlib/** (14): `CallbackHook.h`, `hashlist.h`, `hashtab.h`, `incdec.h`, `ntree.h`, `ref_ptr.h`, `rsacrypt.h`, `search.h`, `smartptr.h`, `stl.h`, `surfrect.h`, `trackwin.h`, `trackxy.h`, `watcom.h`

**Code/WWMath/** (2): `culltype.h`, `normalcone.h`

**Code/wwphys/** (4): `bpt.h`, `cloud.h`, `phunits.h`, `zone.h`

**compat/** (16): `atlbase.h`, `commando/nat.h`, `crtdbg.h`, `dsound.h`, `imagehlp.h`, `io.h`, `ocidl.h`, `ole2.h`, `packet.h`, `regexpr.h`, `rpc.h`, `shlobj.h`, `snmp.h`, `stdafx.h`, `vfw.h`, `winsock2.h`

---

## Phase 2: Delete 8 single-include forwarder headers & rewrite their includes

| Forwarder file | Forwards to | Action |
|---|---|---|
| `Code/wwlib/always.h` | `global.h` | Remove includes (Phase 3 adds global.h) |
| `Code/Scripts/always.h` | `global.h` | Remove includes |
| `compat/windef.h` | `global.h` | Remove includes |
| `compat/c16string.h` | `global.h` | Remove includes |
| `compat/osdep/osdep.h` | `../osdep.h` | Replace with `#include "compat/osdep.h"` |
| `compat/malloc.h` | `<stdlib.h>` | Replace with `#include <stdlib.h>` |
| `compat/new.h` | `<new>` | Replace with `#include <new>` |
| `Code/wwlib/stl.h` | `<map>` | Replace with `#include <map>` |

---

## Phase 3: Transform all .h files — `#pragma once` + `global.h` first

For every `.h` file under `original/` (excluding `build/`):
1. Remove include guards (`#ifndef GUARD` / `#define GUARD` + matching `#endif`)
2. Add `#pragma once` as first line (followed by blank line)
3. Ensure `#include "global.h"` is first `#include` (after `#pragma once\n\n`)
4. Remove redundant always.h/windef.h/c16string.h includes

### Exclusions:
- `global.h` itself — gets `#pragma once` but not self-include
- `compat/typesizes.h` — included mid-file in winnt.h, must not get global.h prepended
- `tools/primitive-type-check/PrimitiveTypeCheck.h` — clang-tidy plugin, not game code

---

## Phase 4: Transform all .cpp files — `global.h` first

For every `.cpp` file under `original/` (excluding `build/`):
1. Remove any `#include "always.h"` / `#include <always.h>` lines
2. Ensure `#include "global.h"` is the first `#include` directive

### Exclusions:
- Empty stub files (no `#include` directives)
- `compat/macos_fix.mm` — Objective-C++ file

---

## Phase 5: Add pre-compiled header (PCH) for global.h

Add to CMakeLists.txt:
```cmake
target_precompile_headers(Commando PRIVATE "global.h")
```

---

## Phase 6: Update CLAUDE.md

Update the `global.h` section to reflect:
- Forwarder headers (`always.h`, `windef.h`, `c16string.h`) are DELETED
- All files use `#pragma once` exclusively (no include guards)
- `global.h` is included explicitly as first include in every .h and .cpp file
- PCH is enabled for global.h

---

## Verification

1. `cmake -S original -B original/build -G Ninja` — reconfigure after file deletions
2. `cmake --build original/build --target Commando -j8` — must compile cleanly
3. Spot-checks:
   - `grep -r 'always\.h' original/ --include='*.h' --include='*.cpp' | grep -v build/` → zero results
   - `grep -rn '#ifndef.*_H' original/ --include='*.h' | grep -v build/` → zero include-guard matches
   - Verify a representative file like `building.h` starts with `#pragma once\n\n#include "global.h"`
