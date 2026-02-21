//
// Filename:     devoptions.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef DEVOPTIONS_H
#define DEVOPTIONS_H

#include "regbool.h"
#include "regint.h"
#include "boolean.h"

//-----------------------------------------------------------------------------
//
// Various options used for developing and testing
//
class cDevOptions
{
	public:

#ifdef WWDEBUG
   static cRegistryBool ShowGodStatus;
   static cRegistryBool ShowSoldierData;
   static cRegistryBool ShowVehicleData;
   static cRegistryBool ShowDoorData;
   static cRegistryBool ShowElevatorData;
   static cRegistryBool ShowDSAPOData;
   static cRegistryBool ShowPowerupData;
   static cRegistryBool ShowBuildingData;
   static cRegistryBool ShowSpawnerData;
   static cRegistryBool ShowImportStates;
   static cRegistryBool ShowImportStatesSV;
   static cRegistryBool ShowServerRhostData;
   static cRegistryBool ShowClientRhostData;

	//static cRegistryBool ShowPacketGraphs;

	static cRegistryBool PacketsSentServer;
	static cRegistryBool PacketsSentClient;
	static cRegistryBool PacketsRecdServer;
	static cRegistryBool PacketsRecdClient;
	static cRegistryBool AvgSizePacketsSentServer;
	static cRegistryBool AvgSizePacketsSentClient;
	static cRegistryBool AvgSizePacketsRecdServer;
	static cRegistryBool AvgSizePacketsRecdClient;
	static cRegistryBool BytesSentServer;
	static cRegistryBool BytesSentClient;
	static cRegistryBool BytesRecdServer;
	static cRegistryBool BytesRecdClient;

	static cRegistryBool WwnetPacketsSentServer;
	static cRegistryBool WwnetPacketsSentClient;
	static cRegistryBool WwnetPacketsRecdServer;
	static cRegistryBool WwnetPacketsRecdClient;
	static cRegistryBool WwnetAvgSizePacketsSentServer;
	static cRegistryBool WwnetAvgSizePacketsSentClient;
	static cRegistryBool WwnetAvgSizePacketsRecdServer;
	static cRegistryBool WwnetAvgSizePacketsRecdClient;
	static cRegistryBool WwnetBytesSentServer;
	static cRegistryBool WwnetBytesSentClient;
	static cRegistryBool WwnetBytesRecdServer;
	static cRegistryBool WwnetBytesRecdClient;

   static cRegistryBool ShowPriorities;
   static cRegistryBool ShowBandwidth;
   static cRegistryBool ShowLatency;
   static cRegistryBool ShowLastContact;
   static cRegistryBool ShowListSizes;
   static cRegistryBool ShowListTimes;
   static cRegistryBool ShowListPacketSizes;
   //static cRegistryBool ShowBandwidthBudgetOut;
   static cRegistryBool ShowWatchList;
   static cRegistryBool ShowWolLocation;
	static cRegistryBool ShowDiagnostics;
	static cRegistryBool ShowMenuStack;
	static cRegistryBool ShowIpAddresses;
	static cRegistryBool ShowClientFps;
	static cRegistryBool ShowId;
	static cRegistryBool ShowPing;
	static cRegistryBool ShowObjectTally;
	static cRegistryBool ShowInactivePlayers;
	static cRegistryBool SoundEffectOnAssert;
	static cRegistryBool DisplayLogfileOnAssert;
	static cRegistryBool BreakToDebuggerOnAssert;
	static cRegistryBool ShutdownInputOnAssert;
	static cRegistryBool PreloadAssets;
	static cRegistryBool FilterLevelFiles;
	static cRegistryBool IBelieveInGod;
	static cRegistryBool LogDataSafe;
	static cRegistryBool EnableExceptionHandler;
	static cRegistryBool ShowThumbnailPreInitDialog;
	static cRegistryBool CrtDbgEnabled;
	static cRegistryBool PacketOptimizationsEnabled;
	static cRegistryBool ShowMoney;
	static cRegistryBool ExtraNetDebug;
	static cRegistryBool ExtraModemBandwidthThrottling;
	static cRegistryBool ShowGameSpyAuthState;

   //
   // These are development conveniences for starting a client or server,
   // either from a main menu keypress or from a command line param.
	//

	static cRegistryInt DesiredFrameSleepMs;
	static cRegistryInt SimulatedPacketLossPc;
	static cRegistryInt SimulatedPacketDuplicationPc;
	static cRegistryInt SimulatedLatencyRangeMsLower;
	static cRegistryInt SimulatedLatencyRangeMsUpper;
	static cRegistryInt SpamCount;

	//
	// GoToMainMenu uses QuickFullExit but stops at the main menu.
	//
	static cBoolean GoToMainMenu;

#endif WWDEBUG

   //
   // QuickFullExit is a quick but hopefully clean way to leave combat (via
   // a keypress), and stop execution.
   // If running as a server, clients will be instructed to exit also (debug only).
   //
   static cBoolean QuickFullExit;

	static cRegistryBool ExitThreadOnAssert;
	static cRegistryBool CompareExeVersionOnNetwork;
   static cRegistryBool ShowFps;

	// TEMP. ST - 12/10/2001 3:39PM
	static cRegistryBool UseNewTCADO;

   private:

};

//-----------------------------------------------------------------------------
#endif // DEVOPTIONS_H












	//static cRegistryBool DoThumbnailPreInit;