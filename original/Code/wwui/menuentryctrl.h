
#ifndef __MENU_ENTRY_CTRL_H
#define __MENU_ENTRY_CTRL_H

#include "dialogcontrol.h"
#include "vector3.h"
#include "render2dsentence.h"


////////////////////////////////////////////////////////////////
//
//	MenuEntryCtrlClass
//
////////////////////////////////////////////////////////////////
class MenuEntryCtrlClass : public DialogControlClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	MenuEntryCtrlClass (void);
	virtual ~MenuEntryCtrlClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	From DialogControlClass
	//
	void					Render (void);
	bool					Wants_Tooltip (void) const	{ return false; }
	void					Center_Mouse (void);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////

	//
	//	From DialogControlClass
	//
	void					On_LButton_Down (const Vector2 &mouse_pos);
	void					On_LButton_Up (const Vector2 &mouse_pos);
	void					On_Mouse_Move (const Vector2 &mouse_pos);
	void					On_Set_Cursor (const Vector2 &mouse_pos);
	void					On_Create (void);	
	void					On_Set_Focus (void);
	void					On_Kill_Focus (DialogControlClass *);
	bool					On_Key_Down (uint32 key_id, uint32 key_data);
	bool					On_Key_Up (uint32 key_id);
	void					On_Mouse_Wheel (int direction);
	void					On_Frame_Update (void);
	void					Update_Client_Rect (void);

	void					Create_Text_Renderer (void);
	void					Set_State (int new_state);
	void					Update_State (void);

	void					Create_Glow (int radiusx, int radiusy, int color);
	void					On_Pushed (void);	


	////////////////////////////////////////////////////////////////
	//	Protected constants
	////////////////////////////////////////////////////////////////
	enum
	{
		UP				= 0,
		DOWN,
		HILIGHT,
		STATE_MAX
	};

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	Render2DSentenceClass	GlowRenderer;
	Render2DSentenceClass	TextRenderer;
	RectClass				MaxRect;
	bool						WasButtonPressedOnMe;
	bool						IsMouseOverMe;
	bool						TriggerCommand;
	int						CurrState;

	int						CurrRadiusX;
	int						CurrRadiusY;
	int						StartTime;
	int						EndTime;
	int						CurrColor;

	static int				MaxDefaultRedValue;
	static int				MaxHilightRedValue;
};


#endif //__MENU_ENTRY_CTRL_H
