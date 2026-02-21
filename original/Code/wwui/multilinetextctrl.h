
#ifndef __MULTILINETEXTCTRL_H
#define __MULTILINETEXTCTRL_H


#include "render2d.h"
#include "render2dsentence.h"
#include "scrollbarctrl.h"


//////////////////////////////////////////////////////////////////////
//
//	MultiLineTextCtrlClass
//
//////////////////////////////////////////////////////////////////////
class MultiLineTextCtrlClass : public DialogControlClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	MultiLineTextCtrlClass  (void);
	~MultiLineTextCtrlClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////

	//
	//	Inherited
	//
	void				Render (void);
	void				Set_Text (const WCHAR *title);

	//
	//	Advise-sink callbacks
	//
	void				On_VScroll (ScrollBarCtrlClass *scrollbar, int ctrl_id, int new_position);

protected:
	
	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	void				Update_Client_Rect (void);
	bool				On_Key_Down (uint32 key_id, uint32 key_data);
	void				On_Mouse_Wheel (int direction);

	void				Create_Control_Renderer (void);
	void				Create_Text_Renderer (void);

	void				Calculate_Row_Count (void);
	void				Update_Scroll_Bar_Visibility (void);	
	void				Set_Scroll_Pos (int new_position);
	
	///////////////////////////////////////////////////////////////////
	//	Protected member data
	///////////////////////////////////////////////////////////////////
	Render2DClass				ControlRenderer;
	Render2DSentenceClass	TextRenderer;
	ScrollBarCtrlClass		ScrollBarCtrl;
	bool							IsInitialized;
	int							ScrollPos;
	bool							IsScrollBarDisplayed;
	int							RowCount;
	int							RowsPerPage;
	float							MouseWheelIncrement;
};


#endif //__MULTILINETEXTCTRL_H
