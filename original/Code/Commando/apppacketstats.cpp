#include "apppacketstats.h"

#include <memory.h>
#include <string.h>

#include "mathutil.h"
#include "networkobjectmgr.h"

//
// Class statics
//
DWORD				cAppPacketStats::PacketsSent[];
DWORD				cAppPacketStats::BitsSent[];
DWORD				cAppPacketStats::BitsSentTier[][PACKET_TIER_COUNT];
DWORD				cAppPacketStats::ObjectTally[];
StringClass		cAppPacketStats::WorkingString;

//-----------------------------------------------------------------------------
void
cAppPacketStats::Reset
(	
	void
)
{
	::memset(&PacketsSent,	0, sizeof(PacketsSent));
	::memset(&BitsSent,		0, sizeof(BitsSent));
	::memset(&BitsSentTier,	0, sizeof(BitsSentTier));
	::memset(&ObjectTally,	0, sizeof(ObjectTally));
}

//-----------------------------------------------------------------------------
void
cAppPacketStats::Dump_Diagnostics
(	
	void
)
{

	for (BYTE i = 0; i < APPPACKETTYPE_COUNT; i++)
	{
	}

}

//-----------------------------------------------------------------------------
void
cAppPacketStats::Increment_Packets_Sent
(	
	BYTE app_packet_type
)
{

	PacketsSent[app_packet_type]++;
	
	PacketsSent[APPPACKETTYPE_ALL]++;
}

//-----------------------------------------------------------------------------
void
cAppPacketStats::Increment_Bits_Sent
(	
	BYTE	app_packet_type,
	DWORD	bits
)
{

	BitsSent[app_packet_type] += bits;

	BitsSent[APPPACKETTYPE_ALL] += bits;
}

//-----------------------------------------------------------------------------
void
cAppPacketStats::Increment_Bits_Sent_Tier
(	
	BYTE					app_packet_type,
	PACKET_TIER_ENUM	tier,
	DWORD					bits
)
{

	BitsSentTier[app_packet_type][tier] += bits;

	BitsSentTier[APPPACKETTYPE_ALL][tier] += bits;
}

//-----------------------------------------------------------------------------
DWORD
cAppPacketStats::Get_Packets_Sent
(	
	BYTE app_packet_type
)
{

	return PacketsSent[app_packet_type];
}

//-----------------------------------------------------------------------------
DWORD
cAppPacketStats::Get_Bits_Sent
(	
	BYTE	app_packet_type
)
{

	return BitsSent[app_packet_type];
}

//-----------------------------------------------------------------------------
DWORD
cAppPacketStats::Get_Bits_Sent_Tier
(	
	BYTE					app_packet_type,
	PACKET_TIER_ENUM	tier
)
{

	return BitsSentTier[app_packet_type][tier];
}

//-----------------------------------------------------------------------------
#define ADD_CASE(exp)	case exp: return #exp;

LPCSTR
cAppPacketStats::Interpret_Type
(
	BYTE app_packet_type
)
{
   switch (app_packet_type) 
	{
		//
		// S->S
		//	
		ADD_CASE(APPPACKETTYPE_UNKNOWN);
		ADD_CASE(APPPACKETTYPE_SIMPLE);
		ADD_CASE(APPPACKETTYPE_SOLDIER);
		ADD_CASE(APPPACKETTYPE_VEHICLE);
		ADD_CASE(APPPACKETTYPE_TURRET);
		ADD_CASE(APPPACKETTYPE_BUILDING);
		ADD_CASE(APPPACKETTYPE_PLAYER);
		ADD_CASE(APPPACKETTYPE_TEAM);
		ADD_CASE(APPPACKETTYPE_GAMEOPTIONSEVENT);
		ADD_CASE(APPPACKETTYPE_PLAYERKILLEVENT);
		ADD_CASE(APPPACKETTYPE_PURCHASERESPONSEEVENT);
		ADD_CASE(APPPACKETTYPE_SCTEXTOBJ);
		ADD_CASE(APPPACKETTYPE_SVRGOODBYEEVENT);
		ADD_CASE(APPPACKETTYPE_WINEVENT);
		ADD_CASE(APPPACKETTYPE_POWERUP);
		ADD_CASE(APPPACKETTYPE_STATIC);
		ADD_CASE(APPPACKETTYPE_DOOR);
		ADD_CASE(APPPACKETTYPE_ELEVATOR);
		ADD_CASE(APPPACKETTYPE_DSAPO);
		ADD_CASE(APPPACKETTYPE_SERVERFPS);
		ADD_CASE(APPPACKETTYPE_CONSOLECOMMANDEVENT);
		ADD_CASE(APPPACKETTYPE_RESETWINSEVENT);
		ADD_CASE(APPPACKETTYPE_EVICTIONEVENT);
		ADD_CASE(APPPACKETTYPE_NETWEATHER);
		ADD_CASE(APPPACKETTYPE_GAMEDATAUPDATEEVENT);
		ADD_CASE(APPPACKETTYPE_SCPINGRESPONSEEVENT);
		ADD_CASE(APPPACKETTYPE_BASECONTROLLER);
		ADD_CASE(APPPACKETTYPE_CINEMATIC);
		ADD_CASE(APPPACKETTYPE_C4);
		ADD_CASE(APPPACKETTYPE_BEACON);
		ADD_CASE(APPPACKETTYPE_SCEXPLOSIONEVENT);
		ADD_CASE(APPPACKETTYPE_SCOBELISKEVENT);
		ADD_CASE(APPPACKETTYPE_SCANNOUNCEMENT);
		ADD_CASE(APPPACKETTYPE_NETBACKGROUND);

		//
		// C->S
		//	
		ADD_CASE(APPPACKETTYPE_CLIENTCONTROL);
		ADD_CASE(APPPACKETTYPE_CSTEXTOBJ);
		ADD_CASE(APPPACKETTYPE_SUICIDEEVENT);
		ADD_CASE(APPPACKETTYPE_CHANGETEAMEVENT);
		ADD_CASE(APPPACKETTYPE_MONEYEVENT);
		ADD_CASE(APPPACKETTYPE_WARPEVENT);
		ADD_CASE(APPPACKETTYPE_PURCHASEREQUESTEVENT);
		ADD_CASE(APPPACKETTYPE_CLIENTGOODBYEEVENT);
		ADD_CASE(APPPACKETTYPE_BIOEVENT);
		ADD_CASE(APPPACKETTYPE_LOADINGEVENT);
		ADD_CASE(APPPACKETTYPE_GODMODEEVENT);
		ADD_CASE(APPPACKETTYPE_VIPMODEEVENT);
		ADD_CASE(APPPACKETTYPE_SCOREEVENT);
		ADD_CASE(APPPACKETTYPE_CLIENTBBOEVENT);
		ADD_CASE(APPPACKETTYPE_CLIENTFPS);
		ADD_CASE(APPPACKETTYPE_CSPINGREQUESTEVENT);
		ADD_CASE(APPPACKETTYPE_CSDAMAGEEVENT);
		ADD_CASE(APPPACKETTYPE_REQUESTKILLEVENT);
		ADD_CASE(APPPACKETTYPE_CSCONSOLECOMMANDEVENT);
		ADD_CASE(APPPACKETTYPE_CSHINT);
		ADD_CASE(APPPACKETTYPE_CSANNOUNCEMENT);
		ADD_CASE(APPPACKETTYPE_DONATEEVENT);

		//
		// Summation
		//
		ADD_CASE(APPPACKETTYPE_ALL);

		default:
         break;
   }

	return "ERROR";
}

//-----------------------------------------------------------------------------
void
cAppPacketStats::Update_Object_Tally
(	
	void
)
{
	::memset(&ObjectTally, 0, sizeof(ObjectTally));

	int count = NetworkObjectMgrClass::Get_Object_Count();

	for (int index = 0; index < count; index ++) 
	{
		NetworkObjectClass * p_object = NetworkObjectMgrClass::Get_Object(index);

		if (p_object != NULL) 
		{
			BYTE type = p_object->Get_App_Packet_Type();

			ObjectTally[type]++;
			ObjectTally[APPPACKETTYPE_ALL]++;

			/*
			if (type == APPPACKETTYPE_UNKNOWN)
			{
			}
			*/
		}
	}
}

//-----------------------------------------------------------------------------
DWORD
cAppPacketStats::Get_Object_Tally
(	
	BYTE app_packet_type
)
{

	return ObjectTally[app_packet_type];
}

//-----------------------------------------------------------------------------
StringClass &
cAppPacketStats::Get_Heading
(	
	void
)
{
	//StringClass description;

	WorkingString.Format(
		"%-30s %-8s %-10s %-10s %-7s %-7s %-7s %-7s %-7s %-7s", 
		"Type",
		"Tally",
		"Packets",
		"Bytes",
		"PC",
		"Avg.",
		"PC TC",
		"PC TR",
		"PC TO",
		"PC TF"
		);

	return WorkingString;
}

//-----------------------------------------------------------------------------
StringClass &
cAppPacketStats::Get_Description
(	
	BYTE type
)
{

	float num_bytes = BitsSent[type] / 8.0f;
	DWORD average_bytes = 0;
	if (PacketsSent[type] > 0)
	{
		average_bytes = cMathUtil::Round(num_bytes / (float) PacketsSent[type]);
	}

	float percentage = 0;
	if (BitsSent[APPPACKETTYPE_ALL] > 0)
	{
		percentage = 100 * (BitsSent[type] / (float) BitsSent[APPPACKETTYPE_ALL]);
	}

	//
	// Strip the leading "APPPACKETTYPE_"
	//
	char name[200] = "";
	::strcpy(name, &Interpret_Type(type)[14]);

	//
	// Tier percentages
	//
	DWORD t0 = 0;
	DWORD t1 = 0;
	DWORD t2 = 0;
	DWORD t3 = 0;
	if (BitsSent[type] > 0)
	{
		float bits = BitsSent[type];

		t0 = cMathUtil::Round(100 * (BitsSentTier[type][0] / bits));
		t1 = cMathUtil::Round(100 * (BitsSentTier[type][1] / bits));
		t2 = cMathUtil::Round(100 * (BitsSentTier[type][2] / bits));
		t3 = cMathUtil::Round(100 * (BitsSentTier[type][3] / bits));
	}

	//StringClass description;
	WorkingString.Format(
		"%-30s %-8d %-10d %-10d %-7.1f %-7d %-7d %-7d %-7d %-7d", 
		name, 
		ObjectTally[type],
		PacketsSent[type],
		cMathUtil::Round(num_bytes), 
		percentage,
		average_bytes,
		t0, 
		t1, 
		t2, 
		t3
		);

	/**/
	//
	// Replace all solo zero's with a space.
	//
	char last = ' ';
	char next = ' ';
	char * p = WorkingString.Peek_Buffer();
	for (int i = 0; i < WorkingString.Get_Length(); i++)
	{
		if (i == 0)
		{
			last = ' ';
		}
		else 
		{
			last = p[i - 1];
		}

		if (i == WorkingString.Get_Length() - 1)
		{
			next = ' ';
		}
		else 
		{
			next = p[i + 1];
		}

		if (last == ' ' && next == ' ' && p[i] == '0') 
		{
			p[i] = ' ';
		}
	}
	/**/

	return WorkingString;
}

		//ADD_CASE(APPPACKETTYPE_FLAGCAPEVENT);
		//ADD_CASE(APPPACKETTYPE_STEALTHEVENT);
