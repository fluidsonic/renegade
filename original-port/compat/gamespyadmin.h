#pragma once
// gamespyadmin.h stub - GameSpy admin removed

class WideStringClass;

class cGameSpyAdmin {
public:
    static bool Is_Gamespy_Game(void) { return false; }
    static bool I_Am_Server(void) { return false; }
    static bool Get_Is_Server_Gamespy_Listed(void) { return false; }
    static bool Get_Is_Launched_From_Gamespy(void) { return false; }
    static bool Is_Nickname_Collision(const WideStringClass& nick) { return false; }
    static bool Is_Nickname_Collision(const wchar_t* nick) { return false; }
    static const wchar_t* Get_Password_Attempt(void) { return L""; }
    static void Set_Game_Host_Ip(unsigned long ip) {}
    static void Set_Game_Host_Port(unsigned short port) {}
    static void Set_Is_Launch_From_Gamespy_Requested(bool v) {}
    static void Set_Is_Server_Gamespy_Listed(bool v) {}
    static void Set_Is_Under_Gamespy_Menuing(bool v) {}
    static void Set_Password_Attempt(const wchar_t* v) {}
    static void Set_Player_Nickname(const char *name) {}
    static void Reset(void) {}
    static void Think(void) {}
    static int  GameSpyBandwidthType;
    static void Find(void) {}
};
