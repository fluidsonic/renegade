
#ifndef __RENEGADE_DIALOG_MGR_H
#define __RENEGADE_DIALOG_MGR_H

#include "dialogfactory.h"
#include "dialogresource.h"
#include "wwuiinput.h"
#include "win.h"


////////////////////////////////////////////////////////////////
//	Prototypes
////////////////////////////////////////////////////////////////
int MyLoadStringW (UINT str_id, LPWSTR buffer, int buffer_len);


////////////////////////////////////////////////////////////////
//	Macros
////////////////////////////////////////////////////////////////
/*#define	LOAD_STRING(strobj, strid)	\
	::MyLoadStringW (strid, strobj.Get_Buffer (64), 64);*/


////////////////////////////////////////////////////////////////
//	Constants and globals
////////////////////////////////////////////////////////////////
const int								FACTORY_COUNT	= (DIALOG_LINK_LAST - DIALOG_LINK_FIRST);
extern DialogFactoryBaseClass *	FactoryArray[FACTORY_COUNT];
extern WWUIInputClass *				_TheWWUIInput;


////////////////////////////////////////////////////////////////
//
//	RenegadeDialogMgrClass
//
////////////////////////////////////////////////////////////////
class RenegadeDialogMgrClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constants
	////////////////////////////////////////////////////////////////
	typedef enum
	{
		LOC_MAIN_MENU				= 0,
		LOC_INTERNET_MAIN,
		LOC_INTERNET_GAME_LIST,
		LOC_LAN_MAIN,
		LOC_ENCYCLOPEDIA,
		LOC_OBJECTIVES,
		LOC_MAP,
		LOC_CNC_REFERENCE,
		LOC_LOAD_GAME,
		LOC_IN_GAME_HELP,
		LOC_SPLASH_IN,
		LOC_SPLASH_OUT,
	}	LOCATION;

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	
	//
	//	Initialization
	//
	static void		Initialize (void);
	static void		Shutdown (void);

	//
	//	Dialog creation
	//
	static void		Do_Dialog_By_Button_ID (int button_id);

	//
	//	Dialog creation
	//
	static void		Do_Simple_Dialog (int dlg_res_id);

	//
	//	Menu traversal access
	//
	static void		Goto_Location (LOCATION location);
};


////////////////////////////////////////////////////////////////
//	Inlines
////////////////////////////////////////////////////////////////
inline void
RenegadeDialogMgrClass::Do_Dialog_By_Button_ID (int button_id)
{
	
	//
	//	Start the dialog
	//
	FactoryArray[button_id - DIALOG_LINK_FIRST]->Do_Dialog ();
	return ;
}


#endif //__RENEGADE_DIALOG_MGR_H

