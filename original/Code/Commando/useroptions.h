#pragma once

#include "global.h"

//
// Filename:     useroptions.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:
//
//-----------------------------------------------------------------------------


#include "regbool.h"
#include "regint.h"
#include "regfloat.h"
#include "boolean.h"
#include "regstring.h"
#include "bandwidth.h"

//-----------------------------------------------------------------------------
//
// Various options that the player chooses
//
class cUserOptions
{
	public:

		static bool Parse_Command_Line(LPCSTR command);

		static void Set_Server_INI_File(char *cmd_line_entry);

		static void Set_Bandwidth_Type(BANDWIDTH_TYPE_ENUM bandwidth_type);
		static BANDWIDTH_TYPE_ENUM Get_Bandwidth_Type(void);
		static void Set_Bandwidth_Bps(int bandwidth_bbs);

		static void Reread(void);

		static cRegistryBool ShowNamesOnSoldier;
		static cRegistryBool SkipQuitConfirmDialog;
		static cRegistryBool SkipIngameQuitConfirmDialog;
		static cRegistryBool CameraLockedToTurret;
		static cRegistryBool PermitDiagLogging;

		static cRegistryInt Sku;

		static cRegistryInt BandwidthBps;
		static cRegistryInt BandwidthType;

		static cRegistryInt		GameSpyBandwidthType;
		static cRegistryInt		PreferredGameSpyNic;
		static cRegistryString	GameSpyNickname;
		static cRegistryInt		GameSpyGamePort;
		static cRegistryInt		GameSpyQueryPort;
		static cRegistryInt		SplashCount;
		static cRegistryBool	DoneClientBandwidthTest;

		static cRegistryInt PreferredLanNic;

		static cRegistryInt NetUpdateRate;
		static cRegistryFloat ClientHintFactor;
		static cRegistryFloat MaxFacingPenalty;
		static cRegistryFloat IrrelevancePenalty;

		static cRegistryInt ResultsLogNumber;

	private:
};

//-----------------------------------------------------------------------------

/*
		static cRegistryInt	GameListFilterMaxPing;
		static cRegistryInt	GameListFilterMinPlayersPresent;
		static cRegistryInt	GameListFilterMaxPlayersPresent;
		static cRegistryInt	GameListFilterMaxPlayersPermitted;
		static cRegistryBool	GameListFilterShowPrivateGames;
		static cRegistryBool	GameListFilterShowOnlyDedicatedGames;
		static cRegistryBool	GameListFilterShowOnlyGamesIRankFor;
*/
