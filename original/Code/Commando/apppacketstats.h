#ifndef __APPPACKETSTATS_H__
#define __APPPACKETSTATS_H__

#include "apppackettypes.h"
#include "networkobject.h"

//-----------------------------------------------------------------------------
//
// Record and report app packet stats
//
class	cAppPacketStats
{
public:
	static void				Reset(void);
	static void				Dump_Diagnostics(void);
	static LPCSTR			Interpret_Type(BYTE app_packet_type);
	static void				Update_Object_Tally(void);

	static void				Increment_Packets_Sent(BYTE app_packet_type);
	static void				Increment_Bits_Sent(BYTE app_packet_type, DWORD bits);
	static void				Increment_Bits_Sent_Tier(BYTE app_packet_type, PACKET_TIER_ENUM tier, DWORD bits);

	static DWORD			Get_Packets_Sent(BYTE app_packet_type);
	static DWORD			Get_Bits_Sent(BYTE app_packet_type);
	static DWORD			Get_Bits_Sent_Tier(BYTE app_packet_type, PACKET_TIER_ENUM tier);

	static DWORD			Get_Object_Tally(BYTE app_packet_type);

	static StringClass &	Get_Heading(void);
	static StringClass &	Get_Description(BYTE app_packet_type);

private:
	static DWORD			PacketsSent[APPPACKETTYPE_COUNT];
	static DWORD			BitsSent[APPPACKETTYPE_COUNT];
	static DWORD			BitsSentTier[APPPACKETTYPE_COUNT][PACKET_TIER_COUNT];
	static DWORD			ObjectTally[APPPACKETTYPE_COUNT];

	static StringClass	WorkingString;
};

//-----------------------------------------------------------------------------

#endif	// __APPPACKETSTATS_H__
