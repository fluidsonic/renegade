#include "global.h"
#include "movie.h"
#include "binkmovie.h"
#include "campaign.h"
#include "input.h"
#include "_globals.h"
#include "registry.h"
#include "renegadedialogmgr.h"
#include "gameinitmgr.h"
#include "wwaudio.h"
#include "stylemgr.h"
#include "render2dsentence.h"

enum {
	STARTUP_MOVIE_OFF,
	STARTUP_MOVIE_EA,
	STARTUP_MOVIE_INTRO,
};

int MovieStartupMode = STARTUP_MOVIE_OFF;
bool	IntroMovieSkipAllowed = false;
bool	SkipAllIntroMovies = false;

void	MovieGameModeClass::Init()
{
	BINKMovie::Init();
	IsPending = false;
	IsPlaying = false;

	RegistryClass registry( APPLICATION_SUB_KEY_NAME_OPTIONS );
	if ( registry.Is_Valid() ) {
		IntroMovieSkipAllowed = registry.Get_Bool( "IntroMovieSkipAllowed", false );
		SkipAllIntroMovies = registry.Get_Bool( "SkipAllIntroMovies", false );
		registry.Set_Bool( "SkipAllIntroMovies", SkipAllIntroMovies );
	}

}

void 	MovieGameModeClass::Shutdown()
{
	BINKMovie::Shutdown();
	IsPending = false;
	IsPlaying = false;
}

/*
** called each time through the main loop
*/
void 	MovieGameModeClass::Think()
{
	if ( Is_Active() ) {
		BINKMovie::Update();

		if ( IsPending == false && BINKMovie::Is_Complete() ) {
			Movie_Done();
			return;
		}

		if (( MovieStartupMode == STARTUP_MOVIE_INTRO ) && 
			( IntroMovieSkipAllowed == false )) {
			return;
		}

		bool	leave = Input::Get_State(INPUT_FUNCTION_MENU_TOGGLE);
		static bool was_leave = true;
		if ( leave && !was_leave ) {
			was_leave = leave;		// Weird.  Double looping calls
			Movie_Done();
		}
		was_leave = leave;
	}
}

void 	MovieGameModeClass::Render()
{
	if ( Is_Active() ) {

		if (IsPlaying) {
			if (BINKMovie::Is_Complete () == false) {
				BINKMovie::Render ();
			} else {
				WWAudioClass::Get_Instance ()->Temp_Disable_Audio (false);
				BINKMovie::Stop ();
				IsPlaying = false;
			}
		}
	}
}

void	MovieGameModeClass::Start_Movie( const char * filename )
{
	
	//
	//	Check to see if we should enforce the CD or not...
	//
	bool force_cd = true;


	//
	//	Play the movie (if it exists locally)
	//
	if ( ::GetFileAttributes ( filename ) != 0xFFFFFFFF ) {
		Play_Movie ( filename );
	} else {

		//
		//	Strip any path information off the filename
		//
		StringClass filename_only( filename, true );
		const char *fwd  = ::strrchr( filename, '/' );
		const char *back = ::strrchr( filename, '\\' );
		const char *delimiter = (back > fwd) ? back : fwd;
		if ( delimiter != NULL ) {
			filename_only = delimiter + 1;
		}

		//
		//	Try to find the CD...
		//
		StringClass cd_path;
		if ( CDVerifier.Get_CD_Path( cd_path ) ) {
			
			//
			//	Build a full-path to the movie on the CD
			//
			StringClass full_path = cd_path;
			if ( cd_path[cd_path.Get_Length () - 1] != '/' ) {
				full_path += "/";
			}
			full_path += filename_only;
			Play_Movie( full_path );

		} else if ( force_cd ) {
			// No local file and no CD available (macOS port has no CD drive).
			// Skip the movie rather than hanging waiting for a CD that will never arrive.
			Movie_Done();
		}
	}
}

void	MovieGameModeClass::Play_Movie( const char * filename )
{
	WWAudioClass::Get_Instance ()->Temp_Disable_Audio (true);
	
	FontCharsClass* font = StyleMgrClass::Get_Font(StyleMgrClass::FONT_INGAME_SUBTITLE_TXT);

	BINKMovie::Play( filename, "data/subtitle.ini", font );

	if (font) {
		font->Release_Ref();
	}

	IsPlaying = true;
	return ;
}

void	MovieGameModeClass::HandleNotification (CDVerifyEvent &event)
{
	if ( event.Event() == CDVerifyEvent::VERIFIED ) {

		//
		//	Get the path to the CD...
		//
		StringClass cd_path;
		if ( CDVerifier.Get_CD_Path( cd_path ) ) {
			
			//
			//	Build a full-path to the movie on the CD
			//
			StringClass full_path = cd_path;
			if ( cd_path[cd_path.Get_Length () - 1] != '/' ) {
				full_path += "/";
			}
			full_path += PendingMovieFilename;
			Play_Movie( full_path );
		}		
	} else if ( event.Event() == CDVerifyEvent::NOT_VERIFIED ) {			

		if ( MovieStartupMode == STARTUP_MOVIE_EA || MovieStartupMode == STARTUP_MOVIE_INTRO) {
			MovieStartupMode = STARTUP_MOVIE_OFF;
			
			// Goto main menu
			RenegadeDialogMgrClass::Goto_Location (RenegadeDialogMgrClass::LOC_MAIN_MENU);
			Deactivate();

		} else {
			Movie_Done();
		}
	}

	PendingMovieFilename = "";
	IsPending = false;
	return ;
}

void	MovieGameModeClass::Startup_Movies( void )
{
	if ( SkipAllIntroMovies ) {
		MovieStartupMode = STARTUP_MOVIE_INTRO;
		MovieGameModeClass::Movie_Done();
	} else {
		MovieStartupMode = STARTUP_MOVIE_EA;
		Start_Movie( "DATA/MOVIES/EA_WW.BIK" );		// Play WW/EA movie
	}
}

void	MovieGameModeClass::Movie_Done( void )
{
	if (IsPlaying) {
		WWAudioClass::Get_Instance ()->Temp_Disable_Audio (false);
		BINKMovie::Stop ();
		IsPlaying = false;
	}

	if ( MovieStartupMode == STARTUP_MOVIE_EA ) {
		MovieStartupMode = STARTUP_MOVIE_INTRO;
		Start_Movie( "DATA/MOVIES/R_INTRO.BIK" );		// Play Renegade intro movie
	} else if ( MovieStartupMode == STARTUP_MOVIE_INTRO ) {
		MovieStartupMode = STARTUP_MOVIE_OFF;
		// Goto main menu
		
		RenegadeDialogMgrClass::Goto_Location (RenegadeDialogMgrClass::LOC_MAIN_MENU);

		IntroMovieSkipAllowed = true;

		RegistryClass registry( APPLICATION_SUB_KEY_NAME_OPTIONS );
		if ( registry.Is_Valid() ) {
			registry.Set_Bool( "IntroMovieSkipAllowed", true );
		}

		Deactivate();
	} else {
		CampaignManager::Continue();
	}
}
