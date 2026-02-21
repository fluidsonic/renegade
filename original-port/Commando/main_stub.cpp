// main_stub.cpp — macOS entry point that bridges main() -> WinMain()
// WinMain is defined in WINMAIN.CPP with our compat types
#ifndef _MSC_VER

#include "windef.h"
#include "winbase.h"

// Forward-declare WinMain as defined in WINMAIN.CPP
extern int WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow);

int main(int argc, char* argv[])
{
    // Join all argv[1..] into a single command-line string
    static char cmdline[4096] = {0};
    for (int i = 1; i < argc; i++) {
        if (i > 1) { int n = 0; while (cmdline[n]) n++; cmdline[n] = ' '; cmdline[n+1] = 0; }
        int n = 0; while (cmdline[n]) n++;
        const char* s = argv[i]; while (*s) cmdline[n++] = *s++;
        cmdline[n] = 0;
    }
    return WinMain(NULL, NULL, cmdline, 1);
}

#endif // !_MSC_VER
