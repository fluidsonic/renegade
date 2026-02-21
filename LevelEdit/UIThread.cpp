// UIThread.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "UIThread.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// UIThreadClass

IMPLEMENT_DYNCREATE(UIThreadClass, CWinThread)

UIThreadClass::UIThreadClass()
{
}

UIThreadClass::~UIThreadClass()
{
}

BOOL UIThreadClass::InitInstance()
{
	// TODO:  perform and per-thread initialization here
	return TRUE;
}

int UIThreadClass::ExitInstance()
{
	// TODO:  perform any per-thread cleanup here
	return CWinThread::ExitInstance();
}

BEGIN_MESSAGE_MAP(UIThreadClass, CWinThread)
	//{{AFX_MSG_MAP(UIThreadClass)
		// NOTE - the ClassWizard will add and remove mapping macros here.
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// UIThreadClass message handlers
