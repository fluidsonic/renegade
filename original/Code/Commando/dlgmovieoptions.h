#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DLGOPTIONSMOVIE_H
#define __DLGOPTIONSMOVIE_H


#include "menudialog.h"
#include "resource.h"
#include "cdverify.h"


////////////////////////////////////////////////////////////////
//
//	MovieOptionsMenuClass
//
////////////////////////////////////////////////////////////////
class MovieOptionsMenuClass : public MenuDialogClass, public Observer<CDVerifyEvent>
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	MovieOptionsMenuClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	
	//
	//	Inherited
	//
	void		On_Init_Dialog (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_Frame_Update (void);
	void		Render (void);
	void		On_ListCtrl_Delete_Entry (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_ListCtrl_DblClk (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	bool		On_Key_Down (uint32 key_id, uint32 key_data);

	//
	//	Notification
	//
	void		HandleNotification (CDVerifyEvent &event);

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////
	void		Begin_Play_Movie (void);
	void		Play_Movie (const char *filename);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	bool				IsPlaying;
	CDVerifyClass	CDVerifier;
	StringClass		PendingMovieFilename;
};


#endif //__DLGOPTIONSMOVIE_H

