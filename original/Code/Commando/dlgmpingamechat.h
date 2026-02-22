#pragma once

#include "global.h"

#include "popupdialog.h"
#include "childdialog.h"
#include "resource.h"
#include "sctextobj.h"

////////////////////////////////////////////////////////////////
//
//	MPChatChildDialogClass
//
////////////////////////////////////////////////////////////////
class MPChatChildDialogClass : public ChildDialogClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	MPChatChildDialogClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Configuration
	//
	void	Set_Default_Type (TextMessageEnum message_type)		{ MessageType = message_type; }
	void	Set_End_Dialog_On_Send (bool onoff)						{ EndDialogOnSend = onoff; }

	//
	//	Inherited
	//
	void	On_Init_Dialog (void);
	bool	On_EditCtrl_Key_Down (EditCtrlClass *edit_ctrl, uint32_t key_id, uint32_t key_data);
	void	On_EditCtrl_Change(EditCtrlClass *edit_ctrl, int ctrl_id);
	void	On_EditCtrl_Enter_Pressed (EditCtrlClass *edit_ctrl, int ctrl_id);
	void	On_Command (int ctrl_id, int message_id, DWORD param);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////	
	void Process_Message(void);
	void Send_Message(WideStringClass& message, TextMessageEnum type, int recipientID);
	bool Process_Commands(const WCHAR* message);

	//
	//	Auto completetion
	//
	void	Auto_Complete_Name (void);
	void	Complete_Player_Name (const WCHAR *typed_name, WideStringClass &completed_name);
	bool	Find_Current_Command (const WCHAR *	message, int &start_index, int &end_index);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	TextMessageEnum	MessageType;
	WideStringClass	RecipientName;
	WideStringClass	CurrRecipientName;
	bool					TestForAutoCompletion;
	bool					EndDialogOnSend;
};

////////////////////////////////////////////////////////////////
//
//	MPIngameChatPopupClass
//
////////////////////////////////////////////////////////////////
class MPIngameChatPopupClass : public PopupDialogClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	MPIngameChatPopupClass (void);
	~MPIngameChatPopupClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Configuration
	//
	void	Set_Default_Type (TextMessageEnum message_type)		{ DefaultType = message_type; }

	//
	//	Inherited
	//
	void	On_Init_Dialog (void);
	void	On_Command (int ctrl_id, int mesage_id, DWORD param);
	void	Render (void);

	//
	//	Callbacks
	//
	//void	On_ComboBoxCtrl_Sel_Change (ComboBoxCtrlClass *combo_ctrl, int ctrl_id, int old_sel, int new_sel);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void	Send_Message (void);
	void	Configure_Recipient_Combobox (void);
	void	Configure_Message_Log_List (void);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	Render2DClass					WindowBackgroundRenderer;
	TextMessageEnum				DefaultType;
	MPChatChildDialogClass *	ChatModule;
};
