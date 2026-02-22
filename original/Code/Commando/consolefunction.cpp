#include "global.h"
#include "consolefunction.h"
#include "console.h"
#include "textdisplay.h"
#include "combatgmode.h"
#include "combat.h"
#include "datasafe.h"
#include "pscene.h"
#include "playermanager.h"
#include "ccamera.h"
#include "level.h"
#include "weaponmanager.h"
#include "cnetwork.h"
#include "ww3d.h"
#include "miscutil.h"
#include "smartgameobj.h"
#include "weapons.h"
#include "WWAudio.H"
//#include "gamesettings.h"
#include "waypoint.h"
#include "action.h"
#include "gameobjmanager.h"
#include "surfaceeffects.h"
#include "gamedata.h"
#include "damage.h"
#include "radar.h"
#include "assets.h"
#include "animobj.h"
#include "matpass.h"
#include "objlibrary.h"
#include "useroptions.h"
#include "devoptions.h"
#include "god.h"
#include "playertype.h"
#include "systemsettings.h"
#include "spawn.h"
#include "input.h"
#include "pathfind.h"
#include "waypath.h"
#include "definitionclassids.h"
#include "netinterface.h"
#include "bandwidth.h"
#include "statistics.h"
#include "physaabtreecull.h"
#include "gametype.h"
#include "part_emt.h"
#include "translatedb.h"
#include "string_ids.h"
#include "dx8renderer.h"
#include "backgroundmgr.h"
#include "dynamicphys.h"
#include "humanphys.h"
#include "vehiclephys.h"
#include "objectives.h"
#include "umbrasupport.h"
#include "vehicle.h"
#include "damageablestaticphys.h"
#include "persistfactory.h"
#include "wwphysids.h"
#include "hud.h"
#include "conversationmgr.h"
#include "basecontroller.h"
#include "building.h"
#include "vendor.h"
#include "texture.h"
#include "rddesc.h"
#include "combatchunkid.h"
#include "dialogtests.h"
#include "resource.h"
#include "dx8wrapper.h"
#include "sortingrenderer.h"
#include "weathermgr.h"
#include "mapmgr.h"
#include "path.h"
#include "sctextobj.h"
#include "consolecommandevent.h"
#include "hudinfo.h"
#include "physresourcemgr.h"
#include "cstextobj.h"
#include "suicideevent.h"
#include "godmodeevent.h"
#include "serverfps.h"
#include "warpevent.h"
//#include "helptext.h"
#include "ffactory.h"
#include "dazzle.h"
#include "renegadedialogmgr.h"
#include "moneyevent.h"
#include "scoreevent.h"
#include "dlgmpingamechat.h"
#include "playerterminal.h"
#include "encyclopediamgr.h"
#include "clientbboevent.h"
#include "wheelvehicle.h"
#include "trackedvehicle.h"
#include "packetmgr.h"
#include "requestkillevent.h"
#include "csconsolecommandevent.h"
#include "apppacketstats.h"
#include "bandwidthgraph.h"
#include "scexplosionevent.h"
#include "bwbalance.h"
#include "sbbomanager.h"
#include "_globals.h"
#include "registry.h"
#include "vipmodeevent.h"
#include "dx8rendererdebugger.h"
#include "changeteamevent.h"
#include "gamesideservercontrol.h"
#include "except.h"
#include "donateevent.h"
#include "consolemode.h"
#include "dlgcncpurchasemainmenu.h"
#include "realcrc.h"
#include "lightsolve.h"
#include "lightsolvecontext.h"

void	ConsoleFunctionClass::Print( const char *format, ... )
{
	va_list arg_list;
	va_start (arg_list, format);
	StringClass string;
	string.Format_Args( format, arg_list );
	if (Get_Text_Display()) {
		Get_Text_Display()->Print_System( string );
	}
	ConsoleBox.Print(string.Peek_Buffer());
	va_end (arg_list);
}

//----------------------------------------------------------------------------
//
// DEVELOPMENT CONSOLE FUNCTIONS ONLY
// Only present in Debug and Profile builds.
//
//----------------------------------------------------------------------------

class ScreenUVBiasConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "screen_uv_bias"; }
	//virtual	const char * Get_Alias( void )	{ return "sub"; }
	virtual	const char * Get_Help( void )	{ return "SCREEN_UV_BIAS - toggles the half pixel bias in screen text."; }
	virtual	void Activate( const char * input ) {
		WW3D::Set_Screen_UV_Bias( !WW3D::Is_Screen_UV_Biased() );
      Print( "Screen UV Bias Switched\n" );
	}
};

class SetBandwidthBudgetOutConsoleFunctionClass : public ConsoleFunctionClass {
public:
   virtual	const char * Get_Name( void )		{ return "set_bw_budget_out"; }
	virtual	const char * Get_Alias( void )	{ return "sbbo"; }
	virtual	const char * Get_Help( void )		{ return "SET_BW_BUDGET_OUT <bps> - set total bps budget out."; }

	virtual	void Activate( const char * input ) {
      uint32_t bbo = (uint32_t) ::atol(input);
		if (bbo > 0) {

      	if (cNetwork::I_Am_Server()) {
				cNetwork::PServerConnection->Set_Bandwidth_Budget_Out(bbo);
			}

      	if (cNetwork::I_Am_Client()) {
				cNetwork::PClientConnection->Set_Bandwidth_Budget_Out(bbo);

				//
				// Notify the server
				//
				cClientBboEvent * p_event = new cClientBboEvent;
				p_event->Init(bbo);
			}

			int bw_scale = (bbo * 2) / 10;
			bw_scale = (bw_scale / 1000) * 1000;
			cBandwidthGraph::Set_Scale(bw_scale);

		   if (cNetwork::PClientConnection != NULL) {
			   cNetwork::PClientConnection->Init_Stats();
			   Print("Network stats for client connection reset.\n");
		   }
		   if (cNetwork::PServerConnection != NULL) {
			   cNetwork::PServerConnection->Init_Stats();
			   Print("Network stats for server connection reset.\n");
		   }
			//ConsoleFunctionManager::Parse_Input("clear ns"); // reset stats
		}
	}
};

class GameOverConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "gameover"; }
	virtual	const char * Get_Help( void )	{ return "GAMEOVER - end current game (server only)."; }
	virtual	void Activate( const char * input ) {

		bool print_help = false;
      if (IS_MISSION) {
			//
			// Fail the mission
			//
			print_help = true;
		} else if (!cNetwork::I_Am_Server()) {
			print_help = true;
      } else {
			if (cNetwork::I_Am_Only_Server()) {
         	Print("Terminating game on demand...\n");
         	cGameData::Set_Manual_Restart(true);
			} else {
				print_help = true;
			}
      }
		if (print_help) {
			Print("GAMEOVER is for dedicated server only\n");
		}
	}
};

class ToggleSortingConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "toggle_sorting"; }
	virtual	const char * Get_Help( void )	{ return "TOGGLE_SORTING - toggles WW3D sorting."; }
	virtual	void Activate( const char * input ) {
		WW3D::Enable_Sorting(!WW3D::Is_Sorting_Enabled());
      Print( WW3D::Is_Sorting_Enabled() ? "Sorting Enabled\n" : "Sorting Disabled\n" );
	}
};

class ExtrasConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "extras"; }
	virtual	const char * Get_Help( void )	{ return "EXTRAS <key>"; }
	virtual	void Activate( const char * input ) {

		// (gth) secrets don't need a password any more...
		if (The_Game()->IsLaddered.Is_True()) {

			CNCPurchaseMainMenuClass::Enable_Secrets(false);
			Print("Not allowed in laddered games\r\n");

		} else {

			CNCPurchaseMainMenuClass::Enable_Secrets(true);
			if (CNCPurchaseMainMenuClass::Are_Secrets_Enabled()) {
				Print("extras ENABLED!\r\n");
			}
		}
	}
};

class DebugDeviceConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "debug_device"; }
   const char * Get_Alias( void ) { return "dd"; }
	virtual	const char * Get_Help( void )	{ return
		"DEBUG_DEVICE [device|on|off] - toggles the debug device (screen, mono, dbwin32, log, windows)"; }
	virtual	void Activate( const char * input ) {
		char str[128];
		sprintf(str,"ERROR (%s)\n", input );
		if (stricmp(input,"screen") == 0) {
			sprintf(str, "Screen Debug %s\n", false ? "Enabled" : "Disabled" );
		} else if (stricmp(input,"mono") == 0) {
			sprintf(str, "Mono Debug %s\n", false ? "Enabled" : "Disabled" );
		} else if (stricmp(input,"dbwin32") == 0) {
			sprintf(str, "DBWin32 Debug %s\n", false ? "Enabled" : "Disabled" );
		} else if (stricmp(input,"log") == 0) {
			sprintf(str, "Log File Debug %s\n", false ? "Enabled" : "Disabled" );
		} else if (stricmp(input,"windows") == 0) {
			sprintf(str, "Windows Debug %s\n", false ? "Enabled" : "Disabled" );
		} else if (stricmp(input,"on") == 0) {
			sprintf(str, "All Debug Devices Enabled\n" );
		} else if (stricmp(input,"off") == 0) {
			sprintf(str, "All Debug Devices Disabled\n" );
		}
		Print(str);
		Debug_Say((str));
	}
};

class AdminMessageConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "admin_message"; }
   const char * Get_Alias( void ) { return "amsg"; }
	virtual	const char * Get_Help( void )	{ return
		"ADMIN_MESSAGE <message> - sends an admin message to all clients. Host only."; }
	virtual	void Activate( const char * input ) {
		if (cNetwork::I_Am_Server() && ::strlen(input) > 0) {
			WideStringClass widestring;
			widestring.Convert_From(input);
			cScTextObj * p_message = new cScTextObj;
			p_message->Init(widestring, TEXT_MESSAGE_PUBLIC, true, HOST_TEXT_SENDER, -1);
		} else {
			Print(Get_Help());
		}
	}
};

class MessageConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "message"; }
   const char * Get_Alias( void ) { return "msg"; }
	virtual	const char * Get_Help( void )	{ return "MESSAGE <message> - sends a chat message to all clients. Host only."; }
	virtual	void Activate( const char * input ) {
		if (cNetwork::I_Am_Server() && ::strlen(input) > 0) {
			WideStringClass widestring(input, true);
			cScTextObj *event_obj = new cScTextObj;
			event_obj->Init(widestring, TEXT_MESSAGE_PUBLIC, false, HOST_TEXT_SENDER, -1);
		} else {
			Print(Get_Help());
		}
	}
};

class FPSConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "fps"; }
   const char * Get_Alias( void ) { return "fps"; }
	virtual	const char * Get_Help( void )	{ return "FPS - toggle FPS display."; }
	virtual	void Activate( const char * ) {
//		if (!cNetwork::I_Am_Server()) {
         cDevOptions::ShowFps.Toggle();
//		}
	}
};

class ClientPhysicsOptimizationConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "client_physics_optimization"; }
	const char * Get_Alias( void ) { return "cpo"; }
	virtual	const char * Get_Help( void )	{ return "CLIENT_PHYSICS_OPTIMIZATION [0|1] - Update only visible physic objects on client."; }
	virtual	void Activate( const char * input) {
		if (!cNetwork::I_Am_Server()) {
			int state = 0;
			int argcount=::sscanf(input, "%d", &state);
			if (argcount==1) {
				state=!!state;
			}
			else {
				state=!COMBAT_SCENE->Get_Update_Only_Visible_Objects();
			}
         COMBAT_SCENE->Set_Update_Only_Visible_Objects(state==1);

			Print( "Client physics optimization %s\n", state ? "ENABLED" : "DISABLED");
		}
	}
};

class StatsConsoleFunctionClass : public ConsoleFunctionClass
{
public:
	virtual	const char * Get_Name( void )	{ return "stats"; }
	virtual	const char * Get_Help( void )	{ return "STATS <off|fps|dx8|star|sr_history|sr|gerd|collision|culling|physics|vehicle|ai|wol|profile|memory> - changes stats display."; }
	virtual	void Activate( const char * input ) {
		StatisticsDisplayManager::Set_Display( input );
	}
};

class ResolutionConsoleFunctionClass : public ConsoleFunctionClass
{
public:
	virtual	const char * Get_Name( void )	{ return "resolution"; }
	virtual	const char * Get_Help( void )	{ return "RESOLUTION [+|-]"; }
	virtual	void Activate( const char * input ) {
		int w,h,bits;
		bool windowed;
		WW3D::Get_Device_Resolution(w,h,bits,windowed);
		const RenderDeviceDescClass& desc=WW3D::Get_Render_Device_Desc();
		const DynamicVectorClass<ResolutionDescClass> & resos=desc.Enumerate_Resolutions();
		int i;
		for (i=0;i<resos.Count();++i) {
			if (resos[i].Width==w && resos[i].Height==h && resos[i].BitDepth==bits) {
				break;
			}
		}

		unsigned char sign;
		if (sscanf (input, "%c", &sign) == 1 && (sign=='+' || sign=='-')) {

			if (sign=='+') i++;
			else i--;

			if (i>=resos.Count()) i=0;
			else if (i<0) i=resos.Count()-1;

			WW3DErrorType error=WW3D::Set_Device_Resolution(
				resos[i].Width,
				resos[i].Height,
				resos[i].BitDepth,
				-1,
				true );
			if (error==WW3D_ERROR_OK) {
				RectClass rect;
				rect.Left = 0;
				rect.Top = 0;
				rect.Right = resos[i].Width;
				rect.Bottom = resos[i].Height;

				Render2DClass::Set_Screen_Resolution(rect);
				Print( "Resolution changed to %d * %d, %d bits\n", resos[i].Width,resos[i].Height,resos[i].BitDepth);
			}
			else {
				Print( "Failed to set resolution to %d * %d, %d bits\n", resos[i].Width,resos[i].Height,resos[i].BitDepth);
				error=WW3D::Set_Device_Resolution(
					w,
					h,
					bits,
					-1,
					true );
			}
		}
		else {
			Print("Resolution is %d * %d, %d bits\n",w,h,bits);
		}
	}
};

class ToggleSurfaceEffectsConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "toggle_surface_effects"; }
	virtual	const char * Get_Help( void )	{ return "TOGGLE_SURFACE_EFFECTS - toggles surface effects."; }
	virtual	void Activate( const char * input ) {
		SurfaceEffectsManager::Enable_Effects(!SurfaceEffectsManager::Are_Effects_Enabled());
      Print( SurfaceEffectsManager::Are_Effects_Enabled() ? "Surface Effects Enabled\n" : "Surface Effects Disabled\n" );
	}
};

//----------------------------------------------------------------------------
//
// SHIPPING CONSOLE FUNCTIONS ONLY
// THESE MUST BE DEBUGGED!  THEY ARE PRESENT IN RELEASE BUILDS!!!
//
//----------------------------------------------------------------------------

class DisplayFindpathConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "display_findpath"; }
	virtual	const char * Get_Help( void )	{ return "DISPLAY_FINDPATH - Toggles the display of findpath lines."; }
	virtual	void Activate( const char * input ) {
		Toggle_Display_Findpaths();
	}
};

class DonateConsoleFunctionClass : public ConsoleFunctionClass {
	StringClass HelpText;
public:
	virtual const char * Get_Name(void) {return "donate";}
	virtual const char * Get_Help(void) {
		HelpText.Format("DONATE <credits> <teammate> - donate money (minimum %d credits) to teammate.",
			cDonateEvent::Get_Minimum_Acceptable_Donation());
		return HelpText.Peek_Buffer();;
	}

	virtual void Activate(const char * input) {

		int amount = 0;
		char recipient[1000];
		::sscanf(input, "%d %s", &amount, recipient);

		cPlayer * p_me = cNetwork::Get_My_Player_Object();
		cPlayer * p_recipient = cPlayerManager::Find_Player(recipient);

		if (p_me != NULL &&
			 p_recipient != NULL &&
			 amount >= cDonateEvent::Get_Minimum_Acceptable_Donation() &&
			(p_me->Get_Player_Type() == p_recipient->Get_Player_Type()) &&
			 p_me->Get_Money() >= amount) {

			cDonateEvent * p_event = new cDonateEvent;
			p_event->Init(amount, p_recipient->Get_Id());

		} else {
		   Print(Get_Help());
		}
	}
};

class DoStuffConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "do_stuff"; }
	virtual	const char * Get_Help( void )	{ return "DO_STUFF - Execute some task..."; }
	virtual	void Activate( const char * input ) {

		//
		// This command is for executing some test... put whatever code
		// you want here...
		//

		/*
		int recipient_id = atoi(input);

		WideStringClass msg;
		msg.Format(u"Hola amigo!");

		if (cNetwork::I_Am_Client()) {
			cCsTextObj * p_message = new cCsTextObj;
			p_message->Init(msg, TEXT_MESSAGE_PRIVATE, cNetwork::Get_My_Id(), recipient_id);
		} else {
			cScTextObj * p_message = new cScTextObj;
			p_message->Init(msg, TEXT_MESSAGE_PRIVATE, HOST_TEXT_SENDER, recipient_id);
		}
		*/

		//::exit(1);

		//::Sleep(::atoi(input));
/*
*/

		cPlayer * p_player = cPlayerManager::Find_Player(input);
		if (p_player != NULL) {
			int id = p_player->Get_Id();
			cNetwork::Server_Kill_Connection(id);
			cNetwork::Cleanup_After_Client(id);
		}
	}
};

//
// REQUIRED for shipping dedicated server.
//
class QuitConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "quit"; }
	virtual	const char * Get_Help( void )	{
		return ("QUIT - End game and quit to desktop (dedicated server only).\n");
	}
	virtual	void Activate( const char * input ) {

		if (!IS_MISSION) {
			if (cNetwork::I_Am_Server()) {
				if (The_Game() && The_Game()->IsDedicated.Is_True()) {
         			Print("Terminating game on demand...\n");
					Set_Exit_On_Exception(true);
         			cGameData::Set_Manual_Exit(true);
				} else {
         			Print("QUIT is for dedicated server only.\n");
				}
			} else {
				/*
				** I_Am_Server may not report true if we just started up and we aren't a server yet.
				*/
				if (ConsoleBox.Is_Exclusive()) {
					Set_Exit_On_Exception(true);
         			cGameData::Set_Manual_Exit(true);
				} else {
         			Print("QUIT is for dedicated server only.\n");
				}
			}
      }
	}
};

class NetUpdateRateConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "net_update_rate"; }
	virtual	const char * Get_Alias( void ){ return "nur"; }
	virtual	const char * Get_Help( void )	{ return "NET_UPDATE_RATE - set the max. net update think rate (times per second)."; }
	virtual	void Activate( const char * input ) {
		int rate = ::atoi(input);
		if (rate >= 5 && rate <= 30) {
			cUserOptions::NetUpdateRate.Set(rate);
         Print( "NetUpdateRate set to %d.", rate);
		} else {
		   Print(Get_Help());
		}
	}
};

class GameInfoConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "game_info"; }
	virtual	const char * Get_Help( void )	{ return "GAME_INFO - Print info about a game in progress to console box"; }
	virtual	void Activate(const char *) {

		bool active = false;

		if (cNetwork::I_Am_Server()) {

			if (The_Game() && The_Game()->IsDedicated.Is_True()) {

				char upstring[256] = "?";
				char timestr[256] = "?";
				FILETIME creation;
				FILETIME exit;
				FILETIME kernel;
				FILETIME user;
				int ok = GetProcessTimes(GetCurrentProcess(), &creation, &exit, &kernel, &user);
				if (ok) {
					FILETIME local;
					if (FileTimeToLocalFileTime(&creation, &local)) {
						SYSTEMTIME time;
						if (FileTimeToSystemTime(&local, &time)) {
							GetDateFormat(LOCALE_SYSTEM_DEFAULT, 0, &time, NULL, upstring, 256);
							GetTimeFormat(LOCALE_SYSTEM_DEFAULT, TIME_FORCE24HOURFORMAT, &time, NULL, timestr, 256);
							strcat(upstring, " - ");
							strcat(upstring, timestr);
						}
					}
				}

				GameModeClass* game = GameModeManager::Find("Combat");
				if (game && game->Is_Active()) {
					game = GameModeManager::Find("LAN");
					if (game && game->Is_Active()) {
						ConsoleBox.Print("LAN mode active since %s\n", upstring);
						active = true;
					}
				}

				if (active && The_Game()) {

					unsigned long time = static_cast<uint32_t>(The_Game()->Get_Time_Remaining_Seconds());
					unsigned long seconds = time % 60;
					unsigned long minutes = (time / 60) % 60;
					unsigned long hours = (time / (60*60));

					if (The_Game()->Is_Gameplay_Permitted()) {
						ConsoleBox.Print("Gameplay in progress\n");
					} else {
						ConsoleBox.Print("Gameplay Pending\n");
					}

					char buf[256];
					sprintf(buf, "     Map : %s\n", The_Game()->Get_Map_Name().Peek_Buffer());
					ConsoleBox.Print(buf);

					sprintf(buf, "    Time : %d.%02d.%02d\n", hours, minutes, seconds);
					ConsoleBox.Print(buf);

					sprintf(buf, "     Fps : %d\n", cNetwork::Get_Fps());
					ConsoleBox.Print(buf);

					sprintf(buf, "     GDI : %d/%d players      %d points\n",
						cPlayerManager::Tally_Team_Size(PLAYERTYPE_GDI),
						The_Game()->Get_Max_Players(),
						(int)cTeamManager::Find_Team(PLAYERTYPE_GDI)->Get_Score());
					ConsoleBox.Print(buf);

					sprintf(buf, "     NOD : %d/%d players      %d points\n",
						cPlayerManager::Tally_Team_Size(PLAYERTYPE_NOD),
						The_Game()->Get_Max_Players(),
						(int)cTeamManager::Find_Team(PLAYERTYPE_NOD)->Get_Score());
					ConsoleBox.Print(buf);

					ConsoleBox.Print("\n");
				}
			}
		}
	}
};

class PlayerInfoConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "player_info"; }
	virtual	const char * Get_Help( void )	{ return "PLAYER_INFO - Print info about players in the game to the console box"; }
	virtual	void Activate(const char *) {

		if (cNetwork::I_Am_Server() && The_Game() && The_Game()->IsDedicated.Is_True() && cNetwork::PServerConnection != NULL) {

			unsigned long time = TIMEGETTIME();
			unsigned long bw = 0;

			if (cNetwork::PServerConnection->Get_Num_RHosts() == 0) {
				ConsoleBox.Print("No players\n");
			} else {

				ConsoleBox.Print("\rId  Name           Score Side\tPing\tAddress               Kbits/s Time\n\n");

				for (int i=cNetwork::PServerConnection->Get_Min_RHost() ; i <= cNetwork::PServerConnection->Get_Max_RHost() ; i++) {

					cRemoteHost *client = cNetwork::PServerConnection->Get_Remote_Host(i);

					if (client) {
      				cPlayer * playerptr = cPlayerManager::Find_Player(i);

						if (playerptr) {
							WideStringClass name(playerptr->Get_Name(), true);
							StringClass short_name;
							name.Convert_To(short_name);
							int len = name.Get_Length();
							char temp_name[256];
							strcpy(temp_name, short_name.Peek_Buffer());
							strncat(temp_name, "                    ", 14-len);

							unsigned long dur = (time - client->Get_Creation_Time()) / 1000;

							unsigned long seconds = dur % 60;
							unsigned long minutes = (dur / 60) % 60;
							unsigned long hours = (dur / (60*60));

							bw += PacketManager.Get_Compressed_Bandwidth_Out(&client->Get_Address());

							char addr_string[128];
							sockaddr_in *addr = &client->Get_Address();
							{
							unsigned char *_ip = (unsigned char*)&addr->sin_addr.s_addr;
							sprintf(addr_string, "%d.%d.%d.%d;%d",
								(int)_ip[0], (int)_ip[1], (int)_ip[2], (int)_ip[3],
								(unsigned int)ntohs(addr->sin_port));
							}
							int addr_string_len = static_cast<int32_t>(strlen(addr_string));
							char local_addr_string[128];
							strcpy(local_addr_string, addr_string);
							if (addr_string_len < 21) {
								strncat(local_addr_string, "                         ", 21-addr_string_len);
							}

							Vector3 color = playerptr->Get_Color();
							char buffer[256];
							sprintf(buffer, "%3d %s%- 6d %s\t%d\t%s%- 7d  %03d.%02d.%02d\n", i, temp_name,
																								(int)playerptr->Get_Score(),
																								(playerptr->Get_Player_Type() == PLAYERTYPE_NOD) ? "NOD" : "GDI",
																								(int)client->Get_Average_Internal_Pingtime_Ms(),
																								local_addr_string,
																								(int)(PacketManager.Get_Compressed_Bandwidth_Out(&client->Get_Address()) / 1024),
																								hours, minutes, seconds);

							WideStringClass ws(buffer, true);
							ConsoleBox.Add_Message(&ws, &color, true);
						}
					}
				}
				ConsoleBox.Print("Total current bandwidth usage for players is %d kilobits per second\n", bw / 1024);
			}
   	} else {
			Print("player_info is for dedicated server only\n");
		}
	}
};

class KickConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "kick"; }
	virtual	const char * Get_Help( void )	{ return "KICK [<Nickname>|<Id>] - Kick a user from the game."; }
	virtual	void Activate( const char * input ) {
		// GameSpy kick removed; LAN kick not implemented.
		(void)input;
	}
};

class BanConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "ban"; }
	virtual	const char * Get_Help( void )	{ return "BAN [<Nickname>|<Id>] - Permanently ban a user from this server (LAN mode only, not implemented)."; }
	virtual	void Activate( const char * input ) {
		// GameSpy ban removed; LAN ban not implemented.
		(void)input;
	}
};

class AllowConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "allow"; }
	virtual	const char * Get_Help( void )	{ return "ALLOW [<Nickname>|<Id>] - Remove user channel ban from this server (WOL mode only)."; }
	virtual	void Activate( const char * input ) {
		// WOL removed - allow/unban via WOL session not available
		(void)input;
	}
};

class PageConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "page"; }
	virtual	const char * Get_Help( void )	{ return "PAGE Nickname Message - Page a Westwood Online user."; }
	virtual	void Activate( const char * input ) {
		// WOL removed - page user not available
		(void)input;
	}
};

//
// REQUIRED for shipping dedicated server.
//
class RestartConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "restart"; }
	virtual	const char * Get_Help( void )	{ return "RESTART - Quit to desktop and restart process (dedicated server only)."; }
	virtual	void Activate( const char * input ) {

		if (!IS_MISSION) {
			if (cNetwork::I_Am_Server()) {
				if (The_Game() && The_Game()->IsDedicated.Is_True()) {
         			Print("Restarting game on demand...\n");
					Set_Exit_On_Exception(true);
         			cGameData::Set_Manual_Exit(true);
				} else {
         			Print("RESTART is for dedicated server only.\n");
				}
			} else {
         		Print("RESTART is for dedicated server only.\n");
			}
      }
	}
};

class LogMeshStatsConsoleFunctionClass : public ConsoleFunctionClass
{
public:
	virtual	const char * Get_Name( void )	{ return "log_mesh_stats"; }
	virtual	const char * Get_Help( void )	{ return "LOG_MESH_STATS - log stats to debug window or file."; }
	virtual	void Activate( const char * input ) {
		TheDX8MeshRenderer.Request_Log_Statistics();
	}
};

class LogTexturesConsoleFunctionClass : public ConsoleFunctionClass
{
public:
	virtual	const char * Get_Name( void )	{ return "log_textures"; }
	virtual	const char * Get_Help( void )	{ return "LOG_TEXTURES - log texture statistics to debug window or file."; }
	virtual	void Activate( const char * input ) {
		WW3DAssetManager::Get_Instance()->Log_All_Textures();
	}
};

class DeviceInfoConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "device_info"; }
	virtual	const char * Get_Help( void )	{ return "DEVICE_INFO - get device driver info."; }
	virtual	void Activate( const char * input ) {
		const RenderDeviceDescClass &	rddesc = WW3D::Get_Render_Device_Desc();
		Print( "Render Device: %s\n", rddesc.Get_Device_Name() );
		Print( "Graphics Hardware: %s\n",rddesc.Get_Hardware_Name() );
	}
};

//
// TEMP DEBUG CODE
//
class NewTCADOConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "newtcado"; }
	virtual	const char * Get_Help( void )	{ return "newtcado - Toggle new TCADO code"; }
	virtual	void Activate( const char * input ) {
		bool is_new_tcado = cDevOptions::UseNewTCADO.Toggle();
      Print(is_new_tcado ? "Using new TCADO.\n" : "Using old TCADO.\n" );
	}
};

class TimeOfDayConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("time_of_day");}
	virtual	const char * Get_Help (void)	{return ("TIME_OF_DAY <hours [0..23]> <minutes [0..59]> - sets the time of day for the background.");}
	virtual	void Activate (const char *input) {

		if (COMBAT_SCENE != NULL) {

			unsigned hours, minutes;

			if (sscanf (input, "%d %d", &hours, &minutes) == 2) {
				if (BackgroundMgrClass::Set_Time_Of_Day (hours, minutes)) {

					const float theta = (((hours * 60.0f) + minutes) / 1440.0f) * 2.0f * WWMATH_PI;

					BackgroundMgrClass::Set_Light_Source_Type (BackgroundMgrClass::LIGHT_SOURCE_TYPE_SUN);
					COMBAT_SCENE->Set_Sun_Light_Orientation (0.0f, theta + (WWMATH_PI * 1.5f));
					Print ("Time of day changed\n");
				}
			}
		}
	}
};

class CloudsConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("clouds");}
	virtual	const char * Get_Help (void)	{return ("CLOUDS <cloudcover [0..1]> <gloominess [0..1]> [ramptime [0+]]");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "Clouds changed\n";

		int	argcount;
		float cloudcover, gloominess, ramptime;
		bool	success = false;

		argcount = sscanf (input, "%f %f %f", &cloudcover, &gloominess, &ramptime);
		switch (argcount) {

			case 2:
				success = BackgroundMgrClass::Set_Clouds (cloudcover, gloominess);
				break;

			case 3:
				success = BackgroundMgrClass::Set_Clouds (cloudcover, gloominess, ramptime);
				break;

			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

class CopyLogsConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("copy_logs");}
	virtual	const char * Get_Help (void)	{return ("COPY_LOGS <on/off> - enable/disable error logging to network.");}
	virtual	void Activate (const char *input) {

		if (!::stricmp(input, "on")) {

			RegistryClass registry(APPLICATION_SUB_KEY_NAME_DEBUG);
			registry.Set_Int("SystemInfoLogDisable", FALSE);
			registry.Set_Int("DisableLogCopying", FALSE);

			Print("Log copying is ON.");

		} else if (!::stricmp(input, "off")) {

			RegistryClass registry(APPLICATION_SUB_KEY_NAME_DEBUG);
			registry.Set_Int("SystemInfoLogDisable", TRUE);
			registry.Set_Int("DisableLogCopying", TRUE);

			Print("Log copying is OFF.");

		} else {

			Print(Get_Help());
		}
	}
};

class SkyTintConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("sky_tint");}
	virtual	const char * Get_Help (void)	{return ("SKY_TINT <tintfactor [0..1]> [ramptime [0+]]");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "Sky tint changed\n";

		int	argcount;
		float skytintfactor, ramptime;
		bool	success = false;

		argcount = sscanf (input, "%f %f %f", &skytintfactor, &ramptime);
		switch (argcount) {

			case 1:
				success = BackgroundMgrClass::Set_Sky_Tint (skytintfactor);
				break;

			case 2:
				success = BackgroundMgrClass::Set_Sky_Tint (skytintfactor, ramptime);
				break;

			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

class InfoDebugToggle : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "info_debug_toggle"; }
	virtual	const char * Get_Help( void )	{ return "INFO_DEBUG_TOGGLE - Toggles display of debug info on object info."; }
	virtual	void Activate( const char * input ) {
		extern bool	InfoDebug;
		InfoDebug = !InfoDebug;
		Print( "Toggled Info Debug\n" );
	}
};

class MeshRenderSnapshotFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "mesh_render_snapshot"; }
	virtual	const char * Get_Help( void )	{ return "MESH_RENDER_SNAPSHOT - Logs one frame of mesh render system."; }
	virtual	void Activate( const char * input ) {
		WW3D::Activate_Snapshot(true);
	}
};

class UmbraToggleFunctionClass : public ConsoleFunctionClass {
public:
	virtual  const char * Get_Name(void) { return "umbra_toggle"; }
	virtual	const char * Get_Help(void) { return "UMBRA_TOGGLE - toggles umbra culling (if compiled into this build)."; }
	virtual  void Activate(const char * input) {
		Print("Umbra support not compiled into this build.\n");

	}
};

class UnusedSpawnerFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "unused_spawners"; }
	virtual	const char * Get_Help( void )	{ return "UNUSED_SPAWNERS - Displays the ID of spawns which have spawned nothing."; }
	virtual	void Activate( const char * input ) {
		SpawnManager::Display_Unused_Spawners();
	}
};

class VipConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "vip"; }
	virtual	const char * Get_Help( void )	{ return "VIP <password> - Toggle VIP mode. Client only.\n"; }

	virtual	void Activate( const char * input ) {

		StringClass password;
		password.Format("%s", input);

      if (cNetwork::I_Am_Client()) {// && !password.Is_Empty()) {

			cVipModeEvent * p_event = new cVipModeEvent;
			p_event->Init(password);

		} else {

		   Print(Get_Help());
		}
	}
};

class AllowKillingHibernatingSpawnFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "allow_killing_hibernating_spawn"; }
	virtual	const char * Get_Help( void )	{ return "ALLOW_KILLING_HIBERNATING_SPAWN - Toggles the global ability for spawners to kill hibernating spawn."; }
	virtual	void Activate( const char * input ) {
		if (	SpawnManager::Toggle_Allow_Killing_Hibernating_Spawn() ) {
			Print("Killing Hibernating Spawn Allowed\n");
		} else {
			Print("Killing Hibernating Spawn Disallowed\n");
		}
	}
};

class DisplayHibernatingFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "display_hibernating"; }
	virtual	const char * Get_Help( void )	{ return "DISPLAY_HIBERNATING - Toggles displaying of objects begining and ending hibernation."; }
	virtual	void Activate( const char * input ) {
extern bool	_DisplayHibernating;
		_DisplayHibernating = !_DisplayHibernating;
		Print("Display Hibernating now %s\n", _DisplayHibernating ? "ON" : "OFF" );
	}
};

class HideHUDPointsFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "hide_hud_points"; }
	virtual	const char * Get_Help( void )	{ return "HIDE_HUD_POINTS - Toggles displaying of points on the HUD."; }
	virtual	void Activate( const char * input ) {
		Print("HUD points toggled\n" );
		HUDClass::Toggle_Hide_Points();
	}
};

class SnapShotModeToggleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "snap_shot_mode_toggle"; }
	virtual	const char * Get_Alias( void ){ return "ssm"; }
	virtual	const char * Get_Help( void )	{ return "SNAP_SHOT_MODE_TOGGLE - Toggles Snap Shot Mode."; }
	virtual	void Activate( const char * input ) {
		if ( COMBAT_CAMERA ) {
			COMBAT_CAMERA->Set_Snap_Shot_Mode( !COMBAT_CAMERA->Is_Snap_Shot_Mode() );
			Print( "Snap Shot Mode %s\n", COMBAT_CAMERA->Is_Snap_Shot_Mode() ? "ON" : "OFF" );
		}
	}
};

class LightningConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("lightning");}
	virtual	const char * Get_Help (void)	{return ("LIGHTNING <intensity [0..1]> [<start [0..1]> <end [0..1]> <heading [0..360]> <distribution [0..1]>] [ramptime [0+]] - Sets lightning.");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "Lightning changed\n";

		int	argcount;
		float arg0, arg1, arg2, arg3, arg4, arg5;
		bool  success = false;

		argcount = sscanf (input, "%f %f %f %f %f %f", &arg0, &arg1, &arg2, &arg3, &arg4, &arg5);
		switch (argcount) {

			case 1:
				success = BackgroundMgrClass::Set_Lightning_Intensity (arg0);
				break;

			case 5:
				success = BackgroundMgrClass::Set_Lightning (arg0, arg1, arg2, arg3, arg4);
				break;

			case 6:
				success = BackgroundMgrClass::Set_Lightning (arg0, arg1, arg2, arg3, arg4, arg5);
				break;

			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

class WarBlitzConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("warblitz");}
	virtual	const char * Get_Help (void)	{return ("WARBLITZ <intensity [0..1]> [<start [0..1]> <end [0..1]> <heading [0..360]> <distribution [0..1]>] [ramptime [0+]] - Sets war blitz.");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "War blitz changed\n";

		int	argcount;
		float arg0, arg1, arg2, arg3, arg4, arg5;
		bool  success = false;

		argcount = sscanf (input, "%f %f %f %f %f %f", &arg0, &arg1, &arg2, &arg3, &arg4, &arg5);
		switch (argcount) {

			case 1:
				success = BackgroundMgrClass::Set_War_Blitz (arg0);
				break;

			case 5:
				success = BackgroundMgrClass::Set_War_Blitz (arg0, arg1, arg2, arg3, arg4);
				break;

			case 6:
				success = BackgroundMgrClass::Set_War_Blitz (arg0, arg1, arg2, arg3, arg4, arg5);
				break;

			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

class WarpConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("warp");}
	virtual	const char * Get_Help (void)	{return ("WARP <player name> - warp to first other player or named player (client only).");}
	virtual	void Activate (const char *input) {
      if (cNetwork::I_Am_Client()) {

			WideStringClass player_name;
			player_name.Convert_From(input);

			cWarpEvent * p_warp = new cWarpEvent;
			p_warp->Init(player_name);

		} else {
		   Print(Get_Help());
		}
	}
};

class WhoIsConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("whois");}
	virtual	const char * Get_Help (void)	{return ("WHOIS <player name> - who the heck is this annoying bugger? (server only)\n");}
	virtual	void Activate (const char *input) {
		cPlayer * p_player = cPlayerManager::Find_Player(input);

		if (!cNetwork::I_Am_Server() || p_player == NULL) {
		   Print(Get_Help());
		} else {

			uint32_t ip = p_player->Get_Ip_Address();
			HOSTENT * p_host = ::gethostbyaddr((char *) &ip, sizeof(ip), AF_INET);
			char resolved_ip[100] = "";
			if (p_host != NULL) {
				::sprintf(resolved_ip, "(%s)", p_host->h_name);
			}

			Print("%s is at ip %s %s\n",
				input, cNetUtil::Address_To_String(ip), resolved_ip);
		}
	}
};

class WindConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("wind");}
	virtual	const char * Get_Help (void)	{return ("WIND <heading [0..360]> <speed [0+]> <variability [0..1]> [ramptime [0+]] - Sets wind.");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "Wind changed\n";

		int	argcount;
		float heading, speed, variability, ramptime;
		bool  success = false;

		argcount = sscanf (input, "%f %f %f %f", &heading, &speed, &variability, &ramptime);
		switch (argcount) {

			case 3:
				success = WeatherMgrClass::Set_Wind (heading, speed, variability);
				break;

			case 4:
				success = WeatherMgrClass::Set_Wind (heading, speed, variability, ramptime);
				break;

			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

class RainConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("rain");}
	virtual	const char * Get_Help (void)	{return ("RAIN <density [0+]> [ramptime [0+]] - Sets rain.");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "Rain changed\n";

		int	argcount;
		float	arg0, arg1;
		bool	success = false;

		argcount = sscanf (input, "%f %f", &arg0, &arg1);
		switch (argcount) {

			case 1:
				success = WeatherMgrClass::Set_Precipitation (WeatherMgrClass::PRECIPITATION_RAIN, arg0);
				break;

			case 2:
				success = WeatherMgrClass::Set_Precipitation (WeatherMgrClass::PRECIPITATION_RAIN, arg0, arg1);
				break;

			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

class SnowConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("snow");}
	virtual	const char * Get_Help (void)	{return ("SNOW <density [0+]> [ramptime [0+]] - Sets snow.");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "Snow changed\n";

		int	argcount;
		float	arg0, arg1;
		bool	success = false;

		argcount = sscanf (input, "%f %f", &arg0, &arg1);
		switch (argcount) {

			case 1:
				success = WeatherMgrClass::Set_Precipitation (WeatherMgrClass::PRECIPITATION_SNOW, arg0);
				break;

			case 2:
				success = WeatherMgrClass::Set_Precipitation (WeatherMgrClass::PRECIPITATION_SNOW, arg0, arg1);
				break;

 			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

class AppPacketTypesResetConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name( void )	{ return "app_packet_types_reset"; }
	virtual	const char * Get_Alias( void ){ return "aptr"; }
	virtual	const char * Get_Help( void )	{ return "APP_PACKET_TYPES_RESET - reset app packet stats."; }
	virtual	void Activate( const char * input ) {
		cAppPacketStats::Reset();
	}
};

class AshConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("ash");}
	virtual	const char * Get_Help (void)	{return ("ASH <density [0+]> [ramptime [0+]] - Sets ash.");}
	virtual	void Activate (const char *input) {

		const char *feedbackstring = "Ash changed\n";

		int	argcount;
		float	arg0, arg1;
	  	bool	success = false;

		argcount = sscanf (input, "%f %f", &arg0, &arg1);
		switch (argcount) {

			case 1:
				success = WeatherMgrClass::Set_Precipitation (WeatherMgrClass::PRECIPITATION_ASH, arg0);
				break;

			case 2:
				success = WeatherMgrClass::Set_Precipitation (WeatherMgrClass::PRECIPITATION_ASH, arg0, arg1);
				break;

			default:
				// Do nothing.
				break;
		}
		if (success) Print (feedbackstring);
	}
};

/*
**
*/
class CinematicFreezeConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("cinematic_freeze");}
	virtual	const char * Get_Help (void)	{return ("CINEMATIC_FREEZE - Toggles Cinematic Freeze.");}
	virtual	void Activate (const char *input) {
		GameObjManager::Toggle_Cinematic_Freeze();
		Print( "Cinematic Freeze %s\n",
			GameObjManager::Is_Cinematic_Freeze_Active() ? "ON" : "OFF" );
	}
};

class EditVehicleConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("edit_vehicle");}
	virtual	const char * Get_Help (void)	{return ("EDIT_VEHICLE - Edit the parameters of the currently driven vehicle.");}
	virtual	void Activate (const char *input)
	{
		// Only do this in single player or 1-player multiplay games
      if (IS_SOLOPLAY || ((PTheGameData != NULL) && (PTheGameData->Get_Max_Players() == 1)) )
		{
			VehicleGameObj * vehicle = COMBAT_STAR->Get_Vehicle();
			if ((vehicle != NULL) && (vehicle->Peek_Vehicle_Phys() != NULL)) {
				VehiclePhysClass * pobj = vehicle->Peek_Vehicle_Phys();

				TrackedVehicleClass * tv = pobj->As_TrackedVehicleClass();
				WheeledVehicleClass * wv = pobj->As_WheeledVehicleClass();

				if (tv != NULL) {

					float wheel_radius = 0.0f;
					for (int i=0; i<tv->Get_Wheel_Count(); i++) {
						WheelClass * wheel = (WheelClass*)tv->Peek_Wheel(i);
						if (wheel->Get_Flag(SuspensionElementClass::ENGINE)) {
							wheel_radius = wheel->Get_Radius();
						}
					}

					TrackedVehicleDefClass * def = (TrackedVehicleDefClass *)tv->Get_TrackedVehicleDef();

					EditTrackedVehicleDialogClass * popup = new EditTrackedVehicleDialogClass(def,wheel_radius);
					popup->Start_Dialog();
					popup->Release_Ref();

				} else if (wv != NULL) {

					float wheel_radius = 0.0f;
					for (int i=0; i<wv->Get_Wheel_Count(); i++) {
						WheelClass * wheel = (WheelClass*)wv->Peek_Wheel(i);
						if (wheel->Get_Flag(SuspensionElementClass::ENGINE)) {
							wheel_radius = wheel->Get_Radius();
						}
					}

					WheeledVehicleDefClass * def = (WheeledVehicleDefClass *)wv->Get_WheeledVehicleDef();
					EditWheeledVehicleDialogClass * popup = new EditWheeledVehicleDialogClass(def,wheel_radius);
					popup->Start_Dialog();
					popup->Release_Ref();

				}
			}
		}
	}
};

/*
**
*/
class ToggleUseLetencyInterpretConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("toggle_latency_interpret");}
	virtual	const char * Get_Help (void)	{return ("TOGGLE_LATENCY_INTERPRET - Toggles Latency Interpret logic.");}
	virtual	void Activate (const char *input) {
		extern	bool	_UseLatencyInterpret;
		_UseLatencyInterpret	= !_UseLatencyInterpret;
		Print( "UseLetencyInterpret %s\n",
			_UseLatencyInterpret ? "ON" : "OFF" );
	}
};

/*
**
*/
class ToggleHitReticleConsoleFunctionClass : public ConsoleFunctionClass {
public:
	virtual	const char * Get_Name (void)	{return ("toggle_hit_reticle");}
	virtual	const char * Get_Help (void)	{return ("TOGGLE_HIT_RETICLE - Toggles the center reticle dot.");}
	virtual	void Activate (const char *input) {
		CombatManager::Toggle_Hit_Reticle_Enabled();
		Print( "Hit Reticle Toggled\n" );
	}
};

//------------------------------------------------------------------------------------
/*
** Console Function Manager
*/
DynamicVectorClass<ConsoleFunctionClass *>	ConsoleFunctionManager::FunctionList;

void	ConsoleFunctionManager::Init( void )
{

   //
   // SHIPPING CONSOLE FUNCTIONS ONLY
   //

	FunctionList.Add( new GameOverConsoleFunctionClass() );
   FunctionList.Add( new AdminMessageConsoleFunctionClass() );
	FunctionList.Add( new GameInfoConsoleFunctionClass() );
	FunctionList.Add( new KickConsoleFunctionClass() );
	FunctionList.Add( new AllowConsoleFunctionClass() );

	FunctionList.Add( new BanConsoleFunctionClass() );
   FunctionList.Add( new MessageConsoleFunctionClass() );
	FunctionList.Add( new PlayerInfoConsoleFunctionClass() );
	FunctionList.Add( new QuitConsoleFunctionClass() );
	FunctionList.Add( new RestartConsoleFunctionClass() );
	FunctionList.Add( new ScreenUVBiasConsoleFunctionClass() );
	FunctionList.Add( new SetBandwidthBudgetOutConsoleFunctionClass() );
	FunctionList.Add( new ToggleSortingConsoleFunctionClass() );
	FunctionList.Add( new ExtrasConsoleFunctionClass() );  /// CHEATS? MAY NEED TO BE DEV ONLY!!!!
	FunctionList.Add( new EditVehicleConsoleFunctionClass() );
	FunctionList.Add( new NetUpdateRateConsoleFunctionClass() );
	FunctionList.Add( new ClientPhysicsOptimizationConsoleFunctionClass() );
	FunctionList.Add( new FPSConsoleFunctionClass() );		// Steve W wanted this.
// FDS: some console commands were restricted or modified

	SystemSettings::Add_Console_Functions( FunctionList );

	Sort_Function_List();

	//
	// Dump list to a file
	//
	Verbose_Help_File();
}

void	ConsoleFunctionManager::Shutdown( void )
{
	while ( FunctionList.Count() ) {
		delete FunctionList[ FunctionList.Count()-1 ];
		FunctionList.Delete( FunctionList.Count()-1 );
	}
}

//------------------------------------------------------------------------------------

void	ConsoleFunctionManager::Sort_Function_List( void )
{
	// ARGH!! Bubblesort!!!
	bool done = false;
	int count = FunctionList.Count();
	while ( !done ) {
		done = true;
		count--;
		for (	int index = 0; index < count; index++) {
			if ( stricmp(	FunctionList[index]->Get_Name(),
								FunctionList[index+1]->Get_Name() ) > 0 ) {
				ConsoleFunctionClass * temp = FunctionList[index];
				FunctionList[index] = FunctionList[index+1];
				FunctionList[index+1] = temp;
				done = false;
			}
		}
	}
}

//------------------------------------------------------------------------------------

void	ConsoleFunctionManager::Help(	const char * function_name )
{
	char string[120];

	// Eat leading spaces
	while ( function_name && *function_name == ' ' ) {
		function_name++;
	}

	if ( ( function_name == NULL ) || ( function_name[0] == 0 ) ) {
		// Show name of each function...
      Print( "------ Available commands -------\n" );
		*string = 0;
		for (	int index = 0; index < FunctionList.Count(); index++) {
			ConsoleFunctionClass * function = FunctionList[index];
			if ( *string ) {
				strcat( string, ", " );
			}
			strcat( string, function->Get_Name() );
			if ( ( strlen( string ) > (sizeof( string ) / 2) ) || (index == FunctionList.Count()-1) ) {
				strcat( string, "\n" );
				Print( string );
				*string = 0;
			}
		}
	} else {
		// Show help for a certian function
		for (	int index = 0; index < FunctionList.Count(); index++) {
			ConsoleFunctionClass * function = FunctionList[index];
			if ( !stricmp( function->Get_Name(), function_name ) ) {
		      Print( function->Get_Help() );
				return;
			}
		}
		Print( "Help not found for \"%s\"\n", function_name );
	}
}

//------------------------------------------------------------------------------------
int ConsoleFunctionManager::Get_Function_Count(void)
{
	return FunctionList.Count();
}

//------------------------------------------------------------------------------------
void ConsoleFunctionManager::Verbose_Help_File(void)
{
	//
	// Log function list to a file
	//
   FILE * file = fopen("commands.txt", "w");
	if ( file ) {
		char buffer[500];
		sprintf(buffer, "RENEGADE COMMANDS as at %s\n\n", cMiscUtil::Get_Text_Time());
		fwrite(buffer, 1, strlen(buffer), file);
		for (	int index = 0; index < FunctionList.Count(); index++) {
			ConsoleFunctionClass * function = FunctionList[index];
			sprintf(buffer, "%s", function->Get_Help());
			if (function->Get_Alias() != NULL) {
				char alias_string[100];
				sprintf(alias_string, " (%s)", function->Get_Alias());
				strcat(buffer, alias_string);
			}
			strcat(buffer, "\n");
			fwrite(buffer, 1, strlen(buffer), file);
		}

		fclose(file);
	}
}

//------------------------------------------------------------------------------------
void ConsoleFunctionManager::Next_Verbose_Help_Screen(void)
{
	//
	// Show name of each function on a separate line...
	//
	char buffer[5000];
	buffer[0] = 0;

	static int page_number = -1;
	page_number++;

	if ( page_number >= 0 ) {
		int command_number = 0;
		for (	int index = 0; index < FunctionList.Count(); index++) {
			command_number++;
			int per_page = 57;
			if (floor(command_number / per_page) == page_number) {
				ConsoleFunctionClass * function = FunctionList[index];
				strcat( buffer, function->Get_Help() );
				strcat( buffer, "\n" );

				if ( index == FunctionList.Count() - 1 ) {	// if this is the last line,
					page_number = -2;				// turn it off for next time

				}
			}
		}
	}
	if (Get_Text_Display()) {
		Get_Text_Display()->Set_Verbose_Help_Text( buffer );
	}
}

//------------------------------------------------------------------------------------
void	ConsoleFunctionManager::Parse_Input( const char * string )
{
	if (!strnicmp(string, "help", 4)) {
		Help( string + 4 );
	} else {
		for (	int index = 0; index < FunctionList.Count(); index++) {
			ConsoleFunctionClass * function = FunctionList[index];

         if ( !strnicmp( string, function->Get_Name(), strlen( function->Get_Name() ) ) ) {

				const char *s = string + strlen (function->Get_Name());

				// The next input character must be either a string terminator or space character
				// in order for the input string to match the function list string.
				if ((*s == '\0') || (*s == ' ')) {

					// Remove any trailing white space.
					while (*s == ' ') s++;

					function->Activate (s);
					return;
				}
         }

         //
         // Check short-form alias too
         // There has to be a space (or nothing) following the alias for
         // it to match
         //
         if (function->Get_Alias() != NULL &&
            strlen(function->Get_Alias()) > 0) {
            char alias[100];
            strcpy(alias, function->Get_Alias());
            int alias_len = static_cast<int32_t>(strlen(alias));
            int string_len = static_cast<int32_t>(strlen(string));
            if (!strnicmp(string, alias, alias_len) &&
               (string_len == alias_len || string[alias_len] == ' ')) {
				   string += alias_len;
				   while ( string && *string == ' ' ) string++;
				   function->Activate( string );
				   return;
            }
         }
		}

		if (ConsoleBox.Is_Exclusive()) {
      	Print( "\"%s\" is not a command. To send a message, use \"msg\" and \"amsg\"\n", string );
		} else {
      	Print( "\"%s\" is not a command. To send a message, Use F2 and F3\n", string );
		}
      Print( "For a list of available commands, use the \"help\" command !\n" );
	}
}

bool ConsoleFunctionManager::Get_Command_Suggestion
(
	const char *	input,					// what user has typed,
	const char *	cur_suggestion,		// last suggestion we gave (move to next if not null)
	char *			set_suggestion,		// copy suggestion into this string
	char *			set_help,				// copy help into this string
	int				len						// don't copy more than this many characters.
)
{
	ConsoleFunctionClass * function = NULL;
	int	node_index = 0;

	/*
	** If user is passing in a 'cur_suggestion', then we need to first find that
	** function in the list and search past it.  If we don't find a suggestion, we
	** will drop through and search from the head of the list.
	*/
	if (cur_suggestion != NULL) {

		node_index = Find_Function_Node(cur_suggestion);

		/*
		** Try to find a suggestion after this node.
		*/
		if (node_index != -1) {
			function = Find_Command_Suggestion(input,node_index+1);
		}
	}

	/*
	** Find a suggestion starting at the head of the list
	*/
	if (function == NULL) {
		function = Find_Command_Suggestion(input,0);
	}

	/*
	** If we found a suggestion, copy its command into 'set_suggestion' and its help
	** into 'set_help'
	*/
	if (function != NULL) {
		if (set_suggestion != NULL) {
			strncpy(set_suggestion,function->Get_Name(),len);
		}
		if (set_help != NULL) {
			strncpy(set_help,function->Get_Help(),len);
		}
		return true;
	}
	return false;
}

int ConsoleFunctionManager::Find_Function_Node(const char * name)
{
	for (	int index = 0; index < FunctionList.Count(); index++) {
		ConsoleFunctionClass * function = FunctionList[index];
		if (stricmp(name,function->Get_Name()) == 0) {
			return index;
		}
	}
	return -1;
}

ConsoleFunctionClass * ConsoleFunctionManager::Find_Command_Suggestion
(
	const char *	input,		// incomplete function name
	int				start_index	// node to start searching from
)
{
	for (	int index = start_index; index < FunctionList.Count(); index++) {
		ConsoleFunctionClass * function = FunctionList[index];
		if (_strnicmp(input,function->Get_Name(),strlen(input)) == 0) {
			return function;
		}
	}
	return NULL;
}

void	ConsoleFunctionManager::Print( const char *format, ... )
{
	va_list arg_list;
	va_start (arg_list, format);
	StringClass string;
	string.Format_Args( format, arg_list );
	if (Get_Text_Display()) {
		Get_Text_Display()->Print_System( string );
	}
	ConsoleBox.Print(string.Peek_Buffer());
	va_end (arg_list);
}
