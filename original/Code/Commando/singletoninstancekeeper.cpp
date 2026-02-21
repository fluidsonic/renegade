#include "singletoninstancekeeper.h"
#include "wwdebug.h"
#include "autostart.h"


//
// Disable 'unreferenced inline function has been removed'
//
#pragma warning(disable : 4514)


//////////////////////////////////////////////////////////////////////
//	Local constants
//////////////////////////////////////////////////////////////////////
static const char *	AUTOPLAY_GUID = "01AF9993-3492-11d3-8F6F-0060089C05B1";


//////////////////////////////////////////////////////////////////////
//	Static member initialization
//////////////////////////////////////////////////////////////////////
const char *	SingletonInstanceKeeperClass::APP_GUID = "C6D925A3-7A9B-4ca3-866D-8B4D506C3665";

bool SingletonInstanceKeeperClass::AllowMultipleInstances = false;


//////////////////////////////////////////////////////////////////////
//
//	SingletonInstanceKeeperClass
//
//////////////////////////////////////////////////////////////////////
SingletonInstanceKeeperClass::SingletonInstanceKeeperClass (void)	:
	AppMutex (NULL),
	AutoPlayMutex (NULL)
{
	if (AutoRestart.Get_Restart_Flag()) {
		AllowMultipleInstances = true;
	}
	return;
}


//////////////////////////////////////////////////////////////////////
//
//	SingletonInstanceKeeperClass
//
//////////////////////////////////////////////////////////////////////
SingletonInstanceKeeperClass::~SingletonInstanceKeeperClass (void)
{
	if (AppMutex != NULL) {
		::CloseHandle (AppMutex);
	}

	if (AutoPlayMutex != NULL) {
		::CloseHandle (AutoPlayMutex);
	}

	return;
}


//////////////////////////////////////////////////////////////////////
//
//	Verify_Safe_To_Execute
//
//////////////////////////////////////////////////////////////////////
bool
SingletonInstanceKeeperClass::Verify_Safe_To_Execute (void)
{
	bool retval = false;

	//
	// Create a mutex with a unique name to Renegade in order to determine if
	// our app is already running.
	//
	// WARNING: DO NOT use this number for any other application except Renegade
	//
	AppMutex = ::CreateMutex (NULL, FALSE, APP_GUID);

	//
	//	Is there already an instance of this app somewhere?
	//
	if (::GetLastError () == ERROR_ALREADY_EXISTS) {

		if (AllowMultipleInstances) {
			WWDEBUG_SAY (("Renegade is already running but AllowMultipleInstances is true\n"));
			retval = true;
		} else {
			//
			//	Find the previous instance
			//
			HWND main_wnd = ::FindWindow (APP_GUID, NULL);
			if (main_wnd != NULL) {
				::SetForegroundWindow (main_wnd);
				::ShowWindow (main_wnd, SW_RESTORE);
			}

			WWDEBUG_SAY (("Renegade is already running...Bail!\n"));
		}
	} else {

		WWDEBUG_SAY (("Create AppMutex okay.\n"));

		//
		// Obtain the mutex unique to the Renegade AutoPlay application.
		//
		// WARNING: DO NOT use this number for any other application except Renegade AutoPlay
		//
		do
		{
			//
			//	Attempt to open the mutex
			//
			AutoPlayMutex = ::OpenMutex (MUTEX_ALL_ACCESS, FALSE, AUTOPLAY_GUID);
			if (AutoPlayMutex != NULL) {
				WWDEBUG_SAY (("Waiting for Autoplay to quit!\n"));

				//
				//	Wait for up to 30 seconds for the autoplay app to close
				//
				if (::WaitForSingleObject (AutoPlayMutex, 30000) == WAIT_FAILED) {
					WWDEBUG_SAY (("Failed waiting for AutoPlayMutex\n"));
					::CloseHandle (AutoPlayMutex);
					AutoPlayMutex = NULL;
				}
			}

			//
			// Create a mutex with a name unique to the Renegade AutoPlay application.
			// This prevents the autoplay from running since it cannot get the mutex.
			// Renegade needs both of these mutexs before it is allowed to run.
			//
			if (AutoPlayMutex == NULL) {
				AutoPlayMutex = ::CreateMutex (NULL, FALSE, AUTOPLAY_GUID);

				if (::GetLastError () == ERROR_ALREADY_EXISTS) {
					::CloseHandle (AutoPlayMutex);
					AutoPlayMutex = NULL;
					::Sleep (2500);
				} else {
					WWDEBUG_SAY (("Create AutoPlayMutex.\n"));
				}
			}

		} while (AutoPlayMutex == NULL);

		WWDEBUG_SAY (("Got AutoPlayMutex okay.\n"));
		retval = true;
	}

	return retval;
}





//////////////////////////////////////////////////////////////////////
//
//	Allow_Multiple_Instances
//
//////////////////////////////////////////////////////////////////////
void SingletonInstanceKeeperClass::Allow_Multiple_Instances(bool flag)
{
	AllowMultipleInstances = flag;
}