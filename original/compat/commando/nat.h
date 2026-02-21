// nat.h compat stub for macOS - NAT traversal stubs (WOL is dead, not needed)
#pragma once
#ifndef NAT_H
#define NAT_H

#include "always.h"

// Minimal stub - no actual NAT functionality needed
#ifndef fw_assert
#define fw_assert assert
#endif

// Forward declarations
class IPAddressClass;
class SocketHandlerClass;
class StringClass;
class cPacket;

struct FirewallHelperClass {
    void Init() {}
    void Shutdown() {}
    void Service() {}
    bool Is_Initialized() { return false; }
};

extern FirewallHelperClass FirewallHelper;

#endif // NAT_H
