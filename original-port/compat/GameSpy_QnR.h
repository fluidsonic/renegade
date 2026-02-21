#pragma once
// GameSpy_QnR.h stub - GameSpy removed
class cGameSpyQnR {
public:
    void Init(void) {}
    void Enable_Reporting(bool enable) { (void)enable; }
    const char* Get_Default_HeartBeat_List(void) { return ""; }
    bool Parse_HeartBeat_List(const char* list) { (void)list; return false; }
    void Think(void) {}
    void Shutdown(void) {}
    void LaunchArcade(void) {}
    bool IsEnabled(void) const { return false; }
    void TrackUsage(void) {}
};
extern cGameSpyQnR GameSpyQnR;
