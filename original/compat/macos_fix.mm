/*
 * macos_fix.mm — Pre-initializes NSApplication to bypass ARM64 CoreAnalytics crashes.
 *
 * Apple's CoreAnalytics has a bug on ARM64: nlohmann::basic_json::dump() executes
 * `ldadd` against a 4-byte-aligned address, but ldadd requires 8-byte alignment,
 * causing EXC_ARM_DA_ALIGN (code=257) inside [NSApplication init].  Multiple paths
 * through AppKit and Security.framework trigger it.
 *
 * Rather than patching individual paths one by one, we:
 *   1. Install a SIGBUS/BUS_ADRALN handler that silently skips alignment-faulting
 *      instructions (works outside lldb)
 *   2. Swizzle the two known ObjC telemetry entry-points to no-ops (works under lldb)
 *   3. Pre-call [NSApplication sharedApplication] in a __attribute__((constructor))
 *      — with both defences active — so NSApp is already initialised before SDL2 runs
 *   4. SDL2's Cocoa_RegisterApp finds NSApp != nil and skips re-initialisation entirely
 *
 * windef.h guards `typedef int BOOL` with #ifdef __OBJC__ so this translation unit
 * gets the correct ObjC `typedef bool BOOL` from objc/objc.h.
 */

#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>
#import <objc/runtime.h>
#include <signal.h>
#include <sys/ucontext.h>
#include <stdio.h>

// ---------------------------------------------------------------------------
// SIGBUS/BUS_ADRALN handler — skips ARM64 alignment-faulting instructions.
// NOTE: does NOT fire under lldb; the ObjC swizzles below cover that case.
// ---------------------------------------------------------------------------

static struct sigaction g_prev_sigbus;

static void sigbus_alignment_skip(int sig, siginfo_t *info, void *ctx)
{
    if (info->si_code == BUS_ADRALN) {
        // Advance PC past the 4-byte ARM64 instruction that faulted.
        // CoreAnalytics telemetry will fail silently; game is unaffected.
        ucontext_t *uc = (ucontext_t *)ctx;
        uc->uc_mcontext->__ss.__pc += 4;
        return;
    }
    // Not an alignment fault — forward to the previous handler.
    if (g_prev_sigbus.sa_flags & SA_SIGINFO) {
        g_prev_sigbus.sa_sigaction(sig, info, ctx);
    } else if (g_prev_sigbus.sa_handler != SIG_DFL &&
               g_prev_sigbus.sa_handler != SIG_IGN) {
        g_prev_sigbus.sa_handler(sig);
    } else {
        signal(SIGBUS, SIG_DFL);
        raise(SIGBUS);
    }
}

static void install_sigbus_handler(void)
{
    struct sigaction sa = {};
    sa.sa_sigaction = sigbus_alignment_skip;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGBUS, &sa, &g_prev_sigbus);
}

static void remove_sigbus_handler(void)
{
    sigaction(SIGBUS, &g_prev_sigbus, NULL);
}

// Public API used by sdl2_platform.cpp as belt-and-suspenders around SDL_Init.
extern "C" void macos_install_alignment_fault_workaround(void) { install_sigbus_handler(); }
extern "C" void macos_remove_alignment_fault_workaround(void)  { remove_sigbus_handler(); }

// ---------------------------------------------------------------------------
// ObjC swizzles + NSApp pre-initialisation
// All three steps run from a single constructor so NSApp is ready before main().
// ---------------------------------------------------------------------------

__attribute__((constructor))
static void preinit_nsapp(void)
{
    // Step 1: catch any alignment faults that survive the swizzles below.
    install_sigbus_handler();

    // Step 2a: Swizzle Path 1 — _setDoubleClickBehavior → _NSGetBoolAppConfig → CoreAnalytics
    {
        Class cls = objc_lookUpClass("NSApplication");
        if (cls) {
            SEL sel = NSSelectorFromString(@"_setDoubleClickBehavior");
            Method m = class_getInstanceMethod(cls, sel);
            if (m)
                method_setImplementation(m, imp_implementationWithBlock(^(id /*self*/) {}));
        }
    }

    // Step 2b: Swizzle Path 2 — +[SecCoreAnalytics sendEventLazy:builder:] → CoreAnalytics
    {
        Class cls = objc_lookUpClass("SecCoreAnalytics");
        if (cls) {
            SEL sel = NSSelectorFromString(@"sendEventLazy:builder:");
            Method m = class_getClassMethod(cls, sel);
            if (m) {
                method_setImplementation(m, imp_implementationWithBlock(
                    ^(id /*self*/, NSString *name, NSDictionary*(^builder)(void)) {
                        fprintf(stderr,
                            "[CoreAnalytics] Suppressed +sendEventLazy: %s\n",
                            name ? [name UTF8String] : "(null)");
                        (void)builder; // do not call — would crash
                    }));
            }
        }
    }

    // Step 3: Pre-initialise NSApplication.
    //   SDL2's Cocoa_RegisterApp checks `if (NSApp == nil)` before calling
    //   [NSApplication sharedApplication].  By creating it here — with the SIGBUS
    //   handler and swizzles in place — any remaining CoreAnalytics crash paths are
    //   silently skipped, and SDL2 will find NSApp already set and do nothing.
    if (NSApp == nil) {
        fprintf(stderr, "[macos_fix] Pre-initialising NSApplication...\n");
        [NSApplication sharedApplication];
        fprintf(stderr, "[macos_fix] NSApplication ready (NSApp=%p)\n", (void*)NSApp);
    }

    // Step 4: NSApp is fully initialised — alignment-fault workaround no longer needed.
    remove_sigbus_handler();
}
