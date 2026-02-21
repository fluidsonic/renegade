#pragma once
// DlgWOLWait.h stub - WOL wait dialog removed
#include <WWOnline/WaitCondition.h>
#include <WWOnline/RefPtr.h>

class DlgWOLWaitEvent {
public:
    WaitCondition::WaitResult Result(void) const { return WaitCondition::ConditionMet; }
};

class DlgWOLWait {
public:
    static void DoDialog(void) {}
    template<typename A, typename B, typename C>
    static void DoDialog(A, B, C) {}
    static DlgWOLWait* Get_Instance(void) { return nullptr; }
};
