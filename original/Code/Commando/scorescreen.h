#pragma once

#include "global.h"


#ifndef	GAMEMODE_H
	#include "gamemode.h"
#endif

#ifndef	__MENU_DIALOG_H
	#include "menudialog.h"
#endif

/*
** Game (Sub) Mode to display ScoreScreen view
*/
class	ScoreScreenGameModeClass : public GameModeClass {
public:
	virtual	const char *Name()	{ return "ScoreScreen"; }	// the name of this mode
	virtual	void	Init();		 	// called when the mode is activated
	virtual	void 	Shutdown() {}	// called when the mode is deactivated
	virtual	void 	Think() {}		// called each time through the main loop
	virtual	void 	Render() {}		// called each time through the main loop

	void			Save_Stats( void );
};

////////////////////////////////////////////////////////////////
//
//	StartSPGameDialogClass
//
////////////////////////////////////////////////////////////////
class ScoreScreenDialogClass : public MenuDialogClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	ScoreScreenDialogClass (void);	
	
	void			On_Init_Dialog (void);
	void			On_Destroy (void);
	void			On_Command (int ctrl_id, int mesage_id, DWORD param);

private:
	bool			ScoreScreenActive;

	int				Get_Time_To_Finish_Stars( void );
	int				Get_Level_Of_Play_Stars( void );
	int				Get_Saves_Loaded_Stars( void );
	int				Get_Secondary_Missions_Stars( void );
};
