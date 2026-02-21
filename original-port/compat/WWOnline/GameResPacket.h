// WWOnline/GameResPacket.h - Game result packet stub (Westwood Online is dead, all no-ops)
#pragma once
#ifndef WWONLINE_GAMERESPACKET_H
#define WWONLINE_GAMERESPACKET_H
#include "wwonline_types.h"

namespace WWOnline {

// GameResPacket - sends game statistics to WOL (dead service)
class GameResPacket {
public:
    GameResPacket() {}
    ~GameResPacket() {}

    // Add_Field overloads for all types the game uses
    void Add_Field(const char* name, const char* value) {}
    void Add_Field(const char* name, char value) {}
    void Add_Field(const char* name, unsigned char value) {}
    void Add_Field(const char* name, short value) {}
    void Add_Field(const char* name, unsigned short value) {}
    void Add_Field(const char* name, int value) {}
    void Add_Field(const char* name, unsigned int value) {}
    void Add_Field(const char* name, long value) {}
    void Add_Field(const char* name, unsigned long value) {}
    void Add_Field(const char* name, float value) {}
    void Add_Field(const char* name, double value) {}
    void Add_Field(const char* name, bool value) {}
    void Add_Field(const char* name, const void* data, int size) {}

    // Create a binary packet for transmission (stub - returns null)
    unsigned char* Create_Comms_Packet(unsigned long& packetSize, void* signature, unsigned long& sig_offset) {
        packetSize = 0; sig_offset = 0; return nullptr;
    }
};

} // namespace WWOnline

#endif // WWONLINE_GAMERESPACKET_H
