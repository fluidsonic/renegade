#include "tooltipmgr.h"
#include "tooltip.h"
#include "dialogcontrol.h"
#include "dialogmgr.h"
#include "ww3d.h"


////////////////////////////////////////////////////////////////
//	Static member initialization
////////////////////////////////////////////////////////////////
ToolTipClass *			ToolTipMgrClass::ToolTip					= NULL;
int						ToolTipMgrClass::ToolTipDelay				= 1000;
int						ToolTipMgrClass::DefaultToolTipDelay	= 1000;
bool						ToolTipMgrClass::ToolTipDisplayed		= false;
DialogControlClass *	ToolTipMgrClass::CurrentControl			= NULL;
Vector2					ToolTipMgrClass::LastMousePos	(0, 0);
int						ToolTipMgrClass::PauseTime					= -1;


////////////////////////////////////////////////////////////////
//
//	Initialize
//
////////////////////////////////////////////////////////////////
void
ToolTipMgrClass::Initialize (void)
{
	ToolTipDisplayed = false;
	
	//
	//	Allocate the tooltip object
	//
	ToolTip = new ToolTipClass;
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Shutdown
//
////////////////////////////////////////////////////////////////
void
ToolTipMgrClass::Shutdown (void)
{
	ToolTipDisplayed = false;

	//
	//	Free the tooltip object
	//
	delete ToolTip;
	ToolTip = NULL;
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Render
//
////////////////////////////////////////////////////////////////
void
ToolTipMgrClass::Render (void)
{
	//
	//	Simply render the tooltip (if necessary)
	//
	if (ToolTipDisplayed && ToolTip != NULL) {
		ToolTip->Render ();
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	Reset
//
////////////////////////////////////////////////////////////////
void
ToolTipMgrClass::Reset (void)
{
	ToolTipDisplayed = false;
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Update
//
////////////////////////////////////////////////////////////////
void
ToolTipMgrClass::Update (const Vector2 &mouse_pos)
{
	if (ToolTip == NULL) {
		return ;
	}

	if (ToolTipDisplayed == false && mouse_pos == LastMousePos) {
		
		//
		//	Begin waiting for the mouse to move
		//
		if (PauseTime == -1) {
			PauseTime = (int)DialogMgrClass::Get_Time ();
		} else if (((int)DialogMgrClass::Get_Time () - PauseTime) > ToolTipDelay) {
			
			//
			//	What control are we over?
			//
			DialogControlClass *control = DialogMgrClass::Find_Control (mouse_pos);
			if (control != NULL && control->Wants_Tooltip ()) {
				CurrentControl = control;
				
				//
				//	Update the text of the tooltip
				//
				WideStringClass text;
				control->Get_Tooltip_Text (text);
				ToolTip->Set_Text (text);

				//
				//	Reposition the tooltip
				//
				Vector2 tip_pos (mouse_pos.X + 16, mouse_pos.Y + 32);
				ToolTip->Set_Position (tip_pos);

				//
				//	Display the tooltip
				//
				ToolTipDisplayed = true;
				PauseTime = -1;
			}
		}

	} else if (mouse_pos != LastMousePos) {
		PauseTime = -1;
		
		if (ToolTipDisplayed) {
			
			//
			//	Did we move onto a different control?
			//
			DialogControlClass *control = DialogMgrClass::Find_Control (mouse_pos);
			if (control != CurrentControl) {
				
				//
				//	Force the tooltip to be hidden
				//
				ToolTipDisplayed	= false;
				CurrentControl		= NULL;				
			}
		}
	}

	LastMousePos = mouse_pos;
	return ;
}

