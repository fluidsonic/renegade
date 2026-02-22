#include "gameoptionsevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "gamemode.h"
#include "cnetwork.h"
#include "gametype.h"
#include "dialogbase.h"
#include "dialogmgr.h"
#include "dlgmpconnect.h"
#include "dialogresource.h"
#include "resource.h"
#include "apppackettypes.h"
#include "modpackagemgr.h"
#include "specialbuilds.h"
#include "dlgmpconnectionrefused.h"
#include "translatedb.h"
#include "string_ids.h"
#include <RealCRC.h>

DECLARE_NETWORKOBJECT_FACTORY(cGameOptionsEvent, NETCLASSID_GAMEOPTIONSEVENT);

//-----------------------------------------------------------------------------
cGameOptionsEvent::cGameOptionsEvent(void)
{
	Set_App_Packet_Type(APPPACKETTYPE_GAMEOPTIONSEVENT);

	HostedGameNumber = -1;
}

//-----------------------------------------------------------------------------
void
cGameOptionsEvent::Init(int client_id)
{

	HostedGameNumber = The_Game()->Get_Hosted_Game_Number();

	Set_Object_Dirty_Bit(client_id, BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
void
cGameOptionsEvent::Act(void)
{

	The_Game()->Set_Hosted_Game_Number(HostedGameNumber);

	if (!IS_SOLOPLAY) {
		DialogBaseClass* dialog = DialogMgrClass::Find_Dialog(IDD_MULTIPLAY_CONNECTING);

		if (dialog != NULL) {
	 		((DlgMPConnect*)dialog)->Connected(The_Game());
		}
	}
}

//-----------------------------------------------------------------------------
void
cGameOptionsEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	//GAMESPY
	//
	// Due to gamespy support, we can no longer assume that the client exe
	// knows about tier 1 options, so transmit them.
	//
	The_Game()->Export_Tier_1_Data((cPacket &) packet);

	The_Game()->Export_Tier_2_Data((cPacket &) packet);
   packet.Add(The_Game()->Get_Time_Remaining_Seconds());

	packet.Add(HostedGameNumber);

#ifndef MULTIPLAYERDEMO
	packet.Add((uint32)CRC_Stringi(The_Game()->Get_Mod_Name()));
	packet.Add((uint32)CRC_Stringi(The_Game()->Get_Map_Name()));
#endif // MULTIPLAYERDEMO

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cGameOptionsEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	The_Game()->Import_Tier_1_Data((cPacket &) packet);

	The_Game()->Import_Tier_2_Data((cPacket &) packet);
   float time_remaining_seconds = packet.Get(time_remaining_seconds);
   The_Game()->Set_Time_Remaining_Seconds(time_remaining_seconds);

	packet.Get(HostedGameNumber);
	bool act = true;

#ifndef MULTIPLAYERDEMO
	//
	// TSS103001...n.b. need test that Find_Map_Name succeeds...
	//
	uint32_t mod_name_crc = packet.Get(mod_name_crc);
	uint32_t map_name_crc = packet.Get(map_name_crc);

	// Find the mod and map names from their CRC
	StringClass mod_name(0, true);
	StringClass map_name(0, true);
	ModPackageMgrClass::Get_Mod_Map_Name_From_CRC (mod_name_crc, map_name_crc, &mod_name, &map_name);
	The_Game()->Set_Mod_Name(mod_name);
	The_Game()->Set_Map_Name(map_name);

	if (!IS_SOLOPLAY) {
		if (!The_Game()->Is_Map_Valid()) {
			DialogBaseClass* dialog = DialogMgrClass::Find_Dialog(IDD_MULTIPLAY_CONNECTING);

			if (dialog != NULL) {
	 			((DlgMPConnect*)dialog)->Failed_To_Connect();
				act = false;
			}
			WideStringClass tval;
			tval.Format(u"%s  %s", TRANSLATE(IDS_MP_CONNECTION_REFUSED_BY_APPLICATION),
				TRANSLATE(IDS_MENU_MISSING_MAP));
			DlgMPConnectionRefused::DoDialog(tval, false);
		}
	}

#endif // MULTIPLAYERDEMO

	if (act) {
		Act();
	}

	Set_Delete_Pending();
}

