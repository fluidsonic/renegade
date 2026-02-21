
#ifndef __DLGCNCREFERENCE_H
#define __DLGCNCREFERENCE_H

#include "menudialog.h"
#include "dlgmessagebox.h"

//////////////////////////////////////////////////////////////////////
//
//	CnCReferenceMenuClass
//
//////////////////////////////////////////////////////////////////////
class CnCReferenceMenuClass : public MenuDialogClass, public Observer<DlgMsgBoxEvent>
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	CnCReferenceMenuClass  (void);
	~CnCReferenceMenuClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////
	void		On_Init_Dialog (void);
	void		On_Destroy (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_Menu_Activate (bool onoff);
	void		On_Frame_Update(void);

	//
	//	Singleton access
	//
	static void								Display (void);
	static CnCReferenceMenuClass *	Get_Instance (void)	{ return _TheInstance; }

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////	
	void				Prompt_User (void);
	void				HandleNotification (DlgMsgBoxEvent &event);
	void				Exit_Game (void);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////	
	static CnCReferenceMenuClass *	_TheInstance;
	MenuBackDropClass *					OldBackdrop;
	float										Timer;

	enum				{ACTION_TIMEOUT_MS = 10000};
	static DWORD	LastSuicideTimeMs;
	static DWORD	LastChangeTeamTimeMs;
};

#endif //__DLGCNCREFERENCE_H
