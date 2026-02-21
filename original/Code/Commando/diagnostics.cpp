//
// Filename:     diagnostics.cpp
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:
//

#include "diagnostics.h"

#include <stdio.h>

#include "assets.h"
#include "font3d.h"
#include "render2d.h"

//
// Add any includes for exposed interfaces that you are diagnosing
//

#include "devoptions.h"
#include "cnetwork.h"
#include "gamedata.h"
#include "networkobjectmgr.h"
#include "serverfps.h"
#include "sbbomanager.h"
#include "singlepl.h"
#include "gameobjmanager.h"
#include "gamemode.h"
#include "humanphys.h"
#include "playermanager.h"
#include "useroptions.h"
#include "packetmgr.h"
#include "apppacketstats.h"
#include "connect.h"
#include "vehicle.h"
#include "csdamageevent.h"
#include "specialbuilds.h"
#include "consolemode.h"
#include "gametype.h"

static int RendererFps;
static int RendererSFps;
static int RendererPing;
static int RendererBandwidthBps;
static bool RendererGodStatus;
static bool RendererVipStatus;

//
// Class statics
//
Render2DTextClass *		cDiagnostics::PRenderer		= NULL;
Font3DInstanceClass *	cDiagnostics::PFont			= NULL;
float							cDiagnostics::DiagnosticX	= 0;
float							cDiagnostics::DiagnosticY	= 0;

//-----------------------------------------------------------------------------
void cDiagnostics::Init(void)
{
	if (!ConsoleBox.Is_Exclusive()) {
   	PFont = WW3DAssetManager::Get_Instance()->Get_Font3DInstance("FONT6x8.TGA");
		SET_REF_OWNER(PFont);
		PFont->Set_Mono_Spaced();

		PRenderer = new Render2DTextClass(PFont);

		RectClass rect = Render2DClass::Get_Screen_Resolution();
		PRenderer->Set_Coordinate_Range(rect);

		DiagnosticX = 10;
		//DiagnosticY = 10;

		RendererFps=0;
		RendererSFps=0;
		RendererPing = 0;
		RendererBandwidthBps = 0;
		RendererGodStatus=false;
		RendererVipStatus=false;
	}

}

//-----------------------------------------------------------------------------
void cDiagnostics::Close(void)
{
	if (PFont != NULL) {
		PFont->Release_Ref();
		PFont = NULL;
	}

	if (PRenderer != NULL) {
		delete PRenderer;
		PRenderer = NULL;
	}
}

//-----------------------------------------------------------------------------
void cDiagnostics::Show_Object_Tally(void)
{
	cAppPacketStats::Update_Object_Tally();

	Add_Diagnostic("");
	Add_Diagnostic("Object stats (reset with APTR command):");
	Add_Diagnostic(cAppPacketStats::Get_Heading());

	for (BYTE i = 0; i < APPPACKETTYPE_COUNT; i++)
	{
		Add_Diagnostic(cAppPacketStats::Get_Description(i));
	}
}

//-----------------------------------------------------------------------------
void cDiagnostics::Add_Diagnostic(LPCSTR format, ...)
{
	if (PRenderer) {
		va_list va;
		char buffer[1024];

		va_start(va, format);
		::vsprintf(buffer, format, va);

		PRenderer->Set_Location(Vector2(DiagnosticX, DiagnosticY));
		PRenderer->Draw_Text(buffer);

		DiagnosticY += (int)(PRenderer->Peek_Font()->Char_Height() * 1.2);
	}
}

//-----------------------------------------------------------------------------
void cDiagnostics::Render(void)
{
	if (PFont == NULL || PRenderer == NULL) {
		return;
	}

#ifdef BETACLIENT
	PRenderer->Reset();
	RectClass rect = Render2DClass::Get_Screen_Resolution();
	PRenderer->Set_Location(Vector2(rect.Left + 5, rect.Bottom - 10));
	PRenderer->Draw_Text("BETACLIENT");
	PRenderer->Render();
#endif // BETACLIENT

#ifdef FREEDEDICATEDSERVER
	PRenderer->Reset();
	RectClass rect = Render2DClass::Get_Screen_Resolution();
	PRenderer->Set_Location(Vector2(rect.Left + 5, rect.Bottom - 10));
	PRenderer->Draw_Text("FREEDEDICATEDSERVER");
	PRenderer->Render();
#endif // FREEDEDICATEDSERVER

#ifdef MULTIPLAYERDEMO
	PRenderer->Reset();
	RectClass rect = Render2DClass::Get_Screen_Resolution();
	PRenderer->Set_Location(Vector2(rect.Left + 5, rect.Bottom - 10));
	PRenderer->Draw_Text("MULTIPLAYER DEMO");
	PRenderer->Render();
#endif // MULTIPLAYERDEMO


	bool changed=false;

	if (cDevOptions::ShowFps.Is_True()) {
		int fps=cNetwork::Get_Fps();
		if (fps!=RendererFps) {
			RendererFps=fps;
			changed=true;
		}

		if (cNetwork::I_Am_Only_Client())
		{
			fps=cServerFps::Get_Instance()->Get_Fps();
			if (fps!=RendererSFps) {
				RendererSFps=fps;
				changed=true;
			}

			if (cNetwork::PClientConnection) {
				cRemoteHost *server = cNetwork::PClientConnection->Get_Remote_Host(0);
				if (server) {
					int ping = server->Get_Average_Internal_Pingtime_Ms();
					if (ping && ping != RendererPing) {
						RendererPing = ping;
						changed = true;
					}
					int bps = PacketManager.Get_Compressed_Bandwidth_In(&server->Get_Address());
					if (bps && bps != RendererBandwidthBps) {
						RendererBandwidthBps = bps;
						changed = true;
					}
				}
			}
		} else {
			int bps = PacketManager.Get_Total_Compressed_Bandwidth_Out();
			if (bps && bps != RendererBandwidthBps) {
				RendererBandwidthBps = bps;
				changed = true;
			}
		}
	}
	// If we displayed fps last frame, turn it off now
	else {
		if (RendererFps) {
			changed=true;
		}
		RendererFps=0;
	}

	if (cDevOptions::ShowFps.Is_True()) {
		// Stop the flicker
		changed = true;
	}
	// Render only if changed!
	if (!changed) {
		PRenderer->Render();
		return;
	}

	PRenderer->Reset();

	DiagnosticY = 75;

	if (cDevOptions::ShowFps.Is_True()) {
		StringClass fps_text;
		fps_text.Format("FPS = %3d", RendererFps);

		if (cNetwork::I_Am_Only_Client())
		{
			StringClass server_fps_text;
			server_fps_text.Format(", SFPS = %3d", RendererSFps);
			fps_text += server_fps_text;

			if (cNetwork::PClientConnection && cNetwork::PClientConnection->Get_Remote_Host(0)) {
				StringClass ping_time;
				ping_time.Format(", PING = %4d", min(9999, RendererPing));
				fps_text += ping_time;
			}

		}

		if (!IS_SOLOPLAY) {
			StringClass bps_text;
			bps_text.Format(", KBPS = %4d", RendererBandwidthBps / 1024);
			fps_text += bps_text;
		}

		float width = PFont->String_Width(fps_text);
		RectClass rect = Render2DClass::Get_Screen_Resolution();
		PRenderer->Set_Location(Vector2(rect.Right - width - 5, rect.Top + 2));
		PRenderer->Draw_Text(fps_text);
	}

	PRenderer->Render();

}
















/*
		Add_Diagnostic("PTheGameData:	%d", PTheGameData);
		if (PTheGameData != NULL) {
			Add_Diagnostic("ip addy:   %s", cNetUtil::Address_To_String(
				PTheGameData->Get_Ip_Address()));
			Add_Diagnostic("owner:     %s", PTheGameData->Get_Owner());
			Add_Diagnostic("map:       %s", PTheGameData->Get_Map_Name());

			if (PTheGameData->Is_Lms()) {
				Add_Diagnostic("reap:	   %5.2f", The_Lms_Game()->Get_Reaper_Countdown_Seconds());
			}
		}

	//
	// Toss all the defunct diagnostics down here
	//
		if (cDevOptions::ShowMenuStack.Is_True()) {
			Add_Diagnostic("MENU STACK:");
			for (int i = 0; i <= MenuManager::Get_Current_Depth(); i++) {
				//Add_Diagnostic("  %-30s		%d",
				//	MenuManager::Get_Menu_Stack_Name_Element(i),
				//	MenuManager::Get_Menu_Stack_Index_Element(i));
				Add_Diagnostic("  %-30s",
					MenuManager::Get_Menu_Stack_Name_Element(i));
			}
		}

		if (cNetwork::I_Am_Only_Client()) {
				Add_Diagnostic("CIM : %d", cUserOptions::ClientInterpolationModel.Get());
			}

			Add_Diagnostic("I_Am_Server:						%d", cNetwork::I_Am_Server());
			Add_Diagnostic("I_Am_Client:						%d", cNetwork::I_Am_Client());
			Add_Diagnostic("cNetwork::Get_Update_Count:	%d", cNetwork::Get_Update_Count());

		if (cNetwork::I_Am_Client()) {
			cPlayer * p_me = cNetwork::Get_My_Player_Object();
			if (p_me == NULL) {
				Add_Diagnostic("p_me == NULL");
			} else {
				Add_Diagnostic("p_me != NULL");
				if (p_me->Is_Living()) {
					Add_Diagnostic("p_me->Is_Living()");
				} else {
					Add_Diagnostic("! p_me->Is_Living()");
				}
				Add_Diagnostic("p_me->Get_Spectatee_Id() == %d", p_me->Get_Spectatee_Id());
			}
		}

		//Add_Diagnostic("Menu elements = %d", MenuManager::Get_Count());
		//Add_Diagnostic("Get_Current_Entry_Index = %d", MenuManager::Get_Current_Entry_Index());


		if (cDevOptions::ShowMenuStack.Is_True()) {
         Add_Diagnostic("MENU STACK:");
			for (int i = 0; i <= MenuManager::Get_Current_Depth(); i++) {
				//Add_Diagnostic("  %-30s		%d",
				//	MenuManager::Get_Menu_Stack_Name_Element(i),
				//	MenuManager::Get_Menu_Stack_Index_Element(i));
				Add_Diagnostic("  %-30s",
					MenuManager::Get_Menu_Stack_Name_Element(i));
			}
		}

         //Add_Diagnostic("Will_Be_Server:   %d", cNetwork::Will_Be_Server());
			//Add_Diagnostic("Will_Be_Client:   %d", cNetwork::Will_Be_Client());


			SLNode<BaseGameObj> * objnode;
			PhysicalGameObj * p_phys_obj;
			for (objnode = GameObjManager::Get_Game_Obj_List()->Head(); objnode; objnode = objnode->Next()) {
				p_phys_obj = objnode->Data()->As_PhysicalGameObj();
				if (p_phys_obj != NULL && p_phys_obj->As_SoldierGameObj() != NULL) {
					Add_Diagnostic("Soldier: ID %d, Type %d",
						p_phys_obj->Get_ID(),
						p_phys_obj->Get_Player_Type());
				}
			}


			cRemoteHost * p_rhost = NULL;
			if (cNetwork::I_Am_Client_Server()) {
				p_rhost = cNetwork::Get_Server_Rhost(2);
			} else if (cNetwork::I_Am_Only_Server()) {
				p_rhost = cNetwork::Get_Server_Rhost(1);
			} else if (cNetwork::I_Am_Client()) {
				p_rhost = cNetwork::Get_Client_Rhost();
			}

			if (p_rhost != NULL) {
				float sample_target_bits = cNetUtil::Get_Net_Stats_Sample_Time_Ms() / 1000.0f * p_rhost->Get_Target_Bps();
				Add_Diagnostic("TargetBps         : %d / %d",
					(int) sample_target_bits, p_rhost->Get_Target_Bps());
				Add_Diagnostic("STAT_BitsSent     : %-d",		p_rhost->Stats.StatSnapshot[STAT_BitsSent]);
			}

			if (cNetwork::I_Am_Server()) {
				for (int i = 0; i < 20; i++) {
					if (cNetwork::Get_Server_Rhost(i) != NULL) {
						Add_Diagnostic("Server rhost %d", i);
					}
				}
			}

         Add_Diagnostic("MENU STACK:");
			for (int i = 0; i <= MenuManager::Get_Current_Depth(); i++) {
				Add_Diagnostic("  %-30s		%d",
					MenuManager::Get_Menu_Stack_Name_Element(i),
					MenuManager::Get_Menu_Stack_Index_Element(i));
			}

			Add_Diagnostic("menu_direction:      %d", MenuManager::Get_Menu_Direction());

			Add_Diagnostic("SPG: %d", xxx);


			Add_Diagnostic("LEVEL : %s", SaveGameManager::Get_Level_Filename());

			if (cNetwork::I_Am_Server()) {
				Add_Diagnostic("Host 0 = %d", cNetwork::PServerConnection->Get_Remote_Host(0));
				Add_Diagnostic("Host 1 = %d", cNetwork::PServerConnection->Get_Remote_Host(1));
			}

			Add_Diagnostic("Duration:			 %d s", The_Game()->Get_Duration_Seconds());

			Add_Diagnostic("ServerQuickstart: %d", cUserOptions::ServerQuickstart.Get());
			Add_Diagnostic("ClientQuickstart: %d", cUserOptions::ClientQuickstart.Get());
			Add_Diagnostic("# teams = %d", cTeamManager::Count());
			Add_Diagnostic("Team: %d", cUserOptions::Get_Player_Type_Choice());
			Add_Diagnostic("Players: %d", cPlayerManager::Count());
			Add_Diagnostic("game channels:       %d", cGameChannelList::Get_Chan_List()->Get_Count());
			extern int menu_direction;
			Add_Diagnostic("menu_direction:      %d", MenuManager::Get_Menu_Direction());
			Add_Diagnostic("COMBAT ACTIVE:		 %d", GameModeManager::Find("Combat")->Is_Active());

			if (PLC != NULL) {
				Add_Diagnostic("Location:         %s",
					Translate_Location(PLC->Get_Current_Location()));
			} else if (PWC != NULL) {
				Add_Diagnostic("Location:         %s",
					Translate_Location(PWC->CurrentLocation));
			}

			Add_Diagnostic("ENTRIES:");
			int index = 0;
			MenuEntryClass * p_entry;
			do {
				p_entry = MenuManager::Find_Entry(index);
				if (p_entry != NULL) {
					Add_Diagnostic("%2d. %s (%d)",
						index, p_entry->Get_Name(), p_entry->Is_Cancel_Option());
				}
				index++;
			} while (p_entry != NULL);

			if (PWC != NULL) {
				SLNode<cNick> * objnode;
				for (objnode = PWC->Get_Banned_List()->Head(); objnode; objnode = objnode->Next()) {
					cNick * p_nick = objnode->Data();
					Add_Diagnostic("Banned : %s", p_nick->Nickname);
				}
			}
*/
















	/*
//#include "lcw.h"
	//
	// LCW - slow to compress, fast to decompress
	//
	char source[5000]; // datasize
	for (int i = 0; i < sizeof(source); i++) {
		source[i] = rand() % 256;
	}
	char dest[6000];   // >= datasize + datasize/128
	int retcode = LCW_Comp(source, dest, sizeof(source));
	Debug_Say(("Compressed down to %d bytes\n", retcode));
	*/


	/*
//#include "lzo.h"
	//
	// LZO - fast to compress, fast to decompress
	//
#define SOURCE_BUFFER_SIZE			5000
#define COMPRESSED_BUFFER_SIZE	LZO_BUFFER_SIZE(SOURCE_BUFFER_SIZE)

	char source[SOURCE_BUFFER_SIZE];
	char dest[COMPRESSED_BUFFER_SIZE];
	char source2[SOURCE_BUFFER_SIZE];

	int source_size = sizeof(source);

	//
	// Put some data in source
	//
	for (int i = 0; i < source_size; i++) {
		source[i] = rand() % 256;
	}

	//
	// Compress
	//
	int compressed_size;
	int lzo_code = LZOCompressor::Compress((const lzo_byte *)	source,
														(lzo_uint) 		  		source_size,
														(lzo_byte *) 			dest,
														(unsigned *)			&compressed_size);

	Debug_Say(("Compressed from %d to %d bytes\n",
		source_size, compressed_size));

	//
	// Decompress
	//
	int lzo_code_2;
	int decompressed_size;
	lzo_code_2 = LZOCompressor::Decompress((const lzo_byte*)		dest,
														(lzo_uint)				compressed_size,
														(lzo_byte*)				source2,
														(unsigned *)			&decompressed_size);

	Debug_Say(("Decompressed from %d to %d bytes\n",
		compressed_size, decompressed_size));

	//
	// Compare with original
	//
	*/


//void test(Vector3 v) {}
//const Vector3 white = Vector3(1, 1, 1);
//Vector3 test2(void) {return white;}


/*
#include "overlay.h"
#include "langmode.h"
#include "chatshre.h"
#include "useroptions.h"
#include "menu.h"
#include "playermanager.h"
#include "gamechanlist.h"
#include "menuentry.h"
#include "savegame.h"
#include "gameobjmanager.h"
#include "gametype.h"
#include "quat.h"
*/
//#include "nettgas.h"
			//sprintf(buffer, "SFPS %d", cNetwork::Get_Server_Fps());
		/*
		if (cNetwork::I_Am_Server() &&
			 GameModeManager::Find("Combat")->Is_Active() &&
			 The_Game() != NULL &&
			 The_Game()->As_Lms() != NULL) {

			Add_Diagnostic("ReaperCountdownSeconds = %d",
				(int) The_Game()->As_Lms()->Get_Reaper_Countdown_Seconds());
		}
		*/
//#include "gdlms.h"
		//Add_Diagnostic("#delobjects: %d", NetworkObjectMgrClass::Get_Pending_Object_Count());

			//Add_Diagnostic("CSC Last Ping:      %d ms", cClientPingManager::Get_Last_Round_Trip_Ping_Ms());
			//Add_Diagnostic("CSC Avg. Ping:      %d ms", cClientPingManager::Compute_Average_Round_Trip_Ping_Ms());
//#include "clientpingmanager.h"

		/*
		char buffer[20];
		RectClass rect;

		sprintf(buffer, "%d", RendererFps);
		rect = Render2DClass::Get_Screen_Resolution();
		PRenderer->Set_Location(Vector2(rect.Right - 25, rect.Top + 10));
		PRenderer->Draw_Text(buffer);

		if (cNetwork::I_Am_Only_Client())
		{
			sprintf(buffer, "SFPS %d", RendererSFps);
			rect = Render2DClass::Get_Screen_Resolution();
			PRenderer->Set_Location(Vector2(rect.Right - 70, rect.Top + 20));
			PRenderer->Draw_Text(buffer);
		}
		*/

		/*
		//
		// Strip the leading "APPPACKETTYPE_"
		//
		char name[200] = "";
		::strcpy(name, &cAppPacketStats::Interpret_Type(i)[14]);

		int tally = cAppPacketStats::Get_Object_Tally(i);

		char line[1000];
		if (tally > 0)
		{
			::sprintf(line, "%-3d %s", tally, name);
		}
		else
		{
			::sprintf(line, "    %s", name);
		}

		Add_Diagnostic(line);
		*/

/*
#ifdef BETASERVER
	PRenderer->Reset();
	RectClass rect = Render2DClass::Get_Screen_Resolution();
	PRenderer->Set_Location(Vector2(rect.Left + 5, rect.Bottom - 10));
	PRenderer->Draw_Text("BETASERVER");
	PRenderer->Render();
#endif // BETASERVER
*/

		//Add_Diagnostic("Is_Single_Player:   %d",		cSinglePlayerData::Is_Single_Player());