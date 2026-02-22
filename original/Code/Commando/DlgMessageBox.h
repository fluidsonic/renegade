#pragma once

#include "global.h"

#include <PopupDialog.h>
#include <Notify.h>

class DlgMsgBox;

class DlgMsgBoxEvent :
		public TypedEventPtr<DlgMsgBoxEvent, DlgMsgBox>
	{
	public:
		typedef enum
			{
			None = 0, // NULL event
			Okay,     // Okay button pressed
			Yes,      // Yes button pressed
			No,       // No button pressed
			Quitting, // Dialog quitting.
			} EventID;

		//! Retrieve event
		inline EventID Event(void) const
			{return mEvent;}

		//! User data access
		inline unsigned long Get_User_Data(void) const
			{return mUserData;}

		inline void Set_User_Data(unsigned long data)
			{mUserData = data;}

		DlgMsgBoxEvent(EventID event, DlgMsgBox* object, unsigned long user_data) :
				TypedEventPtr<DlgMsgBoxEvent, DlgMsgBox>(object),
			  mEvent(event), mUserData (user_data)
			{}

	protected:
		// Prevent copy and assignment
		DlgMsgBoxEvent(const DlgMsgBoxEvent&);
		const DlgMsgBoxEvent& operator=(const DlgMsgBoxEvent&);

	private:
		EventID mEvent;
		unsigned long mUserData;
	};

class DlgMsgBox :
		public PopupDialogClass,
		public Notifier<DlgMsgBoxEvent>
	{
	public:
		typedef enum
			{
			Okay = 0, // Message box with okay button (Default)
			YesNo,    // Yes/No message box
			} Type;

		static bool DoDialog(const WCHAR* title, const WCHAR* text,
			DlgMsgBox::Type type = DlgMsgBox::Okay, Observer<DlgMsgBoxEvent>* observer = NULL,
			unsigned long user_data = 0);

		static bool DoDialog(int titleID, int textID, DlgMsgBox::Type type = DlgMsgBox::Okay,
			Observer<DlgMsgBoxEvent>* observer = NULL, unsigned long user_data = 0);

		void Set_User_Data(unsigned long user_data)
			{mUserData = user_data;}

		unsigned long Get_User_Data(void) const
			{return mUserData;}

		static int Get_Current_Count(void)
			{return CurrentCount;}

	protected:
		DlgMsgBox();
		virtual ~DlgMsgBox();

		void SetResourceType(DlgMsgBox::Type type);
		void End_Dialog(void);
		void On_Command(int ctrl, int message, DWORD param);

		DECLARE_NOTIFIER(DlgMsgBoxEvent)

	private:
		// Prevent copy and assignment
		DlgMsgBox(const DlgMsgBox&);
		const DlgMsgBox& operator=(const DlgMsgBox&);

		static int CurrentCount;
		unsigned long mUserData;
	};
