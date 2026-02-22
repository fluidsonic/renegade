# Type Enforcement Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enforce fixed-width types (`(u)intN_t`, `charN_t`, `float32_t`, `float64_t`) project-wide via a custom clang-tidy plugin, and enable narrowing/conversion warnings.

**Architecture:** A `MODULE` library (`PrimitiveTypeCheck.dylib`) registers a `ClangTidyCheck` subclass that walks AST declarations and flags any raw `BuiltinType` that isn't `bool`, `void`, `char8_t`, `char16_t`, or `char32_t`. A CMake `lint` target runs `run-clang-tidy` with this plugin across all files in `original/Code/`. Narrowing and conversion warnings are separate flag-level changes in the root `CMakeLists.txt`.

**Tech Stack:** Clang-tidy plugin API (LLVM 21, `/opt/homebrew/opt/llvm`), CMake 3.20, Ninja, C++23.

---

## Status

- [x] C++23 already bumped (`CMAKE_CXX_STANDARD 23` in `original/CMakeLists.txt`)
- [ ] `<stdfloat>` not available in Apple Clang or LLVM 21 libc++ → provide `float32_t`/`float64_t` via `original/compat/floattypes.h`
- [ ] Narrowing/conversion warnings not yet enabled
- [ ] Plugin not yet written
- [ ] `lint` CMake target not yet wired

---

## Task 1: Remove deprecated `-fdelayed-template-parsing` flag

C++23 deprecated this flag (clang warns about it on every TU). Remove it.

**Files:**
- Modify: `original/CMakeLists.txt`

**Step 1: Remove the flag**

In `original/CMakeLists.txt`, delete the line:
```cmake
    -fdelayed-template-parsing
```

**Step 2: Build to confirm it still compiles**

```bash
cmake --build /Users/marc/Documents/ccr/original/build --target Commando -j8 2>&1 | tail -10
```
Expected: build succeeds (same warnings as before, minus the deprecation notice).

**Step 3: Commit**

```bash
git add original/CMakeLists.txt
git commit -m "build: bump to C++23, remove deprecated -fdelayed-template-parsing"
```

---

## Task 2: Add `float32_t` / `float64_t` type aliases

`<stdfloat>` is not available in the current toolchain (`LLVM 21 + Apple libc++`). Provide the aliases via a new compat header force-included from `windef.h`.

**Files:**
- Create: `original/compat/floattypes.h`
- Modify: `original/compat/windef.h` (one line: `#include "floattypes.h"`)

**Step 1: Create `original/compat/floattypes.h`**

```cpp
#pragma once
// Portable fixed-width float types.
// Replace with <stdfloat> when toolchain support is available.
#include <cstdint>

static_assert(sizeof(float)  == 4, "float must be 32-bit IEEE 754");
static_assert(sizeof(double) == 8, "double must be 64-bit IEEE 754");

using float32_t = float;
using float64_t = double;
```

**Step 2: Add include to `original/compat/windef.h`**

Find the bottom of the existing `#pragma once` block (before any other content, or at the very end). Add:
```cpp
#include "floattypes.h"
```

Place it near the other type-providing includes (after `<cstdint>` is already included).

**Step 3: Build to confirm no conflicts**

```bash
cmake --build /Users/marc/Documents/ccr/original/build --target Commando -j8 2>&1 | grep -i "error\|float32\|float64" | head -20
```
Expected: no errors mentioning `float32_t` or `float64_t`.

**Step 4: Commit**

```bash
git add original/compat/floattypes.h original/compat/windef.h
git commit -m "compat: add float32_t/float64_t aliases (C++23 stdfloat fallback)"
```

---

## Task 3: Enable narrowing and conversion warnings

**Files:**
- Modify: `original/CMakeLists.txt`

**Step 1: Edit `original/CMakeLists.txt`**

In the `add_compile_options(...)` block:
- Remove: `-Wno-narrowing`
- Add after the existing `-Werror=non-pod-varargs`:
```cmake
    -Wconversion
    -Wsign-conversion
    -Wdouble-promotion
```

**Step 2: Build to see the warning count**

```bash
cmake --build /Users/marc/Documents/ccr/original/build --target Commando -j8 2>&1 | grep -c "warning:" || true
```
Expected: many warnings (hundreds expected — this is normal; they'll be fixed per-module separately).

**Step 3: Confirm build still succeeds**

```bash
cmake --build /Users/marc/Documents/ccr/original/build --target Commando -j8 2>&1 | grep "^.*error:" | grep -v "warning:" | head -20
```
Expected: no lines (no errors, only warnings).

**Step 4: Commit**

```bash
git add original/CMakeLists.txt
git commit -m "build: enable -Wconversion, -Wsign-conversion, -Wdouble-promotion; re-enable -Wnarrowing"
```

---

## Task 4: Create the plugin directory and CMakeLists

**Files:**
- Create: `original/tools/primitive-type-check/CMakeLists.txt`

**Step 1: Create directory**

```bash
mkdir -p /Users/marc/Documents/ccr/original/tools/primitive-type-check
```

**Step 2: Write `original/tools/primitive-type-check/CMakeLists.txt`**

```cmake
# Primitive-type clang-tidy check plugin
# Requires: brew install llvm  (LLVM 18+ at /opt/homebrew/opt/llvm)
set(LLVM_PREFIX "/opt/homebrew/opt/llvm" CACHE PATH "LLVM installation prefix")

add_library(PrimitiveTypeCheck MODULE
    PrimitiveTypeCheck.cpp
)

target_include_directories(PrimitiveTypeCheck PRIVATE
    "${LLVM_PREFIX}/include"
)

target_compile_options(PrimitiveTypeCheck PRIVATE
    -fno-rtti   # LLVM is built without RTTI; all plugins must match
    -fPIC
)

target_link_libraries(PrimitiveTypeCheck PRIVATE
    "${LLVM_PREFIX}/lib/libclang-cpp.dylib"
    "${LLVM_PREFIX}/lib/libLLVM.dylib"
)

set_target_properties(PrimitiveTypeCheck PROPERTIES
    CXX_STANDARD 17
    PREFIX ""
    SUFFIX ".dylib"
    MACOSX_RPATH ON
    BUILD_WITH_INSTALL_RPATH TRUE
    INSTALL_RPATH "${LLVM_PREFIX}/lib"
)
```

No step to build yet — source files come in the next task.

---

## Task 5: Write `PrimitiveTypeCheck.h`

**Files:**
- Create: `original/tools/primitive-type-check/PrimitiveTypeCheck.h`

```cpp
#pragma once
#include "clang-tidy/ClangTidyCheck.h"

namespace ccr {

/// Warns when a declaration uses a raw C++ built-in type instead of a
/// fixed-width alias ((u)intN_t, charN_t, float32_t, float64_t).
///
/// Allowed raw built-ins: bool, void, nullptr_t, char8_t, char16_t, char32_t.
/// Everything else (int, float, double, char, long, short, …) is flagged.
/// Typedefs are NOT flagged — int32_t, float32_t, size_t all pass through.
class PrimitiveTypeCheck : public clang::tidy::ClangTidyCheck {
public:
    PrimitiveTypeCheck(llvm::StringRef Name, clang::tidy::ClangTidyContext *Context)
        : ClangTidyCheck(Name, Context) {}

    void registerMatchers(clang::ast_matchers::MatchFinder *Finder) override;
    void check(const clang::ast_matchers::MatchFinder::MatchResult &Result) override;

private:
    /// Returns true if QT is a raw BuiltinType that should be flagged.
    /// Returns false for typedefs (even if the underlying type is an int).
    static bool isForbiddenRawBuiltin(clang::QualType QT);

    /// Reports a diagnostic if QT is forbidden and Loc is in Code/.
    void checkType(clang::QualType QT, clang::SourceLocation Loc,
                   const clang::ASTContext &Ctx) const;
};

} // namespace ccr
```

---

## Task 6: Write `PrimitiveTypeCheck.cpp`

**Files:**
- Create: `original/tools/primitive-type-check/PrimitiveTypeCheck.cpp`

```cpp
#include "PrimitiveTypeCheck.h"
#include "clang-tidy/ClangTidyModule.h"
#include "clang-tidy/ClangTidyModuleRegistry.h"
#include "clang/AST/ASTContext.h"
#include "clang/ASTMatchers/ASTMatchFinder.h"
#include "clang/ASTMatchers/ASTMatchers.h"

using namespace clang;
using namespace clang::ast_matchers;

namespace ccr {

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

bool PrimitiveTypeCheck::isForbiddenRawBuiltin(QualType QT) {
    // Strip qualifiers but preserve sugar (typedefs).
    // If it is NOT directly a BuiltinType (e.g. it is a TypedefType), return false.
    const Type *T = QT.getTypePtr();
    const auto *BT = dyn_cast<BuiltinType>(T);
    if (!BT)
        return false;

    switch (BT->getKind()) {
        // ── Allowed raw built-ins ──────────────────────────────────────────
        case BuiltinType::Bool:     // bool
        case BuiltinType::Void:     // void
        case BuiltinType::NullPtr:  // nullptr_t
        case BuiltinType::Char8:    // char8_t  (C++20)
        case BuiltinType::Char16:   // char16_t (C++11)
        case BuiltinType::Char32:   // char32_t (C++11)
            return false;

        // ── Everything else is forbidden ───────────────────────────────────
        // char, signed char, unsigned char
        // short, unsigned short
        // int, unsigned int
        // long, unsigned long
        // long long, unsigned long long
        // float, double, long double
        // wchar_t
        // __int128, __float128, etc.
        default:
            return true;
    }
}

static bool isInCodeDir(SourceLocation Loc, const SourceManager &SM) {
    if (Loc.isInvalid())
        return false;
    llvm::StringRef File = SM.getFilename(SM.getSpellingLoc(Loc));
    return File.contains("/Code/");
}

void PrimitiveTypeCheck::checkType(QualType QT, SourceLocation Loc,
                                   const ASTContext &Ctx) const {
    if (!isInCodeDir(Loc, Ctx.getSourceManager()))
        return;
    if (!isForbiddenRawBuiltin(QT))
        return;

    const auto *BT = QT.getTypePtr()->castAs<BuiltinType>();
    PrintingPolicy PP = Ctx.getPrintingPolicy();
    PP.SuppressTagKeyword = false;

    diag(Loc,
         "use a fixed-width type instead of %0; "
         "prefer (u)intN_t, charN_t, float32_t, or float64_t")
        << BT->getName(PP);
}

// ---------------------------------------------------------------------------
// Matchers
// ---------------------------------------------------------------------------

void PrimitiveTypeCheck::registerMatchers(MatchFinder *Finder) {
    // Variable declarations (local, global, static member)
    Finder->addMatcher(
        varDecl(unless(isExpansionInSystemHeader()),
                unless(isImplicit()))
            .bind("var"),
        this);

    // Struct / class field declarations
    Finder->addMatcher(
        fieldDecl(unless(isExpansionInSystemHeader()))
            .bind("field"),
        this);

    // Function parameters
    Finder->addMatcher(
        parmVarDecl(unless(isExpansionInSystemHeader()),
                    unless(isImplicit()))
            .bind("parm"),
        this);

    // Function return types
    Finder->addMatcher(
        functionDecl(unless(isExpansionInSystemHeader()),
                     unless(isImplicit()))
            .bind("func"),
        this);
}

// ---------------------------------------------------------------------------
// Check callback
// ---------------------------------------------------------------------------

void PrimitiveTypeCheck::check(const MatchFinder::MatchResult &Result) {
    const ASTContext &Ctx = *Result.Context;

    if (const auto *VD = Result.Nodes.getNodeAs<VarDecl>("var"))
        checkType(VD->getType(), VD->getTypeSpecStartLoc(), Ctx);

    if (const auto *FD = Result.Nodes.getNodeAs<FieldDecl>("field"))
        checkType(FD->getType(), FD->getTypeSpecStartLoc(), Ctx);

    if (const auto *PD = Result.Nodes.getNodeAs<ParmVarDecl>("parm"))
        checkType(PD->getType(), PD->getTypeSpecStartLoc(), Ctx);

    if (const auto *FuncD = Result.Nodes.getNodeAs<FunctionDecl>("func"))
        checkType(FuncD->getReturnType(),
                  FuncD->getReturnTypeSourceRange().getBegin(), Ctx);
}

} // namespace ccr

// ---------------------------------------------------------------------------
// Module registration — required for `clang-tidy --load`
// ---------------------------------------------------------------------------

class CcrModule : public clang::tidy::ClangTidyModule {
public:
    void addCheckFactories(clang::tidy::ClangTidyCheckFactories &Factories) override {
        Factories.registerCheck<ccr::PrimitiveTypeCheck>("ccr-primitive-type");
    }
};

static clang::tidy::ClangTidyModuleRegistry::Add<CcrModule>
    X("ccr-module", "CCR-specific checks: fixed-width type enforcement.");
```

---

## Task 7: Wire plugin and lint target into root CMakeLists

**Files:**
- Modify: `original/CMakeLists.txt`

**Step 1: Add `CMAKE_EXPORT_COMPILE_COMMANDS` and lint target**

At the bottom of `original/CMakeLists.txt`, add:

```cmake
# ── Primitive-type lint (requires brew install llvm) ──────────────────────────
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

find_program(CLANG_TIDY_EXE NAMES clang-tidy
    HINTS /opt/homebrew/opt/llvm/bin NO_DEFAULT_PATH)
find_program(RUN_CLANG_TIDY NAMES run-clang-tidy
    HINTS /opt/homebrew/opt/llvm/bin NO_DEFAULT_PATH)

if(CLANG_TIDY_EXE AND RUN_CLANG_TIDY)
    add_subdirectory(tools/primitive-type-check)

    add_custom_target(lint
        COMMAND "${RUN_CLANG_TIDY}"
                -clang-tidy-binary "${CLANG_TIDY_EXE}"
                -load "$<TARGET_FILE:PrimitiveTypeCheck>"
                -checks=-*,ccr-primitive-type
                -header-filter=".*/Code/.*"
                -p "${CMAKE_BINARY_DIR}"
                -j8
        WORKING_DIRECTORY "${CMAKE_BINARY_DIR}"
        DEPENDS PrimitiveTypeCheck
        COMMENT "Running CCR primitive-type lint across Code/"
    )
else()
    message(STATUS "clang-tidy not found at /opt/homebrew/opt/llvm/bin — lint target unavailable")
endif()
```

**Step 2: Reconfigure to pick up the new subdirectory and export compile commands**

```bash
cmake -S /Users/marc/Documents/ccr/original -B /Users/marc/Documents/ccr/original/build -G Ninja
```

Expected: configuration succeeds; `compile_commands.json` now appears in the build dir.

```bash
ls /Users/marc/Documents/ccr/original/build/compile_commands.json
```

---

## Task 8: Build and smoke-test the plugin

**Step 1: Build the plugin dylib**

```bash
cmake --build /Users/marc/Documents/ccr/original/build --target PrimitiveTypeCheck -j8 2>&1
```

Expected: compiles without errors; output ends with something like:
```
[3/3] Linking CXX shared module tools/primitive-type-check/PrimitiveTypeCheck.dylib
```

If you see RTTI mismatch errors (`cannot dynamic_cast`, vtable errors), add `-fno-rtti` to the compile options in `tools/primitive-type-check/CMakeLists.txt` (it should already be there).

**Step 2: Verify the dylib exports the plugin symbol**

```bash
nm -g /Users/marc/Documents/ccr/original/build/tools/primitive-type-check/PrimitiveTypeCheck.dylib \
    | grep -i "clang_tidy\|CcrModule"
```

Expected: at least one symbol related to `CcrModule` or `clang_tidy`.

**Step 3: Run a quick single-file smoke test**

Pick one file known to have `int` declarations:
```bash
/opt/homebrew/opt/llvm/bin/clang-tidy \
    --load=/Users/marc/Documents/ccr/original/build/tools/primitive-type-check/PrimitiveTypeCheck.dylib \
    --checks="-*,ccr-primitive-type" \
    --header-filter=".*/Code/.*" \
    -p /Users/marc/Documents/ccr/original/build \
    /Users/marc/Documents/ccr/original/Code/Combat/soldier.cpp \
    2>&1 | head -30
```

Expected: lines like:
```
/Users/marc/.../soldier.cpp:42:5: warning: use a fixed-width type instead of 'int' [ccr-primitive-type]
```

If no output: the check may not be matching. Try removing `-checks="-*,..."` to see all available checks and confirm `ccr-primitive-type` appears in the list.

**Step 4: Commit plugin source and CMakeLists changes**

```bash
git add original/tools/primitive-type-check/ original/CMakeLists.txt
git commit -m "build: add ccr-primitive-type clang-tidy plugin and lint target"
```

---

## Task 9: Run full lint and save the baseline violation report

**Step 1: Run lint across all of Code/**

```bash
cmake --build /Users/marc/Documents/ccr/original/build --target lint 2>&1 \
    | tee /Users/marc/Documents/ccr/.tmp/primitive-type-violations.txt
```

This will take a few minutes (all TUs in parallel).

**Step 2: Count violations by file**

```bash
grep "ccr-primitive-type" /Users/marc/Documents/ccr/.tmp/primitive-type-violations.txt \
    | sed 's|:.* warning:.*||' | sort | uniq -c | sort -rn | head -30
```

This gives you the hottest files to fix first.

**Step 3: Commit the violation report**

```bash
git add /Users/marc/Documents/ccr/.tmp/primitive-type-violations.txt 2>/dev/null || true
# .tmp/ is gitignored, so just keep it locally for reference
```

---

## Notes

### Why typedefs pass the check
`int32_t x` has a `TypedefType` node as its type, not `BuiltinType`. `isForbiddenRawBuiltin` checks `getTypePtr()` (with sugar, not canonical), so `int32_t` is NOT a `BuiltinType` and passes. Only a raw `int x` declaration triggers the warning.

### Template instantiations
Template parameters come from the template definition, not instantiation sites. `isImplicit()` filters most compiler-generated nodes. If false positives appear in template heavy code, add `unless(ast_matchers::isInstantiated())` to the matchers.

### Tightening to errors per-module
Once a module is clean, add to its `CMakeLists.txt`:
```cmake
target_compile_options(wwlib PRIVATE -Werror=conversion -Werror=sign-conversion)
```
For the plugin check, add `-warnings-as-errors=ccr-primitive-type` to the lint target for that module's files.

### `<stdfloat>` future
When Apple Clang gains `<stdfloat>` support, replace `original/compat/floattypes.h` with:
```cpp
#include <stdfloat>
using float32_t = std::float32_t;
using float64_t = std::float64_t;
```
