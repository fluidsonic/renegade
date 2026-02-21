#include "mainloop.h"
#include <stdio.h>
#include "sdl2_platform.h"
#include "init.h"
#include "shutdown.h"
#include "timemgr.h"
#include "input.h"
#include "gamemode.h"
#include "debug.h"
#include "msgloop.h"
#include "cnetwork.h"
#include "miscutil.h"
//#include "gamesettings.h"
#include "WWAudio.H"
#include "devoptions.h"
#include "multihud.h"
#include "gamedata.h"
#include "diagnostics.h"
#include "crandom.h"
#include "dialogmgr.h"
#include "ccamera.h"
#include "pathmgr.h"
#include "networkobjectmgr.h"
#include "gameinitmgr.h"
#include "servercontrol.h"
#include "consolemode.h"
#include "gamespyadmin.h"
#include "demosupport.h"
#include "GameSpy_QnR.h"


/*
**
*/
bool	RunMainLoop = true;
int		ExitCode = EXIT_SUCCESS;

void Stop_Main_Loop(int exitCode)
{
	RunMainLoop = false;
	ExitCode = exitCode;
}


void _Game_Main_Loop_Loop(void)
{

	unsigned long time1 = TIMEGETTIME();

   TimeManager::Update();

   Input::Update();


{	WWPROFILE( "Pathfind Evaluate" );
   if (COMBAT_CAMERA != NULL) {
		Vector3 camera_pos = COMBAT_CAMERA->Get_Position();
		PathMgrClass::Resolve_Paths( camera_pos );
	}
}

{	WWPROFILE( "Think" );
   GameModeManager::Think();
	GameInitMgrClass::Think();
}

{	WWPROFILE( "Dialog Mgr Update" );
   DialogMgrClass::On_Frame_Update ();
}

{	WWPROFILE( "Network Object Mgr Think" );
   NetworkObjectMgrClass::Think ();
	ServerControl.Service();
}

{	WWPROFILE("GameSpy_QnR");
	GameSpyQnR.Think();
}

	if (cGameSpyAdmin::Is_Gamespy_Game()) {
		cGameSpyAdmin::Think();
	}

	//
	// If the following assert hits it may indicate that your
	// working directory pathname got cleared in the project settings.
	//

	if (!GameModeManager::Find("Combat")->Is_Active()) {
		cNetwork::Update();
	}

	GameModeManager::Render();


{	WWPROFILE("ConsoleBox");
	ConsoleBox.Think();
}

	DEMO_SECURITY_CHECK;

{	WWPROFILE( "Audio" );
	if (!ConsoleBox.Is_Exclusive()) {
		WWAudioClass::Get_Instance ()->On_Frame_Update (0);
	}
}
	// Give the sound manager a chance to think
  // PROFILE(	"Audio", WWAudioClass::Get_Instance ()->On_Frame_Update (0) );

   Windows_Message_Handler();

#if 0
{	WWPROFILE( "Random" );
	// spin the Random Generator, a little
	int count = FreeRandom.Get_Int( 5 );
	while ( count-- > 0 ) {
		FreeRandom.Get_Int();
	}
}
#endif

   DebugManager::Update();


	/*
	** Sleep for a while if we are hogging the CPU.
	*/
	if (cNetwork::I_Am_Only_Server()) {
		unsigned long time2 = TIMEGETTIME();
		if (time2 >= time1) {

			/*
			** 16 (approx) for 60 fps. (1000/60)
			*/
			unsigned long diff = time2 - time1;
			if (diff < 16) {
				unsigned long sleep_time = 16 - (time2 - time1);
				Sleep(sleep_time);
			}
		}
	}
}

/*
** MAIN GAME LOOP
*/
int Game_Main_Loop(void)
{
	const unsigned long servicetime = 1000; // Time in milliseconds.

	unsigned long time;

	// Only run main loop if the init is succesful!
	fprintf(stderr, "[trace] Calling Game_Init()...\n");
	bool init_ok = Game_Init();
	fprintf(stderr, "[trace] Game_Init() returned %s\n", init_ok ? "true" : "false");
	if (init_ok) {
		fprintf(stderr, "[trace] Entering main loop\n");
		while ( RunMainLoop ) {
			// Check SDL2 quit request (window close, Cmd-Q, etc.)
			if (SDL2_QuitRequested) {
				Stop_Main_Loop(0);
				break;
			}
			_Game_Main_Loop_Loop();
		}
		fprintf(stderr, "[trace] Exited main loop (ExitCode=%d)\n", ExitCode);

		// IML: Allow a short period to process any outstanding sound effects before shutdown.
		time = TIMEGETTIME();
		while (TIMEGETTIME() - time < servicetime) {
			WWAudioClass::Get_Instance ()->On_Frame_Update (0);
		}

		Game_Shutdown();
		fprintf(stderr, "[trace] Game_Shutdown() complete\n");
	}

	return ExitCode;
}