#ifndef __IMEMANAGER_H__
#define __IMEMANAGER_H__

#include "RefCount.h"
#include "IMECandidate.h"
#include "Notify.h"
#include "WideString.h"
#include "win.h"
#include <imm.h>

namespace IME {

#define IME_MAX_STRING_LEN 255
#define IME_MAX_TYPING_LEN 80

class IMEManager;

typedef enum
	{
	IME_ACTIVATED = 1,
	IME_DEACTIVATED,
	IME_LANGUAGECHANGED,
	IME_GUIDELINE,
	IME_ENABLED,
	IME_DISABLED,
	} IMEAction;

typedef TypedActionPtr<IMEAction, IMEManager> IMEEvent;

typedef enum
	{
	COMPOSITION_INVALID = 0,
	COMPOSITION_TYPING,
	COMPOSITION_START,
	COMPOSITION_CHANGE,
	COMPOSITION_FULL,
	COMPOSITION_END,
	COMPOSITION_CANCEL,
	COMPOSITION_RESULT
	} CompositionAction;

typedef TypedActionPtr<CompositionAction, IMEManager> CompositionEvent;

class UnicodeType;
typedef TypedEvent<UnicodeType, char16_t> UnicodeChar;

class IMEManager :
		public RefCountClass,
		public Notifier<IMEEvent>,
		public Notifier<UnicodeChar>,
		public Notifier<CompositionEvent>,
		public Notifier<CandidateEvent>
	{
	public:
		static IMEManager* Create(HWND hwnd);

		void Activate(void);
		void Deactivate(void);
		bool IsActive(void) const;

		void Disable(void);
		void Enable(void);
		bool IsDisabled(void) const;

		const char16_t* GetDescription(void) const
			{return mIMEDescription;}

		WORD GetLanguageID(void) const
			{return mLangID;}

		UINT GetCodePage(void) const
			{return mCodePage;}

		const char16_t* GetResultString(void) const
			{return mResultString;}

		const char16_t* GetCompositionString(void) const
			{return mCompositionString;}

		long GetCompositionCursorPos(void) const
			{return mCompositionCursorPos;}

		const char16_t* GetReadingString(void) const
			{return mReadingString;}

		#ifdef SHOW_IME_TYPING
		const char16_t* GetTypingString(void) const
			{return mTypingString;}
		#endif

		void GetTargetClause(unsigned long& start, unsigned long& end);

		bool GetCompositionFont(LPLOGFONT lpFont);

		const IMECandidateCollection GetCandidateColl(void) const
			{return mCandidateColl;}

		unsigned long GetGuideline(char16_t* outString, int length);

		bool ProcessMessage(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam, LRESULT& result);

	protected:
		IMEManager();
		virtual ~IMEManager();

		bool FinalizeCreate(HWND hwnd);

		LRESULT IMENotify(WPARAM wParam, LPARAM lParam);
		
		HKL InputLanguageChangeRequest(HKL hkl);
		void InputLanguageChanged(HKL hkl);

		void ResetComposition(void);
		void StartComposition(void);
		void DoComposition(unsigned int dbcsChar, long changeFlag);
		void EndComposition(void);

		bool ReadCompositionString(HIMC imc, unsigned long flag, char16_t* buffer, int length);
		long ReadReadingAttr(HIMC imc, unsigned char* attr, int length);
		long ReadReadingClause(HIMC imc, unsigned long* clause, int length);
		long ReadCompositionAttr(HIMC imc, unsigned char* attr, int length);
		long ReadCompositionClause(HIMC imc, unsigned long* clause, int length);
		long ReadCursorPos(HIMC imc);

		void OpenCandidate(unsigned long candList);
		void ChangeCandidate(unsigned long candList);
		void CloseCandidate(unsigned long candList);

		bool IMECharHandler(unsigned short dbcs);
		bool CharHandler(unsigned short ch);

		long ConvertAttrForUnicode(unsigned char* mbcs, unsigned char* attr);
		long ConvertClauseForUnicode(unsigned char* mbcs, long length, unsigned long* clause);

		DECLARE_NOTIFIER(IMEEvent)
		DECLARE_NOTIFIER(UnicodeChar)
		DECLARE_NOTIFIER(CompositionEvent)
		DECLARE_NOTIFIER(CandidateEvent)

		// Prevent copy and assignment
		IMEManager(const IMEManager&);
		const IMEManager& operator=(const IMEManager&);

	private:
		HWND mHWND;
		HIMC mDefaultHIMC;
		HIMC mHIMC;

		HIMC mDisabledHIMC;
		unsigned long mDisableCount;

		WORD mLangID;
		UINT mCodePage;
		WideStringClass mIMEDescription;
		DWORD mIMEProperties;

		bool mHilite;
		bool mStartCandListFrom1;
		bool mOSCanUnicode;
		bool mUseUnicode;
		bool mInComposition;

		#ifdef SHOW_IME_TYPING
		char16_t mTypingString[IME_MAX_TYPING_LEN];
		long mTypingCursorPos;
		#endif
		
		char16_t mCompositionString[IME_MAX_STRING_LEN];
		unsigned char mCompositionAttr[IME_MAX_STRING_LEN];
		unsigned long mCompositionClause[IME_MAX_STRING_LEN / 2];

		long mCompositionCursorPos;

		char16_t mReadingString[IME_MAX_STRING_LEN * 2];
		char16_t mResultString[IME_MAX_STRING_LEN];

		IMECandidateCollection mCandidateColl;
	};

} // namespace IME

#endif __IMEMANAGER_H__
