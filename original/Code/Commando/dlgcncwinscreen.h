#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DLGCNCWINSCREEN_H
#define __DLGCNCWINSCREEN_H


#include "menudialog.h"
#include "player.h"
#include "widestring.h"
#include "render2d.h"
#include "menubackdrop.h"


//////////////////////////////////////////////////////////////////////
//
//	CNCWinScreenMenuClass
//
//////////////////////////////////////////////////////////////////////
class CNCWinScreenMenuClass : public MenuDialogClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	CNCWinScreenMenuClass  (void);
	~CNCWinScreenMenuClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////
	
	//
	//	Inherited
	//
	void		On_Init_Dialog (void);
	void		On_Destroy (void);
	void		Render (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_Menu_Activate (bool onoff);
	void		On_Frame_Update (void);

	//
	//	Control population control
	//
	void		Populate_Player_Lists (int team_id, int list_ctrl_id);
	void		Build_Player_Display_Name (const cPlayer *player, WideStringClass &outName);

	//
	//	Static methods
	//
	static void	Close_Dialog (void);

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////	
	static int __cdecl ListSortCallback (const void *elem1, const void *elem2);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////	
	RenderObjClass *						HeaderModel;
	Render2DClass							BlackoutRenderer;
	MenuBackDropClass						WinScreenBackdrop;
	float										UpdateTimer;
	bool										ShowLadderPoints;
	static CNCWinScreenMenuClass *	_TheInstance;
	static const float					UpdateIntervalS;
};


#endif //__DLGCNCWINSCREEN_H
