#include "player.h"

#include <string.h>
#include <stdio.h>

#include "playermanager.h"
#include "teammanager.h"
#include "miscutil.h"
#include "cnetwork.h"
#include "textdisplay.h"
#include "WWAudio.H"
#include "gamedata.h"
#include "chunkio.h"
#include "useroptions.h"
#include "smartgameobj.h"
#include "playertype.h"
#include "translatedb.h"
#include "string_ids.h"
#include "devoptions.h"
#include "colors.h"
#include "persistfactory.h"
#include "combatchunkid.h"
#include "networkobjectfactory.h"
#include "gameobjmanager.h"
#include "gametype.h"
#include "multihud.h"
#include "apppackettypes.h"
#include "messagewindow.h"
#include "playerkill.h"
#include "consolemode.h"

DECLARE_NETWORKOBJECT_FACTORY(cPlayer, NETCLASSID_PLAYER);

//
// Class statics
//
const int cPlayer::INVALID_ID		= -1000;

//------------------------------------------------------------------------------------
cPlayer::cPlayer(void)
{
	//

   Id						= INVALID_ID;
   LadderPoints		= 0;
	Kills					= 0;
   Deaths				= 0;
	Rung					= 0;
	IpAddress			= 0;
	Fps					= 0;
	LastUpdateTimeMs	= 0;
	DamageScaleFactor = 100;
	Ping					= -1;
	FastSortKey			= 0;

	Reset_Join_Time();
	Reset_Total_Time();
	Set_Is_In_Game(true);
	Set_Is_Waiting_For_Intermission(false);
	Set_App_Packet_Type(APPPACKETTYPE_PLAYER);

   cPlayerManager::Add(this);
}

//------------------------------------------------------------------------------------
cPlayer::~cPlayer(void)
{
   //
	//

	cPlayerManager::Remove(this);
}

//------------------------------------------------------------------------------------
void cPlayer::Delete(void)
{
	delete this;
}

//------------------------------------------------------------------------------------
void cPlayer::Init(void)
{

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
enum	{
	CHUNKID_VARIABLES = 12374,
	CHUNKID_PARENT,

   MICROCHUNK_ID = 1,
   MICROCHUNK_NAME,
   MICROCHUNK_PLAYERTYPE,
   MICROCHUNK_KILLS,
   MICROCHUNK_DEATHS,
   XXXMICROCHUNK_SCORE,
   MICROCHUNK_NUMLIVES,
   MICROCHUNK_TEAMNUMBER,
   MICROCHUNK_REMAP_POINTER,
};

//-----------------------------------------------------------------------------
bool cPlayer::Save(ChunkSaveClass & csave)
{
	csave.Begin_Chunk(CHUNKID_PARENT);
	PlayerDataClass::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk(CHUNKID_VARIABLES);

	WRITE_MICRO_CHUNK(csave, MICROCHUNK_ID, Id);
	WRITE_MICRO_CHUNK_WIDESTRING(csave,	MICROCHUNK_NAME, Name);
	WRITE_MICRO_CHUNK(csave, MICROCHUNK_KILLS, Kills);
	WRITE_MICRO_CHUNK(csave, MICROCHUNK_DEATHS, Deaths);
	WRITE_MICRO_CHUNK(csave, MICROCHUNK_TEAMNUMBER, PlayerType);
	void * old_ptr = this;
	WRITE_MICRO_CHUNK(csave, MICROCHUNK_REMAP_POINTER, old_ptr);

	csave.End_Chunk();

	return true;
}

//-----------------------------------------------------------------------------
bool cPlayer::Load(ChunkLoadClass &cload)
{
	void * old_ptr = NULL;

	while (cload.Open_Chunk()) {
		switch(cload.Cur_Chunk_ID()) {

		case CHUNKID_PARENT:
				PlayerDataClass::Load( cload );
				break;

			case CHUNKID_VARIABLES:
				while (cload.Open_Micro_Chunk()) {

					switch(cload.Cur_Micro_Chunk_ID()) {
						READ_MICRO_CHUNK(cload, MICROCHUNK_ID, Id);
						READ_MICRO_CHUNK_WIDESTRING(cload, MICROCHUNK_NAME, Name);
						READ_SAFE_MICRO_CHUNK(cload, MICROCHUNK_KILLS, Kills,int);
						READ_SAFE_MICRO_CHUNK(cload, MICROCHUNK_DEATHS, Deaths, int);
						READ_SAFE_MICRO_CHUNK(cload, MICROCHUNK_TEAMNUMBER, PlayerType, int);
						READ_MICRO_CHUNK(cload, MICROCHUNK_REMAP_POINTER, old_ptr);

						default:
							Debug_Say(( "Unrecognized cPlayer Variable chunkID\n" ));
							break;
					}

					cload.Close_Micro_Chunk();
				}
				break;

			default:
				Debug_Say(( "Unrecognized cPlayer chunkID\n" ));
				break;

		}
		cload.Close_Chunk();
	}

	if ( old_ptr != NULL ) {
		SaveLoadSystemClass::Register_Pointer(old_ptr, this);
	}

	return true;
}

//------------------------------------------------------------------------------------
void cPlayer::On_Create(void)
{

   //
   // Don't show a join message unless:
   //   - It's MP
   //   - I am rendering
   //   - It's not about me
   //   - His join came after my join (the server will send you existing
   //     players before your own).
   //

   if (!IS_MISSION && cPlayerManager::Is_Player_Present(cNetwork::Get_My_Id())) {

      if (cNetwork::Show_Welcome_Message(Name)) {
         switch (PlayerType) {
            case PLAYERTYPE_RENEGADE:
				{
					WideStringClass widestring;
					widestring.Format(
						L"%s, %s.\n",
						TRANSLATION(IDS_MP_WELCOME_RENEGADE),
						Name);
					//
					//	Display the message...
					//
					CombatManager::Get_Message_Window ()->Add_Message (widestring, Vector3 (0.7F, 0.7F, 0.7F));
               break;
				}
	         case PLAYERTYPE_NOD:
		      case PLAYERTYPE_GDI:
				{
					WideStringClass widestring;
					if (The_Game()->IsTeamChangingAllowed.Is_True()) {
						widestring.Format(
							L"%s, %s.\n",
							TRANSLATION(IDS_MP_WELCOME_RENEGADE),
							Name);
					} else {
						widestring.Format(
							L"%s, %s, %s %s %s.\n",
							TRANSLATION(IDS_MP_WELCOME_RENEGADE),
							Name,
							TRANSLATION(IDS_MP_TO),
							The_Game()->Get_Team_Word(),
							cTeamManager::Get_Team_Name(PlayerType));
					}

					//
					//	Display the message...
					//
					CombatManager::Get_Message_Window ()->Add_Message (widestring, Vector3 (0.7F, 0.7F, 0.7F));
               break;
				}
            default:
         }

      } else {
         switch (PlayerType) {
            case PLAYERTYPE_MUTANT:
            case PLAYERTYPE_NEUTRAL:
               break;
            case PLAYERTYPE_RENEGADE:
				{
					WideStringClass widestring;
					widestring.Format(
						L"%s %s\n",
						Name,
						TRANSLATION(IDS_MP_RENEGADE_JOINS));

					//
					//	Display the message...
					//
					CombatManager::Get_Message_Window ()->Add_Message (widestring, Vector3 (0.7F, 0.7F, 0.7F));
               break;
				}
	         case PLAYERTYPE_NOD:
		      case PLAYERTYPE_GDI:
				{
					WideStringClass widestring;
					if (The_Game()->IsTeamChangingAllowed.Is_True()) {
						widestring.Format(
							L"%s %s\n",
							Name,
							TRANSLATION(IDS_MP_RENEGADE_JOINS));
					} else {
						widestring.Format(
							L"%s %s %s %s\n",
							Name,
							TRANSLATION(IDS_MP_TEAMPLAYER_JOINS),
							The_Game()->Get_Team_Word(),
							cTeamManager::Get_Team_Name(PlayerType));
					}

					//
					//	Display the message...
					//
					Vector3 color(0.7F, 0.7F, 0.7F);
					CombatManager::Get_Message_Window ()->Add_Message (widestring, color);
					break;
				}

            default:
         }
      }
   }
}

//------------------------------------------------------------------------------------
void cPlayer::On_Destroy(void)
{
   if (cNetwork::I_Am_Client() &&
		 GameModeManager::Find("Combat")->Is_Active() &&
		 The_Game() != NULL &&
		 The_Game()->Is_Cnc()) {

		WideStringClass message;
		message.Format(
			L"%s %s\n",
			Name, TRANSLATION(IDS_MP_PLAYER_LEFT_GAME));

		//
		//	Display the message...
		//
		CombatManager::Get_Message_Window ()->Add_Message (message, Vector3 (0.7F, 0.7F, 0.7F));
   }
}

//------------------------------------------------------------------------------------
void	cPlayer::Mark_As_Modified(void)
{
	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Id(int id)
{
	Id = id;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Name(const WideStringClass & name)
{
	Name = name;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Player_Type(int type)
{

	PlayerType = type;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//-----------------------------------------------------------------------------
bool cPlayer::Is_Team_Player(void)
{
	return PlayerType == PLAYERTYPE_NOD || PlayerType == PLAYERTYPE_GDI;
}

//------------------------------------------------------------------------------------
void cPlayer::Reset_Player(void)
{
	//
	// Call base class
	//
	PlayerDataClass::Reset_Player();

	Set_Ladder_Points(0);
   Set_Kills(0);
   Set_Deaths(0);

	Reset_Join_Time();
	Reset_Total_Time();

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//------------------------------------------------------------------------------------
Vector3 cPlayer::Get_Color(void) const
{
   switch (PlayerType) {

      case PLAYERTYPE_NEUTRAL:
			return COLOR_NEUTRAL;
         break;

      case PLAYERTYPE_RENEGADE:
			return COLOR_RENEGADE;
         break;

      case PLAYERTYPE_NOD:
			return Get_Color_For_Team(PLAYERTYPE_NOD);
			break;

      case PLAYERTYPE_GDI:
			return Get_Color_For_Team(PLAYERTYPE_GDI);
			break;

      default:

         //
			//TSS110101
			//This crashed the server in the beta test.
			//I don't know how we got here.
			//Try making it non-fatal.
			//
			//DIE;
         return COLOR_NEUTRAL; // avoid compiler warning
   }
}

//------------------------------------------------------------------------------------
void cPlayer::Get_Player_String(int rank, WideStringClass & string, bool force_verbose)
{
   //
   // Compose a string description of a player's stats for display
   //
#pragma message("TODO: (TSS) Examine all wide string %s formatting for errors...")

	string.Format(L"");

	bool is_verbose = force_verbose ||
		               The_Game()->IsIntermission.Is_True() ||
							//MultiHUDClass::Get_Verbose_Lists();
							(MultiHUDClass::Get_Playerlist_Format() == PLAYERLIST_FORMAT_FULL);

	WideStringClass substring(0, true);

	//
	// Standing
	//
	//XXX substring.Format(L" %2d. ", rank);
	substring.Format(L" %2d. ", (int) Get_Rung());
   string += substring;

	//
   // Name
   //
	substring.Format(L"%-9s", Name);
	if (IsActive.Is_True()) {
		substring += L"  ";
	} else {
		substring += L"##";
	}
   string += substring;

	if (IsInGame.Is_False()) {
		//
		// Don't show all the other details, just show "RELOADING"
		//
		substring.Format(L"%s", TRANSLATION(IDS_MP_LOADING));
		string += substring;
		return;
	}

	//
	// Kills
	//
   if (is_verbose) {
      substring.Format(L"%-8d", Get_Kills());
      string += substring;
   }

	//
	// Deaths
	//
   if (is_verbose) {
      substring.Format(L"%-8d", Get_Deaths());
      string += substring;
   }

	//
	// Kill to Death ratio
	//
   if (is_verbose) {
		float ktd = Get_Kill_To_Death_Ratio();
		if (ktd >= 0) {
			substring.Format(L"%-8.1f", ktd);
		} else {
			substring.Format(L"%-8s", "-");
		}
      string += substring;
   }

	//
	// Money.
	// Do not show other team's money.
	//
	if ((The_Game()->Is_Cnc() || The_Game()->Is_Skirmish()) && is_verbose) {
		bool show =
			  cNetwork::I_Am_Only_Server() ||
			 (cNetwork::I_Am_Client() &&
			  cNetwork::Get_My_Player_Object() != NULL &&
			  (cNetwork::Get_My_Team_Number() == (int) PlayerType));

		if (show) {
	      substring.Format(L"%-8d", (int) Get_Money());
		} else {
	      substring.Format(L"%-8s", "-");
		}

      string += substring;
   }

	//
	// Score
	//
	substring.Format(L"%-8d", (int) Get_Score());
   string += substring;

	//
	// Ladder Points
	//
	if (force_verbose && The_Game()->IsLaddered.Is_True()) {
		substring.Format(L"%-8d", Get_Ladder_Points());
      string += substring;
	}

	if (force_verbose) {
		//
		// Logged player list has no color so we need to show Nod/GDI
		//
	   //substring.Format(L"  T%d", (int) PlayerType);
		if (PlayerType == PLAYERTYPE_NOD) {
			substring.Format(L"  %s", TRANSLATION(IDS_MP_TEAMNAME_MISSIONS_TEAM_0));
		} else {
			substring.Format(L"  %s", TRANSLATION(IDS_MP_TEAMNAME_MISSIONS_TEAM_1));
		}
	   string += substring;
	}
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Score(float score)
{
	//
	// Call base class
	//
	PlayerDataClass::Set_Score(score);

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_OCCASIONAL, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Increment_Score(float add)
{
	if (!CombatManager::Is_Gameplay_Permitted()) {
		return;
	}

	//
	// Call base class
	//
	PlayerDataClass::Increment_Score(add);

	//
	// If this player is on a team, increment the team score
	//
	if (Is_Team_Player()) {
      cTeam * p_team = cTeamManager::Find_Team(Get_Player_Type());
		p_team->Increment_Score(add);
	}

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_OCCASIONAL, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Money(float money)
{
	//
	// Call base class
	//
	PlayerDataClass::Set_Money(money);

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_OCCASIONAL, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Increment_Money(float add)
{
	if (!CombatManager::Is_Gameplay_Permitted()) {
		return;
	}

	//
	// Call base class
	//
	PlayerDataClass::Increment_Money(add);

	/*
	//
	// If this player is on a team, increment the team money
	//
	if (Is_Team_Player()) {
      cTeam * p_team = cTeamManager::Find_Team(Get_Player_Type());
		p_team->Increment_Money(add);
	}
	*/

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_OCCASIONAL, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Ladder_Points(int points)
{
   LadderPoints = points;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Kills(int new_kills)
{

   Kills = new_kills;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_OCCASIONAL, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Deaths(int new_deaths)
{

   Deaths = new_deaths;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_OCCASIONAL, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Rung(int rung)
{
   Rung = rung;
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Damage_Scale_Factor(int factor)
{
   DamageScaleFactor = factor;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Reset_Join_Time(void)
{
   JoinTimeMs = TIMEGETTIME();
}

//------------------------------------------------------------------------------------
void cPlayer::Increment_Total_Time(void)
{
	TotalTimeMs += (TIMEGETTIME() - JoinTimeMs);
}

//------------------------------------------------------------------------------------
void cPlayer::Reset_Total_Time(void)
{
	TotalTimeMs = 0;
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Ip_Address(ULONG ip_address)
{

	IpAddress = ip_address;
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Fps(int fps)
{

	Fps = fps;
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Last_Update_Time_Ms(DWORD time_ms)
{
	LastUpdateTimeMs = time_ms;
}

//------------------------------------------------------------------------------------
float cPlayer::Get_Kill_To_Death_Ratio(void) const
{
	//
	// If Deaths = 0, return -1;
	//
	float ktd = -1;
	if ((int) Deaths > 0) {
		ktd = ((int) Kills) / (float) ((int)Deaths);
	}

	return ktd;
}

//------------------------------------------------------------------------------------
void cPlayer::Increment_Kills(void)
{
	if (!CombatManager::Is_Gameplay_Permitted()) {
		return;
	}

   Set_Kills((int)Kills + 1);
}

//------------------------------------------------------------------------------------
void cPlayer::Increment_Deaths(void)
{
	if (!CombatManager::Is_Gameplay_Permitted()) {
		return;
	}

   Set_Deaths((int)Deaths + 1);
}

/*
//------------------------------------------------------------------------------------
int cPlayer::Get_Avg_Ping(void) const
{
   //
   // If you are a server this method will return the player's S->C ping.
   // If you are a client this method will return your own C->S ping, and
   // -1 for anyone else.
   //

   int ping = -1;

   if (cNetwork::I_Am_Server()) {
      if (Id > 0) {
			cRemoteHost * p_rhost = cNetwork::Get_Server_Rhost(Id);
			ping = p_rhost->Get_Average_Internal_Pingtime_Ms();
		}
   } else if (Id == cNetwork::Get_My_Id()) {
      cRemoteHost * p_rhost = cNetwork::Get_Client_Rhost();
      if (p_rhost != NULL) {
         //
         // It can be NULL as you are quitting
         //
         ping = p_rhost->Get_Average_Internal_Pingtime_Ms();
      }
   }

   return ping;
}
*/

//------------------------------------------------------------------------------------
int cPlayer::Get_Ping(void)
{
   //
	// Update the ping if we are the server.
	//
	if (cNetwork::I_Am_Server() && Id > 0) {
		cRemoteHost * p_rhost = cNetwork::Get_Server_Rhost(Id);
		if (p_rhost != NULL) {
			Ping = p_rhost->Get_Average_Internal_Pingtime_Ms();
		}
   }

	return Ping;
}

//------------------------------------------------------------------------------------
void cPlayer::Export_Creation(BitStreamClass &packet)
{
	NetworkObjectClass::Export_Creation(packet);

   packet.Add_Wide_Terminated_String(Name);
}

//------------------------------------------------------------------------------------
void cPlayer::Import_Creation(BitStreamClass &packet)
{
	NetworkObjectClass::Import_Creation(packet);

	packet.Get_Wide_Terminated_String(Name.Get_Buffer(256), 256);
}

//------------------------------------------------------------------------------------
void cPlayer::Export_Rare(BitStreamClass &packet)
{
	packet.Add(Id);
	packet.Add((int)LadderPoints);
	packet.Add((int)PlayerType);
	packet.Add(DamageScaleFactor);
	packet.Add(Get_Ping());

	packet.Add(IsInGame.Get());
	packet.Add(Invulnerable.Get());
	packet.Add(IsActive.Get());

	packet.Add((int)0);    // was: WolRank (removed — WOL service defunct)
	packet.Add((int)0);    // was: NumWolGames (removed — WOL service defunct)
	//

}

//------------------------------------------------------------------------------------
void cPlayer::Import_Rare(BitStreamClass &packet)
{
	int int_placeholder = 0;

	Set_Id(							packet.Get(int_placeholder));
   Set_Ladder_Points(			packet.Get(int_placeholder));
   Set_Player_Type(				packet.Get(int_placeholder));
	Set_Damage_Scale_Factor(	packet.Get(int_placeholder));
	Set_Ping(						packet.Get(int_placeholder));

	bool bool_placeholder = false;

	Set_Is_In_Game(				packet.Get(bool_placeholder));
	Invulnerable.Set(				packet.Get(bool_placeholder));
	Set_Is_Active(					packet.Get(bool_placeholder));

	packet.Get(int_placeholder);    // was: WolRank (removed — WOL service defunct)
	packet.Get(int_placeholder);    // was: NumWolGames (removed — WOL service defunct)
}

//------------------------------------------------------------------------------------
void cPlayer::Export_Occasional(BitStreamClass &packet)
{
	//
	// Call the base class
	//
	PlayerDataClass::Export_Occasional(packet);

   packet.Add((int)Kills);
	packet.Add((int)Deaths);
}

//------------------------------------------------------------------------------------
void cPlayer::Import_Occasional(BitStreamClass &packet)
{
	//
	// Call the base class
	//
	PlayerDataClass::Import_Occasional(packet);

	int int_placeholder = 0;

   Set_Kills(		packet.Get(int_placeholder));
   Set_Deaths(		packet.Get(int_placeholder));
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Is_In_Game(bool state)
{
	IsInGame.Set(state);

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Is_Waiting_For_Intermission(bool state)
{
	IsWaitingForIntermission.Set(state);
}

//------------------------------------------------------------------------------------
void cPlayer::Set_Is_Active(bool flag)
{
	if (flag != IsActive.Get()) {

		if (cNetwork::I_Am_Client()) {
			if (flag == true) {
				On_Create();
	      cPlayerManager::Activated(this);
			} else {
				On_Destroy();
	      cPlayerManager::Deactivated(this);
			}
		}

		IsActive.Set(flag);

		Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true);
	}
}

//------------------------------------------------------------------------------------
bool cPlayer::Is_Alive_And_Kicking(void) const
{
	//
	// Abbreviates those players who are human, loaded, and active. 
	//

	return Is_Human() && IsInGame.Is_True() && IsActive.Is_True();
}

