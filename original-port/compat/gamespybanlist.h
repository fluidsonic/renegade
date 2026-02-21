#pragma once
// gamespybanlist.h stub - GameSpy ban list removed
#include "wwstring.h"
#include "widestring.h"

class GameSpyBanListClass {
public:
    static bool Is_Banned(unsigned long ip) { return false; }
    static void Add_Ban(unsigned long ip) {}
    static void Remove_Ban(unsigned long ip) {}
    bool Kick_Player(int id) { return false; }
    bool Kick_Player(const WideStringClass& name) { return false; }
    void Ban_User(const char* name) {}
    void Ban_User(const StringClass& name) {}
    void Ban_User(const WideStringClass& name) {}
    void Ban_User(const char* name, const StringClass& hash) {}
    void Ban_User(const StringClass& name, const StringClass& hash) {}
    void Ban_User(const WideStringClass& name, const StringClass& hash) {}
};

extern GameSpyBanListClass GameSpyBanList;
