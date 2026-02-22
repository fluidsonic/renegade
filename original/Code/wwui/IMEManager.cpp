#include "global.h"
#include "IMEManager.h"
#include "WWString.h"
#include <locale.h>
#include <mbctype.h>

#include <algorithm>
#include <memory>

#pragma comment(lib, "imm32.lib")

namespace IME {

/******************************************************************************
*
* NAME
*     IMEManager::Create
*
* DESCRIPTION
*     Create an IME manager instance
*
* INPUTS
*     Window - HWND to associate IME manager instance with.
*
* RESULT
*     IMEManager - Pointer to IME manager
*
******************************************************************************/

IMEManager* IMEManager::Create(HWND hwnd)
	{
	if (hwnd)
		{
		IMEManager* ime = new IMEManager;

		if (ime)
			{
			if (ime->FinalizeCreate(hwnd))
				{
				return ime;
				}

			ime->Release_Ref();
			}
		}

	return NULL;
	}

/******************************************************************************
*
* NAME
*     IMEManager::IMEManager
*
* DESCRIPTION
*     Constructor
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

IMEManager::IMEManager() :
		mHWND(NULL),
		mHIMC(NULL),
		mDisabledHIMC(NULL),
		mDisableCount(0),
		mCodePage(CP_ACP),
		mIMEProperties(0),
		mHilite(true),
		mStartCandListFrom1(true),
		mOSCanUnicode(false),
		mUseUnicode(false),
		mInComposition(false)
	{

	mLangID = MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT);

	ResetComposition();
	mResultString[0] = 0;
	}

/******************************************************************************
*
* NAME
*     IMEManager::~IMEManager
*
* DESCRIPTION
*     Destructor
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

IMEManager::~IMEManager()
	{

	if (mHIMC)
		{
		ImmAssociateContext(mHWND, mDefaultHIMC);
		ImmDestroyContext(mHIMC);
		}

	mCandidateColl.clear();
	}

/******************************************************************************
*
* NAME
*     IMEManager::FinalizeCreate
*
* DESCRIPTION
*     Post creation finalization.
*
* INPUTS
*     HWND - Window to associate IME context with.
*
* RESULT
*     True if successful
*
******************************************************************************/

bool IMEManager::FinalizeCreate(HWND hwnd)
	{
	if (hwnd == NULL)
		{
		return false;
		}

	mHWND = hwnd;

	// Check the OS version, if Win98 or better then we can use unicode
	OSVERSIONINFO osvi;
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
	GetVersionEx(&osvi);

	bool isWin98orLater = (osvi.dwPlatformId == VER_PLATFORM_WIN32_WINDOWS) && ((osvi.dwMajorVersion > 4) || ((osvi.dwMajorVersion == 4) && (osvi.dwMinorVersion >= 10)));
	bool isNT4orLater = (osvi.dwPlatformId == VER_PLATFORM_WIN32_NT) && ((osvi.dwMajorVersion > 4) || ((osvi.dwMajorVersion == 4) && (osvi.dwMinorVersion >= 0)));
	mOSCanUnicode = (isWin98orLater || isNT4orLater);

	// Create new input context for the specified window.
	mHIMC = ImmCreateContext();
	
	if (mHIMC == NULL)
		{
		return false;
		}

	// Associate the new context with this window.
	mDefaultHIMC = ImmAssociateContext(mHWND, mHIMC);

	// Set the language for the current keyboard layout.
	InputLanguageChanged(GetKeyboardLayout(0));

	mCandidateColl.resize(32);

	return true;
	}

/******************************************************************************
*
* NAME
*     IMEManager::Activate
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void IMEManager::Activate(void)
	{

	HIMC imc = ImmGetContext(mHWND);

	if (imc)
		{
		BOOL open = ImmGetOpenStatus(imc);

		if (!open)
			{
			ImmSetOpenStatus(imc, TRUE);

			IMEEvent action(IME_ACTIVATED, this);
			NotifyObservers(action);
			}

		ImmReleaseContext(mHWND, imc);
		}
	}

/******************************************************************************
*
* NAME
*     IMEManager::Deactivate
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void IMEManager::Deactivate(void)
	{

	HIMC imc = ImmGetContext(mHWND);

	if (imc)
		{
		BOOL open = ImmGetOpenStatus(imc);

		if (open)
			{
			ImmSetOpenStatus(imc, FALSE);

			IMEEvent action(IME_DEACTIVATED, this);
			NotifyObservers(action);
			}

		ImmReleaseContext(mHWND, imc);
		}

	ResetComposition();
	}

/******************************************************************************
*
* NAME
*     IMEManager::IsActive
*
* DESCRIPTION
*     Check if IME is active.
*
* INPUTS
*     NONE
*
* RESULT
*     True if IME is currently active.
*
******************************************************************************/

bool IMEManager::IsActive(void) const
	{
	bool isActive = false;

	HIMC imc = ImmGetContext(mHWND);

	if (imc)
		{
		BOOL open = ImmGetOpenStatus(imc);
		isActive = (open != 0);
		ImmReleaseContext(mHWND, imc);
		}

	return isActive;
	}

/******************************************************************************
*
* NAME
*     IMEManager::Disable
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void IMEManager::Disable(void)
	{
	++mDisableCount;

	// If this is the first disable lock the perform the actuall disabling.
	if (1 == mDisableCount)
		{
		mDisabledHIMC = ImmAssociateContext(mHWND, NULL);

		IMEEvent action(IME_DISABLED, this);
		NotifyObservers(action);
		}
	}

/******************************************************************************
*
* NAME
*     IMEManager::Enable
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void IMEManager::Enable(void)
	{
	if (mDisableCount > 0)
		{
		--mDisableCount;

		// Re-enable when there is no disable locks.
		if (0 == mDisableCount)
			{
			ImmAssociateContext(mHWND, mDisabledHIMC);
			mDisabledHIMC = NULL;

			IMEEvent action(IME_ENABLED, this);
			NotifyObservers(action);
			}
		}
	}

/******************************************************************************
*
* NAME
*     IMEManager::IsDisabled
*
* DESCRIPTION
*     Check if IME is turned off.
*
* INPUTS
*     NONE
*
* RESULT
*     True if IME is currently off.
*
******************************************************************************/

bool IMEManager::IsDisabled(void) const
	{
	return (mDisableCount > 0);
	}

/******************************************************************************
*
* NAME
*     IMEManager::ProcessMessage
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*     True of message handled and therefore should NOT be passed to the
*     default window procedure.
*
******************************************************************************/

bool IMEManager::ProcessMessage(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam, LRESULT& outResult)
	{
	if (hwnd != mHWND)
		{
		return false;
		}

	bool handled = true;
	outResult = 0;

	switch (msg)
		{
		// Request New keyboard layout and / or Input method
		case WM_INPUTLANGCHANGEREQUEST:
			{
			HKL layout = InputLanguageChangeRequest((HKL)lParam);

			if (layout)
				{
				lParam = (LPARAM)layout;
				handled = false;
				}
			}
			break;

		// Input language has changed.
		case WM_INPUTLANGCHANGE:
			InputLanguageChanged((HKL)lParam);
			outResult = TRUE;
			handled = false;
			break;

		// Sent when the system is about to change the current IME.
		case WM_IME_SELECT:
			break;

		// We will handle all of the UI so clear all of the flags.
		case WM_IME_SETCONTEXT:
			lParam &= ~(ISC_SHOWUIALL);
			handled = false;
			break;

		// Sent when a composition string is about to be generated in response to a
		// keystroke. This message informs us to prepare for composition.
		case WM_IME_STARTCOMPOSITION:
			StartComposition();
			break;

		// Sent when composition status has changed in response to a keystroke.
		case WM_IME_COMPOSITION:
			DoComposition(wParam, lParam);
			break;

		// Sent when composition has closed.
		case WM_IME_ENDCOMPOSITION:
			EndComposition();
			break;

		// Sent when unable to extend the composition to accomodate any more characters.
		case WM_IME_COMPOSITIONFULL:
			{
			CompositionEvent event(COMPOSITION_FULL, this);
			NotifyObservers(event);
			}
			break;

		// Sent when the IME status has changed.
		case WM_IME_NOTIFY:
			outResult = IMENotify(wParam, lParam);
			break;

		// IMEs send this message when the user accepts the conversion string.
		// wParam contains a single-byte or double-byte character.
		case WM_IME_CHAR:
			handled = IMECharHandler((unsigned short)wParam);

			if (handled)
				{
				outResult = TRUE;
				}
			break;

		case WM_CHAR:
			handled = CharHandler((unsigned short)wParam);

			if (handled)
				{
				outResult = TRUE;
				}
			break;

		case WM_KEYDOWN:
			if (mInComposition)
				{
				outResult = DefWindowProc(hwnd, msg, wParam, lParam);
				}
			else
				{
				handled = false;
				}
			break;

		case WM_KEYUP:
			if (mInComposition)
				{

				outResult = DefWindowProc(hwnd, msg, wParam, lParam);
				}
			else
				{
				handled = false;
				}
			break;

		case WM_IME_CONTROL:
		case WM_IME_KEYDOWN:
		case WM_IME_KEYUP:
		default:
			handled = false;
			break;
		}

	return handled;
	}

/******************************************************************************
*
* NAME
*     IMEManager::IMENotify
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

LRESULT IMEManager::IMENotify(WPARAM wParam, LPARAM lParam)
	{
	switch (wParam)
		{
		// The open status of the input context has changed.
		case IMN_SETOPENSTATUS:
			{
			HIMC imc = ImmGetContext(mHWND);

			if (imc)
				{
				IMEAction action = IME_DEACTIVATED;

				if (!ImmGetOpenStatus(imc))
					{
					// If the IME conversion engine is closed, we need to
					// erase all currently displayed composition chars and any
					// candidate windows.
					mCandidateColl.clear();
					ResetComposition();
					}
				else
					{
					action = IME_ACTIVATED;
					}

				ImmReleaseContext(mHWND, imc);

				IMEEvent event(action, this);
				NotifyObservers(event);
				}
			}
			break;

		// Open the status window
		case IMN_OPENSTATUSWINDOW:
			break;

		// Close the status window
		case IMN_CLOSESTATUSWINDOW:
			break;

		// Update the position of the status window.
		case IMN_SETSTATUSWINDOWPOS:
			break;

		// The font of the input context has changed.
		case IMN_SETCOMPOSITIONFONT:
			break;

		// The style or position of the composition window has changed.
		case IMN_SETCOMPOSITIONWINDOW:
			break;

		// The conversion mode of the input context has changed.
		case IMN_SETCONVERSIONMODE:

			break;

		// The sentence mode has changed.
		case IMN_SETSENTENCEMODE:

			break;

		// Open the candidate window (lParam = candidate flags)
		case IMN_OPENCANDIDATE:
			OpenCandidate(lParam);
			break;

		// Close the candidate window. (lParam = candidate flags)
		case IMN_CLOSECANDIDATE:
			CloseCandidate(lParam);
			break;

		// Changing the contents of the candidate window (lParam = candidate flags)
		case IMN_CHANGECANDIDATE:
			ChangeCandidate(lParam);
			break;

		// Candidate processing is finished; moving the candidate window
		case IMN_SETCANDIDATEPOS:
			break;

		// Show error message or other information
		case IMN_GUIDELINE:
			{
			IMEEvent event(IME_GUIDELINE, this);
			NotifyObservers(event);
			}
			break;

		default:
			break;
		}
	
	return TRUE;
	}

/******************************************************************************
*
* NAME
*     IMEManager::InputLanguageChangeRequest
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

HKL IMEManager::InputLanguageChangeRequest(HKL hkl)
	{

	// Get the number of Keyboard layouts available to the system
	UINT numLayouts = GetKeyboardLayoutList(0, NULL);

	if (numLayouts)
		{
		// Get the list of layouts
		std::vector<HKL> layoutList(numLayouts);
		layoutList.resize(numLayouts);

		numLayouts = GetKeyboardLayoutList(numLayouts, &layoutList[0]);

		// Find the position in the list of the layout which has been requested.
		std::vector<HKL>::iterator iter = std::find(layoutList.begin(), layoutList.end(), hkl);

		if (iter != layoutList.end())
			{
			// Rotate the list so the requested layout is at the head.
			std::rotate(layoutList.begin(), iter, layoutList.end());

			// Look for the layout that doesn't have the  AT_CARET or SPECIAL_UI properties
			iter = layoutList.begin();

			while (iter != layoutList.end())
				{
				DWORD property = ImmGetProperty(*iter, IGP_PROPERTY);

				if ((property & (IME_PROP_AT_CARET | IME_PROP_SPECIAL_UI)) == (IME_PROP_AT_CARET | IME_PROP_SPECIAL_UI))
					{
					iter++;
					}
				else
					{
					return *iter;
					}
				}
			}
		}

	return NULL;
	}

/******************************************************************************
*
* NAME
*     IMEManager::InputLanguageChanged
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::InputLanguageChanged(HKL hkl)
	{
	mLangID = LOWORD((uintptr_t)hkl);
	DWORD lcid = MAKELCID(mLangID, SORT_DEFAULT);

	// Get the default codepage for this input language
	char localeData[8];
	GetLocaleInfo(lcid, LOCALE_IDEFAULTANSICODEPAGE, localeData, sizeof(localeData));

	// Set the codepage for character conversion
	mCodePage = atoi(localeData);

	// Get properties
	mIMEProperties = ImmGetProperty(hkl, IGP_PROPERTY);

	mStartCandListFrom1 = ((mIMEProperties & IME_PROP_CANDLIST_START_FROM_1) == IME_PROP_CANDLIST_START_FROM_1);
	mUseUnicode = (mOSCanUnicode && (mIMEProperties & IME_PROP_UNICODE));

	// Get IME description
	if (mUseUnicode)
		{
		UINT descSize = ImmGetDescriptionW(hkl, NULL, 0);
		++descSize;
		char16_t* descPtr = mIMEDescription.Get_Buffer(descSize);

		ImmGetDescriptionW(hkl, descPtr, descSize);
		}
	else
		{
		UINT descSize = ImmGetDescription(hkl, NULL, 0);
		++descSize;

		StringClass desc((int)descSize, true);
		char* descPtr = desc.Get_Buffer(descSize);

		ImmGetDescription(hkl, descPtr, descSize);
		mIMEDescription = desc;
		}

	#if(0)
	mHilite = true;

	static const char16_t _TradChImeName[] = {0x6CE8,0x97F3,0x8F38,0x5165,0x6CD5,0x0020,0x0034,0x002E,0x0031,0x0020,0x7248,0x0000};

	if (mIMEDescription.Compare(_TradChImeName) == 0)
		{
		mHilite = false;
		}
	#endif

	IMEEvent action(IME_LANGUAGECHANGED, this);
	NotifyObservers(action);
	}

/******************************************************************************
*
* NAME
*     IMEManager::ResetComposition
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::ResetComposition(void)
	{
	mInComposition = false;


	mCompositionString[0] = 0;
	memset(mCompositionAttr, 0, sizeof(mCompositionAttr));
	memset(mCompositionClause, 0, sizeof(mCompositionClause));
	mCompositionCursorPos = 0;

	mReadingString[0] = 0;
	}

/******************************************************************************
*
* NAME
*     IMEManager::StartComposition
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::StartComposition(void)
	{

	mInComposition = true;
	mResultString[0] = 0;

	CompositionEvent event(COMPOSITION_START, this);
	NotifyObservers(event);
	}

/******************************************************************************
*
* NAME
*     IMEManager::DoComposition
*
* DESCRIPTION
*     Handle composition message.
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::DoComposition(unsigned int dbcs, long compFlags)
	{



	HIMC imc = ImmGetContext(mHWND);

	if (imc)
		{
		if (compFlags == 0)
			{
			ResetComposition();
			CompositionEvent event(COMPOSITION_CANCEL, this);
			NotifyObservers(event);
			}
		else if (compFlags & GCS_RESULTSTR)
			{
			// Retrieve the result string
			if (ReadCompositionString(imc, GCS_RESULTSTR, mResultString, sizeof(mResultString)))
				{

				CompositionEvent event(COMPOSITION_RESULT, this);
				NotifyObservers(event);
				}
			}
		else
			{
			CompositionAction action = COMPOSITION_INVALID;

			// Update reading string.
			if (compFlags & GCS_COMPREADSTR)
				{
				if (ReadCompositionString(imc, GCS_COMPREADSTR, mReadingString, sizeof(mReadingString)))
					{
					action = COMPOSITION_CHANGE;
					}
				}

			// Update composition string.
			if (compFlags & GCS_COMPSTR)
				{
				if (ReadCompositionString(imc, GCS_COMPSTR, mCompositionString, sizeof(mCompositionString)))
					{
					action = COMPOSITION_CHANGE;
					}
				}

			if (compFlags & GCS_COMPATTR)
				{
				long size = ReadCompositionAttr(imc, mCompositionAttr, sizeof(mCompositionAttr));

				}

			if (compFlags & GCS_COMPCLAUSE)
				{
				mCompositionClause[0] = 0;
				long size = ReadCompositionClause(imc, mCompositionClause, sizeof(mCompositionClause));

				}

			if (compFlags & GCS_CURSORPOS)
				{
				mCompositionCursorPos = ReadCursorPos(imc);
				action = COMPOSITION_CHANGE;
				}

			if (action != COMPOSITION_INVALID)
				{
				CompositionEvent event(action, this);
				NotifyObservers(event);
				}
			}

		ImmReleaseContext(mHWND, imc);
		}
	}

/******************************************************************************
*
* NAME
*     IMEManager::EndComposition
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::EndComposition(void)
	{

	HIMC imc = ImmGetContext(mHWND);

	if (imc)
		{
		ReadCompositionString(imc, GCS_COMPSTR, mResultString, sizeof(mResultString));
		ImmReleaseContext(mHWND, imc);
		}

	ResetComposition();
	CompositionEvent event(COMPOSITION_END, this);
	NotifyObservers(event);
	}

/******************************************************************************
*
* NAME
*     IMEManager::ReadCompositionString
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

bool IMEManager::ReadCompositionString(HIMC imc, unsigned long flag, char16_t* buffer, int length)
	{
	if (mUseUnicode)
		{
		LONG size = ImmGetCompositionStringW(imc, flag, buffer, length);

		if (size < 0)
			{
			buffer[0] = 0;
			return false;
			}
		
		// Terminate string
		buffer[(size / sizeof(char16_t))] = 0;
		}
	else
		{
		// Read the string as multibyte ANSI
		unsigned char string[IME_MAX_STRING_LEN];
		LONG size = ImmGetCompositionString(imc, flag, string, sizeof(string));

		if (size < 0)
			{
			buffer[0] = 0;
			return false;
			}

		// Terminate the string
		string[size] = 0;

		// Convert to Unicode
		MultiByteToWideChar(mCodePage, 0, (const char*)string, -1, buffer, (length / sizeof(char16_t)));
		}

	return true;
	}

/******************************************************************************
*
* NAME
*     IMEManager::
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

long IMEManager::ReadReadingAttr(HIMC imc, unsigned char* attr, int length)
	{
	if (mUseUnicode)
		{
		LONG size = ImmGetCompositionStringW(imc, GCS_COMPREADATTR, attr, length);
		return (size / sizeof(char16_t));
		}

	// Read the string as multibyte ANSI
	unsigned char string[IME_MAX_STRING_LEN];
	LONG size = ImmGetCompositionString(imc, GCS_COMPREADSTR, string, sizeof(string));

	if (size <= 0)
		{
		return 0;
		}

	// Terminate the string
	string[size] = 0;
		
	LONG attrSize = ImmGetCompositionString(imc, GCS_COMPREADATTR, attr, length);

	if (attrSize <= size)
		{
		return 0;
		}

	return ConvertAttrForUnicode(string, attr);
	}

/******************************************************************************
*
* NAME
*     IMEManager::
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

long IMEManager::ReadReadingClause(HIMC imc, unsigned long* clause, int length)
	{
	if (mUseUnicode)
		{
		LONG size = ImmGetCompositionStringW(imc, GCS_COMPREADCLAUSE, clause, length);
		return (size / sizeof(char16_t));
		}

	// Read the string as multibyte ANSI
	unsigned char string[IME_MAX_STRING_LEN];
	LONG size = ImmGetCompositionString(imc, GCS_COMPREADSTR, string, sizeof(string));

	if (size <= 0)
		{
		return 0;
		}

	// Terminate the string
	string[size] = 0;

	LONG clauseSize = ImmGetCompositionString(imc, GCS_COMPREADCLAUSE, clause, length);

	if (clauseSize <= 0)
		{
		return 0;
		}
	
	return ConvertClauseForUnicode(string, size, clause);
	}

/******************************************************************************
*
* NAME
*     IMEManager::
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

long IMEManager::ReadCompositionAttr(HIMC imc, unsigned char* attr, int length)
	{
	if (mUseUnicode)
		{
		return ImmGetCompositionStringW(imc, GCS_COMPATTR, attr, length);
		}

	// Read the string as multibyte ANSI
	unsigned char string[IME_MAX_STRING_LEN];
	LONG size = ImmGetCompositionString(imc, GCS_COMPSTR, string, sizeof(string));

	if (size <= 0)
		{
		return size;
		}

	// Terminate the string
	string[size] = 0;
		
	LONG attrSize = ImmGetCompositionString(imc, GCS_COMPATTR, attr, length);

	if (attrSize <= size)
		{
		return 0;
		}

	return ConvertAttrForUnicode(string, attr);
	}

/******************************************************************************
*
* NAME
*     IMEManager::
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

long IMEManager::ReadCompositionClause(HIMC imc, unsigned long* clause, int length)
	{
	if (mUseUnicode)
		{
		return ImmGetCompositionStringW(imc, GCS_COMPCLAUSE, clause, length);
		}

	// Read the string as multibyte ANSI
	unsigned char string[IME_MAX_STRING_LEN];
	LONG size = ImmGetCompositionString(imc, GCS_COMPSTR, string, sizeof(string));

	if (size <= 0)
		{
		return size;
		}

	// Terminate the string
	string[size] = 0;

	LONG clauseSize = ImmGetCompositionString(imc, GCS_COMPCLAUSE, clause, length);

	if (clauseSize <= 0)
		{
		return 0;
		}
	
	return ConvertClauseForUnicode(string, size, clause);
	}

/******************************************************************************
*
* NAME
*     IMEManager::ReadCursorPos
*
* DESCRIPTION
*     Read the composition string cursor position.
*
* INPUTS
*
* RESULT
*
******************************************************************************/

long IMEManager::ReadCursorPos(HIMC imc)
	{
	if (mUseUnicode)
		{
		long cursorPos = ImmGetCompositionStringW(imc, GCS_CURSORPOS, NULL, 0);
		return (cursorPos & 0x0000FFFF);
		}

	// Get the multibyte string
	char string[IME_MAX_STRING_LEN];
	LONG size = ImmGetCompositionString(imc, GCS_COMPSTR, string, sizeof(string));

	if (size < 0)
		{
		return 0;
		}

	string[size] = 0;

	long cursorPos = ImmGetCompositionString(imc, GCS_CURSORPOS, NULL, 0);
	cursorPos = (cursorPos & 0x0000FFFF);

	// Convert multibyte character position in unicode position.
	return _mbsnccnt((unsigned char*)string, cursorPos);
	}

/******************************************************************************
*
* NAME
*     IMEManager::GetTargetClause
*
* DESCRIPTION
*     Get the composition string conversion target range. This is the characters
*     that are currently be considered for conversion.
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::GetTargetClause(unsigned long& start, unsigned long& end)
	{
	int index = 0;
	const unsigned long compLength = wcslen(mCompositionString);

	while (mCompositionClause[index] < compLength)
		{
		unsigned long offset = mCompositionClause[index];

		if (ATTR_TARGET_CONVERTED == mCompositionAttr[offset])
			{
			start = offset;
			end = mCompositionClause[index + 1];
			return;
			}

		++index;
		}

	start = 0;
	end = 0;
	}

/******************************************************************************
*
* NAME
*     IMEManager::GetCompositionFont
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

bool IMEManager::GetCompositionFont(LPLOGFONT lpFont)
	{
	BOOL success = FALSE;
	HIMC imc = ImmGetContext(mHWND);

	if (imc)
		{
		success = ImmGetCompositionFont(imc, lpFont);
		ImmReleaseContext(mHWND, imc);
		}

	return (success == TRUE);
	}

/******************************************************************************
*
* NAME
*     IMEManager::OpenCandidate
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::OpenCandidate(unsigned long candList)
	{

	for (int index = 0; index < 32; index++)
		{
		if (candList & (1 << index))
			{
			IMECandidate& candidate = mCandidateColl[index];
			candidate.Open(index, mHWND, mCodePage, mUseUnicode, mStartCandListFrom1);
			candidate.Read();

			CandidateEvent event(CANDIDATE_OPEN, &candidate);
			NotifyObservers(event);
			}
		}
	}

/******************************************************************************
*
* NAME
*     IMEManager::ChangeCandidate
*
* DESCRIPTION
*     The contents of the candidate list has changed.
*
* INPUTS
*     Changed - Bitfield of the candidate lists that have changed.
*
* RESULT
*     NONE
*
******************************************************************************/

void IMEManager::ChangeCandidate(unsigned long candList)
	{

	for (int index = 0; index < 32; index++)
		{
		if (candList & (1 << index))
			{
			IMECandidate& candidate = mCandidateColl[index];
			candidate.Read();
			CandidateEvent event(CANDIDATE_CHANGE, &candidate);
			NotifyObservers(event);
			}
		}
	}

	
/******************************************************************************
*
* NAME
*     IMEManager::CloseCandidate
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void IMEManager::CloseCandidate(unsigned long candList)
	{

	for (int index = 0; index < 32; index++)
		{
		if (candList & (1 << index))
			{
			IMECandidate& candidate = mCandidateColl[index];
			CandidateEvent event(CANDIDATE_CLOSE, &candidate);
			NotifyObservers(event);
			candidate.Close();
			}
		}
	}

/******************************************************************************
*
* NAME
*     IMEManager::GetGuideLine
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

unsigned long IMEManager::GetGuideline(char16_t* outString, int length)
	{
	unsigned long level = GL_LEVEL_NOGUIDELINE;

	HIMC imc = ImmGetContext(mHWND);

	if (imc)
		{
		level = ImmGetGuideLine(imc, GGL_LEVEL, NULL, 0);

		if ((GL_LEVEL_NOGUIDELINE != level) && outString)
			{
			if (mUseUnicode)
				{
				DWORD size = ImmGetGuideLineW(imc, GGL_STRING, outString, (length * sizeof(char16_t)));
				outString[size / sizeof(char16_t)] = 0;
				}
			else
				{
				char temp[512];
				DWORD size = ImmGetGuideLine(imc, GGL_STRING, temp, sizeof(temp));
				temp[size] = 0;

				MultiByteToWideChar(mCodePage, 0, temp, -1, outString, length);
				outString[length] = 0;
				}
			}

		ImmReleaseContext(mHWND, imc);
		}

	return level;
	}

/******************************************************************************
*
* NAME
*     IMEManager::IMECharHandler
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*     True if character converted.
*
******************************************************************************/

bool IMEManager::IMECharHandler(unsigned short dbcs)
	{
	unsigned long mbcs = dbcs;

	// If this char has a lead byte then it is double byte. Swap the bytes
	// for generate string order
	if (dbcs & 0xFF00)
		{
		mbcs = (((dbcs & 0xFF) << 8) | (dbcs >> 8));
		}

	// Convert char to unicode
	char16_t unicode = 0;
	MultiByteToWideChar(mCodePage, 0, (const char*)&mbcs, -1, &unicode, 1);

	UnicodeChar event(unicode);
	NotifyObservers(event);

	return true;
	}

/******************************************************************************
*
* NAME
*     IMEManager::CharHandler
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*     True if character handled.
*
******************************************************************************/

bool IMEManager::CharHandler(unsigned short ch)
	{
	// Because DBCS characters are usually generated by IMEs (as two PostMessages),
	// if a lead byte comes in, the trail byte should arrive very soon after.
	// We wait here for the trail byte and store them into the text buffer together.
	if (!IsDBCSLeadByte((unsigned char)ch))
		{
		return false;
		}

	// Wait an arbitrary amount of time for the trail byte to arrive.
	// If it doesn't, then discard the lead byte. This could happen if the IME
	// screwed up.  Or, more likely, the user generated the lead byte through ALT-numpad.
	MSG msg;
	int i = 10;

	while (!PeekMessage(&msg, mHWND, WM_CHAR, WM_CHAR, PM_REMOVE))
		{
		if (--i == 0)
			{
			return true;
			}

		Sleep(0);
		}

	// Convert char to unicode.
	unsigned long dbcs = (unsigned long)(((unsigned)msg.wParam << 8) | ch);
	char16_t unicode = 0;
	MultiByteToWideChar(mCodePage, 0, (const char*)&dbcs, 2, &unicode, 1);

	UnicodeChar event(unicode);
	NotifyObservers(event);

	return true;
	}

/******************************************************************************
*
* NAME
*     IMEManager::
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

long IMEManager::ConvertAttrForUnicode(unsigned char* mbcs, unsigned char* attr)
	{
	// Scale the attributes for unicode string length
	unsigned char* mbsPtr = mbcs;
	unsigned char* attrPtr = attr;

	while (*mbsPtr != 0)
		{
		*attrPtr = attr[mbsPtr - mbcs];
		++attrPtr;
		mbsPtr = _mbsinc(mbsPtr);
		}

	return (attrPtr - attr);
	}

/******************************************************************************
*
* NAME
*     IMEManager::
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

long IMEManager::ConvertClauseForUnicode(unsigned char* mbcs, long length, unsigned long* clause)
	{
	//---------------------------------------------------------------------------
	// Scale the clause offsets for unicode string
	//---------------------------------------------------------------------------
	unsigned char* mbsPtr = mbcs;
	unsigned long offset = 0;

	// The first clause is always zero so there is no need to adjust it.
	int index = 1;

	// The clause is terminated with the size of the string
	while (clause[index] < (unsigned long)length)
		{
		// Count the number of characters in this clause
		unsigned char* mbsStop = (mbcs + clause[index]);

		while (mbsPtr < mbsStop)
			{
			++offset;
			mbsPtr = _mbsinc(mbsPtr);
			}

		clause[index] = offset;
		++index;
		}

	// Terminate the unicode adjusted clause with the string length
	clause[index] = _mbslen(mbcs);
	++index;

	return (&clause[index] - clause);
	}

} // namespace IME
