#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DLGHELPSCREEN_H
#define __DLGHELPSCREEN_H


#include "menudialog.h"
#include "resource.h"


////////////////////////////////////////////////////////////////
//
//	EVAEncyclopediaMenuClass
//
////////////////////////////////////////////////////////////////
class HelpScreenDialogClass : public MenuDialogClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	HelpScreenDialogClass (void);
	~HelpScreenDialogClass (void);


	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	void		On_Init_Dialog (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_Menu_Activate (bool onoff);
	void		On_Destroy (void);
	bool		On_Key_Down (uint32 key_id, uint32 key_data);

	//
	//	Singleton access
	//
	static void									Display (void);

private:
	MenuBackDropClass *	OldBackdrop;
	
};


#endif //__DLGHELPSCREEN_H

