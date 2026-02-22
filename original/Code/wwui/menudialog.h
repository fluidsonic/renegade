#pragma once

#include "global.h"

#include "dialogbase.h"
#include "wwstring.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class MenuBackDropClass;

////////////////////////////////////////////////////////////////
//
//	MenuDialogClass
//
////////////////////////////////////////////////////////////////
class MenuDialogClass : public DialogBaseClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	MenuDialogClass (int res_id);
	virtual ~MenuDialogClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	static void						Initialize (void);
	static void						Shutdown (void);

	//
	//	RTTI
	//
	virtual MenuDialogClass *	As_MenuDialogClass (void)	{ return this; }

	//
	//	Configuration methods
	//
	static MenuBackDropClass *	Get_BackDrop (void)		{ return BackDrop; }
	static MenuBackDropClass *	Replace_BackDrop (MenuBackDropClass *backdrop);

	//
	//	Informational
	//
	bool						Is_Active_Menu (void) const	{ return (ActiveMenu == this); }

	//
	//	From DialogBaseClass
	//
	virtual void			Start_Dialog (void);
	virtual void			Render (void);
	virtual void			End_Dialog (void);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////

	//
	//	From DialogBaseClass
	//
	virtual void			On_Activate (bool onoff);

	//
	//	New notifications
	//
	virtual void			On_Menu_Activate (bool onoff);
	virtual void			On_Last_Menu_Ending (void) {}

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////	
	static MenuDialogClass *								ActiveMenu;
	static MenuBackDropClass *								BackDrop;
	static DynamicVectorClass<MenuDialogClass *>		MenuStack;
};
