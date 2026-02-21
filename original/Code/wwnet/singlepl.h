//
// Filename:     singlepl.h
// Project:      wwnet
// Author:       Tom Spencer-Smith
// Date:         Nov 1998
// Description:  Single player stuff
//
// TODO:
// - merge into netutil ?
//
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef SINGLEPL_H
#define SINGLEPL_H

#include "bittype.h"
#include "slist.h"

enum {
	CLIENT_LIST = 0,
	SERVER_LIST
};

class cPacket;

//
// This is used in single-player mode and is global, not owned by the 
// C or S threads. A critical section is used to control access by 
// the C and S threads. The client writes to the end of the Server list and 
// reads from the beginning of the Client list. Vice versa for the server.
// Send_Packet handles everything transparently. 
// TSS - linked list
//

class cSinglePlayerData
{
	public:
		static void Init();
		static void Cleanup();

      static bool Is_Single_Player() {return IsSinglePlayer;}

		static SList<cPacket> * Get_Input_Packet_List(int type);

   private:
		static bool IsSinglePlayer;
		static SList<cPacket> InputPacketList[2];
};

//-----------------------------------------------------------------------------

#endif // SINGLEPL_H
  
