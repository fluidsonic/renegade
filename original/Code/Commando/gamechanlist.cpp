// gamechanlist.cpp - stub (WWOnline removed)
#include "gamedata.h"
#include "gamechannel.h"
#include "gamechanlist.h"
#include "wwdebug.h"

SList<cGameChannel> cGameChannelList::ChanList;

void cGameChannelList::Add_Channel(cGameData* p_game_data, const RefPtr<WWOnline::ChannelData>& p_channel)
{
    WWASSERT(p_game_data != NULL);
    // WOL removed - no-op
    delete p_game_data;
}

cGameChannel* cGameChannelList::Find_Channel(const WideStringClass& owner)
{
    return nullptr;
}

void cGameChannelList::Remove_All(void)
{
    ChanList.Remove_All();
}

void cGameChannelList::Remove_Channel(const WideStringClass& owner)
{
    // WOL removed - no-op
    (void)owner;
}
