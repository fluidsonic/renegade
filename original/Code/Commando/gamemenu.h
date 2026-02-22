#pragma once

#include "global.h"


#ifndef	GAMEMODE_H
	#include "gamemode.h"
#endif

/*
** Game Mode to do menus
*/
class	MenuGameModeClass2 : public GameModeClass {

public:
	MenuGameModeClass2 (void);

	virtual	const char *Name()	{ return "Menu"; }	// the name of this mode
	virtual	void	Init();		 	// called when the mode is activated
	virtual	void 	Shutdown(); 	// called when the mode is deactivated
	virtual	void 	Think();			// called each time through the main loop
	virtual	void 	Render();		// called each time through the main loop

	virtual	void Activate();		// activates the mode
	virtual	void Deactivate();	// deactivates the mode (don't shutdown until safe)

private:
	class AudibleSoundClass *	MenuMusic;
};
