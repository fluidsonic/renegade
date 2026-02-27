# Plan: Remove empty editable macros and all usages

## Context
`original/Code/wwsaveload/editable.h` defines 24 macros (lines 59-81) that all expand to nothing. These were editor-time parameter registration macros for the Windows level editor UI — they're no-ops in this runtime build. Every invocation is dead code that clutters the source.

## Approach

### Step 1: Remove macro definitions from editable.h
Delete lines 59-81 from `original/Code/wwsaveload/editable.h`.

### Step 2: Remove all macro usages across the codebase
Use a Python script to remove all lines containing invocations of these 24 macros. The macros to remove:

- `DECLARE_EDITABLE` (~68 usages in .h files)
- `EDITABLE_PARAM` (~198 usages in .cpp files)
- `NAMED_EDITABLE_PARAM` (~95 usages)
- `INT_EDITABLE_PARAM` (14 usages)
- `INT_UNITS_PARAM` (0 usages)
- `NAMED_INT_UNITS_PARAM` (0 usages)
- `FLOAT_EDITABLE_PARAM` (35 usages)
- `FLOAT_UNITS_PARAM` (10 usages)
- `NAMED_FLOAT_UNITS_PARAM` (7 usages)
- `ANGLE_EDITABLE_PARAM` (10 usages)
- `NAMED_ANGLE_EDITABLE_PARAM` (2 usages)
- `GENERIC_EDITABLE_PARAM` (0 usages)
- `MODEL_DEF_PARAM` (6 usages)
- `PHYS_DEF_PARAM` (0 usages)
- `SCRIPT_PARAM` (0 usages)
- `SCRIPTLIST_PARAM` (2 usages)
- `ENUM_PARAM` (1 usage)
- `FILENAME_PARAM` (5 usages)
- `DEFIDLIST_PARAM` (1 usage)
- `CLASSID_DEFIDLIST_PARAM` (1 usage)
- `ZONE_PARAM` (6 usages)
- `PARAM_SEPARATOR` (26 usages)
- `GENERIC_DEFID_PARAM` (4 usages)

Total: ~490 usage lines to remove across ~50 files.

### Step 3: Build verification
Run `cmake --build original/build --target Commando -j8 -- -k 0` to confirm clean build.

## Files modified
- `original/Code/wwsaveload/editable.h` — remove lines 59-81
- ~50 files under `original/Code/Combat/`, `original/Code/wwphys/`, `original/Code/WWAudio/`, `original/Code/wwsaveload/` — remove macro invocation lines

## Execution strategy
- Use a git worktree for isolation
- Parallel subagents for different subdirectories
- After all agents complete, build to verify
