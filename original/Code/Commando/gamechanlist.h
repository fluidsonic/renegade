//
// Filename:     gamechanlist.h
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef GAMECHANLIST_H
#define GAMECHANLIST_H

#include "always.h"
#include "bittype.h"
#include <WWOnline/RefPtr.h>

class cGameData;
class cGameChannel;

namespace WWOnline
{
class ChannelData;
}

#include "slist.h"

class WideStringClass;

//-----------------------------------------------------------------------------
class cGameChannelList
{
   public:
		 static void Add_Channel(cGameData * p_game_data, const RefPtr<WWOnline::ChannelData>& channel = RefPtr<WWOnline::ChannelData>());
		static void Remove_Channel(const WideStringClass & owner);
		static void Remove_All(void);
		static cGameChannel * Find_Channel(const WideStringClass & owner);
		static SList<cGameChannel> * Get_Chan_List(void) {return &ChanList;}

   private:
		static SList<cGameChannel> ChanList;
};	

//-----------------------------------------------------------------------------
#endif // GAMECHANLIST_H





