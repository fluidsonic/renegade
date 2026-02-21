#pragma once
#ifndef _AUTOSTART_H
#define _AUTOSTART_H

// AutoRestart stub - WOL server auto-restart feature removed
class AutoRestartClass {
public:
    void Restart_Game(void) {}
    void Think(void) {}
    bool Is_Active(void) { return false; }
    void Set_Restart_Flag(bool enable) {}
    bool Get_Restart_Flag(void) { return false; }
    void Cancel(void) {}
    static const char *REG_VALUE_AUTO_RESTART_FLAG;
    static const char *REG_VALUE_AUTO_RESTART_TYPE;
};

extern AutoRestartClass AutoRestart;

#endif //_AUTOSTART_H
