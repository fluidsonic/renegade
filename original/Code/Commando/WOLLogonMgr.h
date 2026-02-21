#pragma once
// WOLLogonMgr.h stub - WOL logon manager removed
#include "widestring.h"

class WOLLogonMgr {
public:
    static void ConfigureSession(void) {}
    static void Init(void) {}
    static void Shutdown(void) {}
    static bool IsLoggedIn(void) { return false; }
};
