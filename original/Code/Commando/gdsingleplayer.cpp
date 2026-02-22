#include "global.h"
#include "gdsingleplayer.h"  // I WANNA BE FIRST!

#include "cnetwork.h"
#include "translatedb.h"
#include "string_ids.h"
#include "playertype.h"

//-----------------------------------------------------------------------------
cGameDataSinglePlayer::cGameDataSinglePlayer(void) : cGameData()
{
   //

	IsFriendlyFirePermitted.Set(true);
	IsTeamChangingAllowed.Set(false);
	SpawnWeapons.Set(true);

	Set_Max_Players(1);
}

//-----------------------------------------------------------------------------
cGameDataSinglePlayer::~cGameDataSinglePlayer(void)
{
   //
}

//-----------------------------------------------------------------------------
cGameDataSinglePlayer& cGameDataSinglePlayer::operator=(const cGameDataSinglePlayer& rhs)
{
	// 
	// Call the base class
	//
	cGameData::operator=(rhs);

	return (*this);
}

//-----------------------------------------------------------------------------
const WCHAR* cGameDataSinglePlayer::Get_Static_Game_Name(void)
{
	return TRANSLATION(IDS_MP_GAME_TYPE_SINGLE_PLAYER);
}

//-----------------------------------------------------------------------------
int cGameDataSinglePlayer::Choose_Player_Type(cPlayer* player, int team_choice, bool is_grunt)
{

	int team;

	if (is_grunt) {
		team = PLAYERTYPE_NOD;
	} else {
		team = PLAYERTYPE_GDI;
	}

	return team;
}
