// ZoneInfoPage.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "ZoneInfoPage.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
// ZoneInfoPageClass
//
ZoneInfoPageClass::ZoneInfoPageClass ()
	: m_pZone (NULL),
	  DockableFormClass(ZoneInfoPageClass::IDD)
{
	//{{AFX_DATA_INIT(ZoneInfoPageClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// ZoneInfoPageClass
//
ZoneInfoPageClass::ZoneInfoPageClass (ZoneInstanceClass *pzone)
	: m_pZone (pzone),
	DockableFormClass(ZoneInfoPageClass::IDD)
{
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// ~ZoneInfoPageClass
//
ZoneInfoPageClass::~ZoneInfoPageClass ()
{
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// DoDataExchange
//
void
ZoneInfoPageClass::DoDataExchange (CDataExchange* pDX)
{
	DockableFormClass::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(ZoneInfoPageClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(ZoneInfoPageClass, DockableFormClass)
	//{{AFX_MSG_MAP(ZoneInfoPageClass)
		// NOTE - the ClassWizard will add and remove mapping macros here.
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// ZoneInfoPageClass diagnostics

#ifdef _DEBUG
void ZoneInfoPageClass::AssertValid() const
{
	DockableFormClass::AssertValid();
}

void ZoneInfoPageClass::Dump(CDumpContext& dc) const
{
	DockableFormClass::Dump(dc);
}
#endif //_DEBUG


/////////////////////////////////////////////////////////////////////////////
//
// HandleInitDialog
//
void
ZoneInfoPageClass::HandleInitDialog (void)
{
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Apply_Changes
//
bool
ZoneInfoPageClass::Apply_Changes (void)
{
	// Return true to allow the dialog to close
	return true;
}
