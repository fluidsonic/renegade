#pragma once
// wolloginprofile.h stub - WOL login profile removed
#include "widestring.h"

class LoginProfile {
public:
    static LoginProfile* Get(const WideStringClass& name) { return nullptr; }
    const wchar_t* GetNickname(void) const { return L""; }
    const wchar_t* GetPassword(void) const { return L""; }
    int  GetLocale(void) const { return 0; }
    int  GetSidePreference(void) const { return -1; }
    void Release_Ref(void) {}
};
