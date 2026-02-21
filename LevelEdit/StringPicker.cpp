#include "stdafx.h"
#include "stringpicker.h"
#include "stringpickermaindialog.h"
#include "translatedb.h"
#include "translateobj.h"


/////////////////////////////////////////////////////////////////////////
//
//	StringPickerClass
//
/////////////////////////////////////////////////////////////////////////
StringPickerClass::StringPickerClass (void)
	:	m_EntryID (0)
{	
	m_Icon = (HICON)::LoadImage (::AfxGetResourceHandle (), MAKEINTRESOURCE (IDI_LIST), IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	~StringPickerClass
//
/////////////////////////////////////////////////////////////////////////
StringPickerClass::~StringPickerClass (void)
{
	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	On_Pick
//
/////////////////////////////////////////////////////////////////////////
void
StringPickerClass::On_Pick (void)
{
	//
	//	Show the dialog to the user so they can pick a string
	//
	StringPickerMainDialogClass dialog (this);
	dialog.Set_String_ID (m_EntryID);
	if (dialog.DoModal () == IDOK) {
		
		m_EntryID = dialog.Get_String_ID ();

		//
		//	Change the text of the window control
		//
		TDBObjClass *object = TranslateDBClass::Find_Object (m_EntryID);
		if (object != NULL) {
			const StringClass &text = object->Get_ID_Desc ();
			SetWindowText (text);
		} else {
			SetWindowText ("");
		}
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	Set_Entry
//
/////////////////////////////////////////////////////////////////////////
void
StringPickerClass::Set_Entry (int entry_id)
{
	m_EntryID = entry_id;

	//
	//	Change the text of the window control
	//
	TDBObjClass *object = TranslateDBClass::Find_Object (entry_id);
	if (object != NULL) {
		const StringClass &text = object->Get_ID_Desc ();
		SetWindowText (text);
	} else {
		SetWindowText ("");
	}

	return ;
}
