#include "global.h"
#include "mainloop.h"
#include <stdio.h>
#include "sdl2_platform.h"
#include "init.h"
#include "shutdown.h"
#include "timemgr.h"
#include "input.h"
#include "gamemode.h"
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
#include "demosupport.h"

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

static unsigned long s_frame_count = 0;

void _Game_Main_Loop_Loop(void)
{
	s_frame_count++;

	if (s_frame_count == 1) {
		fprintf(stderr, "[loop] First frame reached — main loop is running\n");
	}

	// Periodic status log every 5 seconds (approx 300 frames at 60fps)
	if (s_frame_count % 300 == 0) {
		fprintf(stderr, "[loop] Frame %lu — game loop running\n", s_frame_count);
	}

	unsigned long time1 = TIMEGETTIME();

   TimeManager::Update();

	if (s_frame_count <= 3) {
		fprintf(stderr, "[loop] frame %lu: Input::Update\n", s_frame_count);
	}
   Input::Update();

{
   if (COMBAT_CAMERA != NULL) {
		Vector3 camera_pos = COMBAT_CAMERA->Get_Position();
		PathMgrClass::Resolve_Paths( camera_pos );
	}
}

{
	if (s_frame_count <= 3) {
		fprintf(stderr, "[loop] frame %lu: GameModeManager::Think / GameInitMgrClass::Think\n", s_frame_count);
	}
   GameModeManager::Think();
	GameInitMgrClass::Think();
}

{
	if (s_frame_count <= 3) {
		fprintf(stderr, "[loop] frame %lu: DialogMgrClass::On_Frame_Update\n", s_frame_count);
	}
   DialogMgrClass::On_Frame_Update ();
}

{
   NetworkObjectMgrClass::Think ();
	ServerControl.Service();
}

	//
	// If the following assert hits it may indicate that your
	// working directory pathname got cleared in the project settings.
	//

	if (!GameModeManager::Find("Combat")->Is_Active()) {
		cNetwork::Update();
	}

	if (s_frame_count <= 3) {
		fprintf(stderr, "[loop] frame %lu: GameModeManager::Render\n", s_frame_count);
	}
	GameModeManager::Render();

{
	ConsoleBox.Think();
}

	DEMO_SECURITY_CHECK;

{
	if (!ConsoleBox.Is_Exclusive()) {
		WWAudioClass::Get_Instance ()->On_Frame_Update (0);
	}
}
	// Give the sound manager a chance to think
  // PROFILE(	"Audio", WWAudioClass::Get_Instance ()->On_Frame_Update (0) );

   Windows_Message_Handler();


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
