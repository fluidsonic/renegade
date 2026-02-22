#include "singletoninstancekeeper.h"

//
// Disable 'unreferenced inline function has been removed'
//

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

		}
	} else {

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

				//
				//	Wait for up to 30 seconds for the autoplay app to close
				//
				if (::WaitForSingleObject (AutoPlayMutex, 30000) == WAIT_FAILED) {
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
				}
			}

		} while (AutoPlayMutex == NULL);

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