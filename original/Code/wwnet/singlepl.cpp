//
// Filename:     singlepl.cpp
// Project:      wwnet
// Author:       Tom Spencer-Smith
// Date:         Nov 1998
// Description:  
//
//-----------------------------------------------------------------------------
#include "singlepl.h" // I WANNA BE FIRST!

#include "netutil.h"
#include "miscutil.h"
#include "wwdebug.h"
#include "wwpacket.h"

//
// class defines
//
bool cSinglePlayerData::IsSinglePlayer = false;
SList<cPacket> cSinglePlayerData::InputPacketList[];

//-----------------------------------------------------------------------------
void cSinglePlayerData::Init()
{
	WWDEBUG_SAY(("cSinglePlayerData::cSinglePlayerData\n"));

	IsSinglePlayer = true;
}

//------------------------------------------------------------------------------------
void cSinglePlayerData::Cleanup() 
{
	WWDEBUG_SAY(("cSinglePlayerData::~cSinglePlayerData\n"));

   SLNode<cPacket> * objnode;
   cPacket * p_packet;

   for (int list_type = 0; list_type < 2; list_type++) {
		for (objnode = InputPacketList[list_type].Head(); objnode != NULL;) {		
			p_packet = objnode->Data();
			WWASSERT(p_packet != NULL);                     
			objnode = objnode->Next();
			InputPacketList[list_type].Remove(p_packet);
			delete p_packet;
		}
	}

   IsSinglePlayer = false;
}

//-----------------------------------------------------------------------------
SList<cPacket> * cSinglePlayerData::Get_Input_Packet_List(int type)
{
	WWASSERT(type == CLIENT_LIST || type == SERVER_LIST);
	return &InputPacketList[type];
}





