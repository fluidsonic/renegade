#include "stdafx.h"
#include "presetpicker.h"
#include "selectpresetdialog.h"
#include "preset.h"
#include "resource.h"


/////////////////////////////////////////////////////////////////////////
//
//	PresetPickerClass
//
/////////////////////////////////////////////////////////////////////////
PresetPickerClass::PresetPickerClass (void)
	:	m_Preset (NULL),
		m_ClassID (0),
		m_IconIndex (0)
{	
	m_Icon = (HICON)::LoadImage (::AfxGetResourceHandle (), MAKEINTRESOURCE (IDI_PRESET_TINY), IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	~PresetPickerClass
//
/////////////////////////////////////////////////////////////////////////
PresetPickerClass::~PresetPickerClass (void)
{
	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	On_Pick
//
/////////////////////////////////////////////////////////////////////////
void
PresetPickerClass::On_Pick (void)
{
	//
	//	Show the dialog to the user so they can pick a preset
	//
	SelectPresetDialogClass dialog (this);
	dialog.Set_Class_ID (m_ClassID);
	dialog.Set_Icon_Index (m_IconIndex);
	dialog.Set_Preset (m_Preset);
	if (dialog.DoModal () == IDOK) {
		
		PresetClass *preset = dialog.Get_Selection ();
		Set_Preset (preset);		
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	Set_Preset
//
/////////////////////////////////////////////////////////////////////////
void
PresetPickerClass::Set_Preset (PresetClass *preset)
{
	//
	//	Build a new text string to display
	//
	CString new_text;
	if (preset != NULL) {
		new_text = preset->Get_Name ();
	}

	//
	//	Put the new text into the control
	//
	SetWindowText (new_text);

	m_Preset = preset;	
	return ;
}

