# Type Enforcement Design

**Date:** 2026-02-22
**Status:** Approved

## Overview

Three coordinated changes to enforce fixed-width types and flag narrowing conversions across the C++ port:

1. **Narrowing/conversion warnings** — CMakeLists.txt flag changes
2. **C++23 bump** — enables `<stdfloat>` for `std::float32_t` / `std::float64_t`
3. **Primitive type clang-tidy plugin** — custom `.dylib` loaded via a `lint` CMake target

Existing violations are not fixed as part of this change; the warnings are enabled and violations are addressed separately per module.

---

## Part 1: Narrowing and Conversion Warnings

Changes to `original/CMakeLists.txt`:

```cmake
# Remove:
-Wno-narrowing

# Add:
-Wconversion          # implicit conversions that lose precision or change value
-Wsign-conversion     # signed ↔ unsigned implicit conversions
-Wdouble-promotion    # implicit float → double widening
```

`-Wconversion` is a superset of `-Wnarrowing`; together they cover all cases of lossy implicit conversion. These are **warnings** (not `-Werror`) initially; individual modules can be locked in with `-Werror` once clean.

---

## Part 2: C++23 Bump

```cmake
set(CMAKE_CXX_STANDARD 23)
```

**Float types:** `float` and `double` are kept as-is — `float32_t`/`float64_t` from `<stdfloat>` are distinct types (not just aliases) and are not available in the current toolchain anyway. Instead, their sizes are guaranteed at compile time via:

```cpp
// original/compat/floattypes.h
static_assert(sizeof(float)  == 4, "float must be 32-bit IEEE 754");
static_assert(sizeof(double) == 8, "double must be 64-bit IEEE 754");
```

`float` and `double` are whitelisted in the plugin (not flagged).

---

## Part 3: Primitive Type clang-tidy Plugin

### Location

```
original/tools/primitive-type-check/
  CMakeLists.txt
  PrimitiveTypeCheck.h
  PrimitiveTypeCheck.cpp
```

### Rule

**Raw `BuiltinType` nodes in user declarations are flagged**, except the explicitly allowed set. This naturally handles typedefs: `int32_t x` is a `TypedefType` (passes); `int x` is a raw `BuiltinType` (flagged).

### Allowed raw built-ins (not flagged)

| Type | Reason |
|------|--------|
| `bool` | No fixed-width equivalent |
| `void` | Not a value type |
| `std::nullptr_t` | Not a value type |
| `char8_t` | C++ Unicode type — built-in, not a typedef |
| `char16_t` | C++ Unicode type — built-in, not a typedef |
| `char32_t` | C++ Unicode type — built-in, not a typedef |
| `float` | Allowed — size guaranteed 4 bytes by `static_assert` in floattypes.h |
| `double` | Allowed — size guaranteed 8 bytes by `static_assert` in floattypes.h |

### Flagged raw built-ins

`char`, `signed char`, `unsigned char`, `short`, `unsigned short`, `int`, `unsigned int`, `long`, `unsigned long`, `long long`, `unsigned long long`, `long double`, `wchar_t`

### Scope

Only files under `original/Code/` are checked. `original/compat/` and all system headers are excluded:
- System headers: excluded automatically via `isInSystemHeader()`
- compat/: excluded via `HeaderFilterRegex: "original/Code/.*"`

### AST matchers

- `varDecl` — local and global variables
- `fieldDecl` — struct/class members
- `parmVarDecl` — function parameters
- `functionDecl` — return types

### Diagnostic format

```
soldier.cpp:42:5: warning: use a fixed-width type instead of 'int' [ccr-primitive-type]
    int health = 100;
    ^
```

### Check ID

`ccr-primitive-type`

---

## Part 4: CMake Integration

### Prerequisites

```bash
brew install llvm
```

### Plugin build target

`original/tools/primitive-type-check/CMakeLists.txt` builds `PrimitiveTypeCheck.dylib` linked against LLVM's clang-tidy libraries.

### Lint target (root CMakeLists.txt)

```cmake
find_program(CLANG_TIDY_EXE NAMES clang-tidy
    HINTS /opt/homebrew/opt/llvm/bin NO_DEFAULT_PATH)
if(CLANG_TIDY_EXE)
    add_subdirectory(tools/primitive-type-check)
    add_custom_target(lint
        COMMAND run-clang-tidy -clang-tidy-binary ${CLANG_TIDY_EXE}
                -extra-arg=--load=$<TARGET_FILE:PrimitiveTypeCheck>
                -checks=ccr-primitive-type
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
        DEPENDS PrimitiveTypeCheck
    )
endif()
```

Run via:
```bash
cmake --build original/build --target lint
```

---

## Migration Path

1. Enable warnings (many violations expected — this is by design)
2. Run `--target lint` to generate a full report of primitive type violations
3. Fix violations module by module (`wwlib`, `wwnet`, `Combat`, etc.)
4. Per module, add `-Werror=conversion -Werror=ccr-primitive-type` to that module's `CMakeLists.txt` once clean

The narrowing warnings follow the same incremental path.
