#include "wwuiinput.h"
#include "dialogmgr.h"

WWUIInputClass::WWUIInputClass(void) :
	mIMEManager(NULL)
{
}

WWUIInputClass::~WWUIInputClass(void)
{
	if (mIMEManager) {
		mIMEManager->Release_Ref();
	}
}

void WWUIInputClass::InitIME(HWND hwnd)
{
	if (mIMEManager == NULL) {
		mIMEManager = IME::IMEManager::Create(hwnd);

		if (mIMEManager) {
			Observer<IME::UnicodeChar>::NotifyMe(*mIMEManager);
			Observer<IME::IMEEvent>::NotifyMe(*mIMEManager);
		}
	}
}

IME::IMEManager* WWUIInputClass::GetIME(void) const
{
	if (mIMEManager) {
		mIMEManager->Add_Ref();
	}

	return mIMEManager;
}

bool WWUIInputClass::ProcessMessage(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam, LRESULT& result)
{

	if (mIMEManager) {
		if (mIMEManager->ProcessMessage(hwnd, msg, wParam, lParam, result)) {
			return true;
		}
	}

	result = 0;

	switch (msg) {
		case WM_KEYDOWN:
			return DialogMgrClass::On_Key_Down(wParam, lParam);
			break;

		case WM_KEYUP:
			return DialogMgrClass::On_Key_Up(wParam);
			break;

		case WM_CHAR:
			DialogMgrClass::On_Unicode_Char((char16_t)wParam);
			return true;
			break;

		default:
			break;
	}

	return false;
}

void WWUIInputClass::HandleNotification(IME::UnicodeChar& unicode)
{
	DialogMgrClass::On_Unicode_Char(unicode.Subject());
}

void WWUIInputClass::HandleNotification(IME::IMEEvent& event)
	{
	if (IME::IME_LANGUAGECHANGED == event.GetAction())
		{
		const char16_t* description = event.Subject()->GetDescription();
		DialogMgrClass::Show_IME_Message(description, 2500);
		}
	else if (IME::IME_GUIDELINE == event.GetAction())
		{
		char16_t desc[255];
		unsigned long level = event.Subject()->GetGuideline(desc, sizeof(desc));

		if (GL_LEVEL_NOGUIDELINE != level)
			{
			DialogMgrClass::Show_IME_Message(desc, 30000);
			}
		}
	}
