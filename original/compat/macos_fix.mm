/*
 * macos_fix.mm — Installs ARM64 CoreAnalytics crash workarounds before SDL2 initialises.
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
 *
 * We intentionally do NOT pre-call [NSApplication sharedApplication] here.
 * SDL2 creates its own SDLApplication subclass (which overrides terminate: to push
 * SDL_QUIT instead of calling exit()) and must be allowed to do so.  Pre-initialising
 * NSApp with a plain NSApplication would prevent SDL2 from installing its subclass,
 * causing dock Quit and Cmd-Q to have no effect.
 *
 * The ObjC swizzles (steps 1-2) modify class definitions, not instances — they work
 * before NSApp exists.  sdl2_platform.cpp wraps SDL_Init() with the SIGBUS handler
 * as belt-and-suspenders while NSApp is being created by SDL2.
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

// Call after SDL_CreateWindow so the window actually appears and has focus.
// When launched from the terminal, macOS does not automatically bring the
// app to the foreground — explicit activation is required.
extern "C" void macos_activate_app(void)
{
    if (NSApp) {
        [NSApp setActivationPolicy:NSApplicationActivationPolicyRegular];
        [NSApp activateIgnoringOtherApps:YES];
    }
}

// ---------------------------------------------------------------------------
// ObjC swizzles — run from a constructor before SDL2 initialises.
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

    // Step 3: Swizzles are class-level and do not require NSApp — remove the SIGBUS
    // handler now.  sdl2_platform.cpp re-installs it around SDL_Init() as
    // belt-and-suspenders while SDL2 creates its SDLApplication instance.
    remove_sigbus_handler();

    // Step 4: Disable the macOS press-and-hold accent picker for this process.
    // Without this, holding any key while the Cocoa text-input machinery is
    // active (which SDL2 leaves enabled by default) shows the accent popup
    // instead of repeating the key.  The NSUserDefaults write is process-local;
    // it does not change any persistent user preference.
    [[NSUserDefaults standardUserDefaults] setBool:NO
                                            forKey:@"ApplePressAndHoldEnabled"];
}
