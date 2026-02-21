// PresetSettingsTab.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "presetsettingstab.h"
#include "definition.h"
#include "preset.h"
#include "specsheet.h"
#include "utils.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
//
// PresetSettingsTabClass
//
/////////////////////////////////////////////////////////////////////////////
PresetSettingsTabClass::PresetSettingsTabClass (PresetClass *preset)
	:	m_Preset (preset),
		m_ParamSheet (NULL),
		m_IsReadOnly (false),
		DockableFormClass (PresetSettingsTabClass::IDD)
{
	//{{AFX_DATA_INIT(PresetSettingsTabClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// ~PresetSettingsTabClass
//
/////////////////////////////////////////////////////////////////////////////
PresetSettingsTabClass::~PresetSettingsTabClass (void)
{
	SAFE_DELETE (m_ParamSheet);
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// PresetSettingsTabClass
//
/////////////////////////////////////////////////////////////////////////////
void
PresetSettingsTabClass::DoDataExchange (CDataExchange *pDX)
{
	DockableFormClass::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(PresetSettingsTabClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(PresetSettingsTabClass, DockableFormClass)
	//{{AFX_MSG_MAP(PresetSettingsTabClass)
	ON_WM_SIZE()
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// PresetSettingsTabClass diagnostics

#ifdef _DEBUG
void PresetSettingsTabClass::AssertValid() const
{
	DockableFormClass::AssertValid();
}

void PresetSettingsTabClass::Dump(CDumpContext& dc) const
{
	DockableFormClass::Dump(dc);
}
#endif //_DEBUG



/////////////////////////////////////////////////////////////////////////////
//
// HandleInitDialog
//
/////////////////////////////////////////////////////////////////////////////
void
PresetSettingsTabClass::HandleInitDialog (void)
{
	ASSERT (m_Preset != NULL);

	DefinitionClass *definition = m_Preset->Get_Definition ();
	if (definition != NULL) {
		
		//
		//	Create a scrollable dialog that contains all the
		//	definition's parameters.
		//
		CRect rect;
		GetClientRect (&rect);
		m_ParamSheet = new SpecSheetClass (definition);
		m_ParamSheet->Set_Is_Temp (m_Preset->Get_IsTemporary ());
		m_ParamSheet->Set_Read_Only (m_IsReadOnly);
		m_ParamSheet->Create ("static", "", WS_CHILD | WS_VISIBLE, rect, this, 101);		
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Apply_Changes
//
/////////////////////////////////////////////////////////////////////////////
bool
PresetSettingsTabClass::Apply_Changes (void)
{
	// Assume success
	bool retval = true;

	DefinitionClass *definition = m_Preset->Get_Definition ();
	if (definition != NULL) {

	}

	m_ParamSheet->Apply ();
	
	// Return true to allow the dialog to close
	return retval;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnSize
//
/////////////////////////////////////////////////////////////////////////////
void
PresetSettingsTabClass::OnSize (UINT nType, int cx, int cy) 
{
	if ((m_ParamSheet != NULL) && (cx > 0) && (cy > 0)) {

		m_ParamSheet->SetWindowPos (NULL, 10, 5, cx-20, cy-10, SWP_NOZORDER);
	}

	DockableFormClass::OnSize (nType, cx, cy);
	return ;
}

