#pragma once
// wolgmode.h stub - WOL game mode removed
#include "gamemode.h"
#include "widestring.h"

class cGameData;

class WolGameModeClass : public GameModeClass {
public:
    virtual const char* Name(void) { return "WOL"; }
    virtual void Init(void) {}
    virtual void Shutdown(void) {}
    virtual void Render(void) {}
    virtual void Think(void) {}
    void Accept_Actions(void) {}
    void Refusal_Actions(void) {}
    void System_Timer_Reset(void) {}
    // Kick/Ban stubs
    bool Kick_Player(const WideStringClass& name) { return false; }
    bool Kick_Player(int id) { return false; }
    void Ban_Player(const WideStringClass& name, unsigned long ip) {}
    // WOL game management stubs
    template<typename T> void SignalMe(T& observer) {}
    void Create_Game(cGameData* game = nullptr) {}
    bool Channel_Create_OK(void) { return true; }
    void Start_Game(cGameData* game = nullptr) {}
    void End_Game(void) {}
    void* Get_WOL_User_Data(const WideStringClass& owner) { return nullptr; }
    // Chat/page stubs
    void Page_WOL_User(const WideStringClass& name, const WideStringClass& msg) {}
    void Reply_Last_Page(const WideStringClass& msg) {}
    void Locate_WOL_User(const WideStringClass& name) {}
    void Invite_WOL_User(const WideStringClass& name, const WideStringClass& msg = WideStringClass(L"")) {}
    void Join_WOL_User(const WideStringClass& name) {}
    void Leave_Game(void) {}
    void Init_WOL_Player(void* player) {}
    bool Post_Game_Check(void) { return false; }
};
