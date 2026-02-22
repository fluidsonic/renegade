#pragma once

#include "global.h"


#include "gamemode.h"

#include "quat.h"

#include "vector3.h"

#include "cdverify.h"
#include "wwstring.h"

// Game Mode to do movies
class	MovieGameModeClass : public GameMajorModeClass, public Observer<CDVerifyEvent>
{
public:
	virtual	const char *Name()	{ return "Movie"; }	// the name of this mode
	virtual	void	Init();		 	// called when the mode is activated
	virtual	void 	Shutdown(); 	// called when the mode is deactivated
	virtual	void 	Think();			// called each time through the main loop
	virtual	void 	Render();			// called each time through the main loop

	void	Start_Movie( const char * filename );

	void	Startup_Movies( void );
	void	Movie_Done( void );

	//
	//	Notification
	//
	void		HandleNotification (CDVerifyEvent &event);

protected:
	void	Play_Movie( const char * filename );

	CDVerifyClass		CDVerifier;
	StringClass			PendingMovieFilename;
	bool					IsPending;
	bool					IsPlaying;
};
