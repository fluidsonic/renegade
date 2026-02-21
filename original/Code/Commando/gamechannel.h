//
// gamechannel.h - stub (WWOnline removed)
//
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef GAMECHANNEL_H
#define GAMECHANNEL_H

#include "wwdebug.h"
#include "always.h"
#include "refcount.h"
#include <WWOnline/wwonline_types.h>

class cGameData;

class cGameChannel : public RefCountClass
{
    friend class cGameChannelList;

public:
    cGameData* Get_Game_Data(void) { WWASSERT(PGameData != NULL); return PGameData; }
    WOL::Channel* Get_Wol_Channel(void) { return &mWolChannel; }

private:
    cGameChannel(cGameData* p_game_data);
    ~cGameChannel(void);

    cGameData* PGameData = nullptr;
    WOL::Channel mWolChannel;
};

#endif // GAMECHANNEL_H
