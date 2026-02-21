#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DIALOG_BUTTON_H
#define __DIALOG_BUTTON_H

#include "dialogcontrol.h"
#include "vector3.h"
#include "render2dsentence.h"


////////////////////////////////////////////////////////////////
//
//	DialogButtonClass
//
////////////////////////////////////////////////////////////////
class DialogButtonClass : public DialogControlClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	DialogButtonClass (void);
	virtual ~DialogButtonClass (void)	{}

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	From DialogControlClass
	//
	void					Render (void);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void					On_LButton_Down (const Vector2 &mouse_pos);
	void					On_LButton_Up (const Vector2 &mouse_pos);
	void					On_Mouse_Move (const Vector2 &mouse_pos);
	void					On_Set_Cursor (const Vector2 &mouse_pos);
	void					On_Create (void);
	void					On_Kill_Focus (DialogControlClass *focus);
	bool					On_Key_Down (uint32 key_id, uint32 key_data);

	void					Create_Bitmap_Button (void);
	void					Create_Component_Button (void);
	void					Create_Component_Button2 (void);
	void					Create_Text_Renderers (void);


	////////////////////////////////////////////////////////////////
	//	Protected constants
	////////////////////////////////////////////////////////////////
	enum
	{
		UP				= 0,
		DOWN,
		STATE_MAX
	};

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	Render2DSentenceClass	TextRenderers[STATE_MAX];
	Render2DClass				ButtonRenderers[STATE_MAX];
	bool							WasButtonPressedOnMe;
	bool							IsMouseOverMe;
};


#endif //__DIALOG_BUTTON_H
