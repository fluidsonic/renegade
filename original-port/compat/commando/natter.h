// natter.h compat stub for macOS - WOL NAT traversal stubs
#pragma once
#ifndef NATTER_H
#define NATTER_H

#include "always.h"

// Forward declarations
class IPAddressClass;
class SocketHandlerClass;
class StringClass;
class cPacket;

// Minimal stub for WOLNATInterfaceClass - WOL servers are dead, no-op stubs
class WOLNATInterfaceClass {
public:
    WOLNATInterfaceClass() {}
    ~WOLNATInterfaceClass() {}
    void Init() {}
    void Shutdown() {}
    void Service() {}
    void Set_Server(bool) {}
    void Set_Server_Negotiated_Address(IPAddressClass*) {}
    void Intercept_Game_Packet(cPacket&) {}
    void Get_Compact_Log(StringClass&) {}
    void Set_Service_Socket_Handler(SocketHandlerClass*) {}
    void Service_Receive_Queue(SocketHandlerClass*) {}
};

extern WOLNATInterfaceClass WOLNATInterface;

#endif // NATTER_H
