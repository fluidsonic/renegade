#include "stdafx.h"
#include "Splash.h"
#include "SplashScreen.h"
#include "Utils.h"


///////////////////////////////////////////////////////////////////////
//	Forward declarations
///////////////////////////////////////////////////////////////////////
UINT fnSplashScreenThread (DWORD, DWORD, DWORD, HRESULT *, HWND *phmain_wnd);


///////////////////////////////////////////////////////////////////////
//
//	SplashClass
//
///////////////////////////////////////////////////////////////////////
SplashClass::SplashClass (void)
	:	m_hThreadWnd (NULL)
{
	return ;
}


///////////////////////////////////////////////////////////////////////
//
//	~SplashClass
//
///////////////////////////////////////////////////////////////////////
SplashClass::~SplashClass (void)
{
	Close ();
	return ;
}


///////////////////////////////////////////////////////////////////////
//
//	Set_Status_Text
//
///////////////////////////////////////////////////////////////////////
void
SplashClass::Set_Status_Text (LPCTSTR text)
{
	if (m_hThreadWnd != NULL) {
		::PostMessage (m_hThreadWnd, WM_USER+102, 0, (LPARAM)text);
	}

	return ;
}


///////////////////////////////////////////////////////////////////////
//
//	Show
//
///////////////////////////////////////////////////////////////////////
void
SplashClass::Show (bool show)
{
	//
	//	Kick off a thread that will display the splash screen
	//
	if (m_hThreadWnd == NULL) {
		::Create_UI_Thread (fnSplashScreenThread, 0, 0, 0, NULL, &m_hThreadWnd);
	}

	::ShowWindow (m_hThreadWnd, show ? SW_SHOW : SW_HIDE);

	//
	//	Register ourselves as the current status window
	//
	if (show) {
		ProgressUIMgrClass::Set_Current_Progress_UI (this);
	}

	return ;
}


///////////////////////////////////////////////////////////////////////
//
//	Close
//
///////////////////////////////////////////////////////////////////////
void
SplashClass::Close (void)
{
	//
	// Close out the window
	//
	if (::IsWindow (m_hThreadWnd)) {
		::PostMessage (m_hThreadWnd, WM_USER+101, 0, 0L);
		m_hThreadWnd = NULL;
	}

	if (ProgressUIMgrClass::Get_Current_Progress_UI () == this) {
		ProgressUIMgrClass::Set_Current_Progress_UI (NULL);
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// fnSplashScreenThread
//
////////////////////////////////////////////////////////////////////////////
UINT
fnSplashScreenThread
(
	DWORD /*dwparam1*/,
	DWORD /*dwparam2*/,
	DWORD /*dwparam3*/,
	HRESULT* /*presult*/,
	HWND* phmain_wnd
)
{
	//
	// Create a new instance of the 'updating' dialog
	//
	SplashScreenClass *splash_wnd = new SplashScreenClass;
	splash_wnd->CreateEx (0, "STATIC", "Level Edit", WS_POPUP | WS_VISIBLE, 0, 0, 100, 100, NULL, NULL);
	splash_wnd->ShowWindow (SW_SHOW);

	//
	// Return the window handle of the main wnd to the calling thread
	//
	if (phmain_wnd != NULL) {
		(*phmain_wnd) = splash_wnd->m_hWnd;
	}

	return 1;
}
