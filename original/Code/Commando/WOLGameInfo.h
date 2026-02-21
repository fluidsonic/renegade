#pragma once
#ifndef __WOLGAMEINFO_H__
#define __WOLGAMEINFO_H__

// WOLGameInfo stub - Westwood Online game info (dead service)
#include "widestring.h"
#include "wwstring.h"

class WOLGameInfo {
public:
    WOLGameInfo() : mMaxPlayers(0), mNumPlayers(0), mVersion(0), mClanID1(0), mClanID2(0),
        mIsDedicated(false), mIsPassworded(false), mIsLaddered(false), mIsFriendlyFire(false),
        mIsFreeWeapons(false), mIsTeamRemix(false), mIsTeamChange(false), mIsClanGame(false),
        mIsQuickmatch(false), mIsRepairBuildings(false), mIsDriverGunner(false),
        mIsSpawnWeapons(false) {}

    const char*        MapName(void) const { return mMapName; }
    const char*        ModName(void) const { return mModName; }
    const WideStringClass& Title(void) const { return mTitle; }
    int                MaxPlayers(void) const { return mMaxPlayers; }
    int                NumPlayers(void) const { return mNumPlayers; }
    unsigned long      Version(void) const { return mVersion; }
    unsigned long      ClanID1(void) const { return mClanID1; }
    unsigned long      ClanID2(void) const { return mClanID2; }
    bool               IsDedicated(void) const { return mIsDedicated; }
    bool               IsPassworded(void) const { return mIsPassworded; }
    bool               IsLaddered(void) const { return mIsLaddered; }
    bool               IsFriendlyFire(void) const { return mIsFriendlyFire; }
    bool               IsFreeWeapons(void) const { return mIsFreeWeapons; }
    bool               IsTeamRemix(void) const { return mIsTeamRemix; }
    bool               IsTeamChange(void) const { return mIsTeamChange; }
    bool               IsClanGame(void) const { return mIsClanGame; }
    bool               IsQuickmatch(void) const { return mIsQuickmatch; }
    bool               IsRepairBuildings(void) const { return mIsRepairBuildings; }
    bool               IsDriverGunner(void) const { return mIsDriverGunner; }
    bool               IsSpawnWeapons(void) const { return mIsSpawnWeapons; }
    bool               IsMapValid(void) const { return false; }
    bool               IsDataValid(void) const { return false; }

    // Import stubs for WOL/LAN channels
    template<typename T>
    void               ImportFromChannel(const T&) {}
    template<typename T>
    void               ImportFromGame(const T&) {}

    char               mMapName[64] = {};
    char               mModName[64] = {};
    WideStringClass    mTitle;
    int                mMaxPlayers;
    int                mNumPlayers;
    unsigned long      mVersion;
    unsigned long      mClanID1;
    unsigned long      mClanID2;
    bool               mIsDedicated;
    bool               mIsPassworded;
    bool               mIsLaddered;
    bool               mIsFriendlyFire;
    bool               mIsFreeWeapons;
    bool               mIsTeamRemix;
    bool               mIsTeamChange;
    bool               mIsClanGame;
    bool               mIsQuickmatch;
    bool               mIsRepairBuildings;
    bool               mIsDriverGunner;
    bool               mIsSpawnWeapons;
};

#endif // __WOLGAMEINFO_H__
