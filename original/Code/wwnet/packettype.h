#pragma once

#include "global.h"

//
// Filename:     packettype.h
// Project:      wwnet
// Author:       Tom Spencer-Smith
// Date:
// Description:
//
//-----------------------------------------------------------------------------


//
// Don't use these at application level
//
enum
{
   PACKETTYPE_FIRST,

	PACKETTYPE_UNRELIABLE = PACKETTYPE_FIRST,
   PACKETTYPE_RELIABLE,
   PACKETTYPE_ACK,
   PACKETTYPE_KEEPALIVE,
   PACKETTYPE_CONNECT_CS,
   PACKETTYPE_ACCEPT_SC,
   PACKETTYPE_REFUSAL_SC,
	PACKETTYPE_FIREWALL_PROBE,

   PACKETTYPE_LAST = PACKETTYPE_FIREWALL_PROBE,

	PACKETTYPE_COUNT
};

//-----------------------------------------------------------------------------
