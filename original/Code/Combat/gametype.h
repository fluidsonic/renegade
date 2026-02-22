#pragma once

#include "global.h"

//-----------------------------------------------------------------------------

#define	IS_MISSION				cGameType::Is_Mission()
#define	IS_SKIRMISH				cGameType::Is_Skirmish()
#define	IS_MULTIPLAY			cGameType::Is_Multiplay()
#define	IS_SOLOPLAY				cGameType::Is_Soloplay()

enum GameTypeEnum 
{
	GAMETYPE_NONE,				// Unassigned
	GAMETYPE_MISSION,			// Traditional soloplay
	GAMETYPE_SKIRMISH,		// C&C practice against AI's
	GAMETYPE_MULTIPLAY,		// C&C against humans
};

//-----------------------------------------------------------------------------
class cGameType
{
public:
	static void				Set_Game_Type(GameTypeEnum game_type)	{GameType = game_type;}
	static GameTypeEnum	Get_Game_Type(void)							{return GameType;}

	static bool				Is_Mission(void)								{return GameType == GAMETYPE_MISSION;}
	static bool				Is_Skirmish(void)								{return GameType == GAMETYPE_SKIRMISH;}
	static bool				Is_Multiplay(void)							{return GameType == GAMETYPE_MULTIPLAY;}
	static bool				Is_Soloplay(void)								{return GameType != GAMETYPE_MULTIPLAY;}

private:
	static GameTypeEnum	GameType;
};

//-----------------------------------------------------------------------------
