// gamechannel.cpp - stub (WWOnline removed)
#include "gamedata.h"
#include "gamechannel.h"
#include "wwdebug.h"

cGameChannel::cGameChannel(cGameData* p_game_data)
{
    WWASSERT(p_game_data != NULL);
    PGameData = p_game_data;
}

cGameChannel::~cGameChannel(void)
{
    delete PGameData;
}
