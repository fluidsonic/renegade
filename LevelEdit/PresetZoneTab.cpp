// PresetGeneralTab.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "presetzonetab.h"
#include "definition.h"
#include "preset.h"
#include "utils.h"
#include "zoneeditdialog.h"
#include "objectnode.h"
#include "rendobj.h"
#include "SceneEditor.h"
#include "decophys.h"
#include "CollisionGroups.h"
#include "staticphys.h"
#include "zone.h"
#include "wheelvehicle.h"
#include "cameramgr.h"
#include "hlod.h"
#include "parameter.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// Constants
/////////////////////////////////////////////////////////////////////////////
const float DROP_POS			= 5.0F;
const float DROP_HEIGHT		= 5.0F;

const int COL_NAME			= 0;


/////////////////////////////////////////////////////////////////////////////
//
// PresetZoneTabClass
//
/////////////////////////////////////////////////////////////////////////////
PresetZoneTabClass::PresetZoneTabClass (PresetClass *preset)
	:	m_Preset (preset),
		m_PhysObj (NULL),
		m_ZoneList (NULL),
		DockableFormClass(PresetZoneTabClass::IDD)
{
	//{{AFX_DATA_INIT(PresetZoneTabClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// ~PresetZoneTabClass
//
/////////////////////////////////////////////////////////////////////////////
PresetZoneTabClass::~PresetZoneTabClass (void)
{
	MEMBER_RELEASE (m_PhysObj);
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// DoDataExchange
//
/////////////////////////////////////////////////////////////////////////////
void
PresetZoneTabClass::DoDataExchange (CDataExchange *pDX)
{
	DockableFormClass::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(PresetZoneTabClass)
	DDX_Control(pDX, IDC_ZONE_LIST, m_ListCtrl);
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(PresetZoneTabClass, DockableFormClass)
	//{{AFX_MSG_MAP(PresetZoneTabClass)
	ON_NOTIFY(NM_DBLCLK, IDC_ZONE_LIST, OnDblclkZoneList)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

#ifdef _DEBUG
void PresetZoneTabClass::AssertValid() const
{
	DockableFormClass::AssertValid();
}

void PresetZoneTabClass::Dump(CDumpContext& dc) const
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
PresetZoneTabClass::HandleInitDialog (void)
{
	ASSERT (m_Preset != NULL);

	ListView_SetExtendedListViewStyle (m_ListCtrl, LVS_EX_FULLROWSELECT);
	m_ListCtrl.InsertColumn (COL_NAME, "Name");

	//
	//	Add the zones to the list ctrl
	//
	int count = m_ZoneList->Count ();
	for (int index = 0; index < count; index ++) {
				
		//
		//	Insert this zone into the control
		//
		ZoneParameterClass *parameter = (*m_ZoneList)[index];
		if (parameter != NULL) {
			m_ListCtrl.InsertItem (index, parameter->Get_Name (), 0);
		}
	}

	//
	//	Size the columns
	//
	CRect rect;
	m_ListCtrl.GetWindowRect (&rect);
	m_ListCtrl.SetColumnWidth (COL_NAME, (rect.Width ()) - (::GetSystemMetrics (SM_CXVSCROLL) + 1));
	m_ListCtrl.SetFocus ();
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Apply_Changes
//
/////////////////////////////////////////////////////////////////////////////
bool
PresetZoneTabClass::Apply_Changes (void)
{
	return true;
}


/////////////////////////////////////////////////////////////////////////////
//
// Modify_Selected_Zone
//
/////////////////////////////////////////////////////////////////////////////
void
PresetZoneTabClass::Modify_Selected_Zone (void)
{
	Create_Phys_Obj ();
	if (m_PhysObj != NULL) {

		//
		//	Get the currently selected item
		//
		int index = m_ListCtrl.GetNextItem (-1, LVNI_ALL | LVNI_SELECTED);
		if (index >= 0) {

			ZoneParameterClass *parameter = (*m_ZoneList)[index];
			if (parameter != NULL) {
				
				//
				//	Show the zone edit dialog
				//
				ZoneEditDialogClass dialog (this);
				dialog.Set_Phys_Obj (m_PhysObj);
				dialog.Set_Zone (parameter->Get_Zone ());
				if (dialog.DoModal () == IDOK) {
					parameter->Set_Zone (dialog.Get_Zone ());
				}
			}
		}
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Create_Phys_Obj
//
/////////////////////////////////////////////////////////////////////////////
void
PresetZoneTabClass::Create_Phys_Obj (void)
{
	CWaitCursor wait_cursor;

	if (m_PhysObj == NULL && m_Preset != NULL) {

		//
		//	Instantiate the node
		//
		NodeClass *node = (NodeClass *)m_Preset->Create ();
		ASSERT (node != NULL);
		if (node != NULL) {
			
			//
			//	Initialize the node and get its phys object pointer
			//
			node->Initialize ();			
			PhysClass *phys_obj = node->Peek_Physics_Obj ();
			ASSERT (phys_obj != NULL);
			if (phys_obj != NULL) {

				//
				//	Keep a copy of the physics object for our own uses.
				//
				m_PhysObj = phys_obj;
				m_PhysObj->Add_Ref ();

				//
				//	Force the object to use its highest LOD
				//
				/*if (m_RenderObj->Class_ID () == RenderObjClass::CLASSID_HLOD) {
					HLodClass *lod_obj = (HLodClass *)m_RenderObj;
					lod_obj->Set_LOD_Level (lod_obj->Get_LOD_Count () - 1);
				}*/
			}

			MEMBER_RELEASE (node);
		}		
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnDblclkZoneList
//
/////////////////////////////////////////////////////////////////////////////
void
PresetZoneTabClass::OnDblclkZoneList
(
	NMHDR *	pNMHDR,
	LRESULT *pResult
)
{
	(*pResult) = 0;
	Modify_Selected_Zone ();
	return ;
}
