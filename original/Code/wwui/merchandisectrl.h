#pragma once

#include "global.h"

#include "dialogcontrol.h"
#include "render2dsentence.h"

////////////////////////////////////////////////////////////////
//
//	MerchandiseCtrlClass
//
////////////////////////////////////////////////////////////////
class MerchandiseCtrlClass : public DialogControlClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	MerchandiseCtrlClass (void);
	virtual ~MerchandiseCtrlClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	From DialogControlClass
	//
	void				Render (void);

	//
	//	Configuration
	//
	int				Get_Cost (void) const		{ return Cost; }
	//const char *	Get_Texture (void) const	{ return TextureName; }	
	void				Set_Cost (int cost)			{ Cost = cost; Set_Dirty (); }

	void				Set_Texture (const char *texture_name);
	void				Add_Alternate_Texture (const char *texture_name);

	int				Get_Selected_Alternate (void)	{ return (CurrentTextureIndex - 1); }

	DWORD				Get_User_Data (void) const		{ return UserData; }
	void				Set_User_Data (DWORD data)		{ UserData = data; }	

	void				Increment_Purchase_Count (void)	{ Count ++; Set_Dirty (); }
	void				Reset_Purchase_Count (void)		{ Count = 0; Set_Dirty (); }
	int				Get_Purchase_Count (void)			{ return Count; }

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void				On_LButton_Down (const Vector2 &mouse_pos);
	void				On_LButton_DblClk (const Vector2 &mouse_pos);
	void				On_Set_Cursor (const Vector2 &mouse_pos);
	void				Update_Client_Rect (void);

	void				Create_Control_Renderer (void);
	void				Create_Text_Renderer (void);
	void				Create_Texture_Renderer (void);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	Render2DSentenceClass 	NameTextRenderer;
	Render2DSentenceClass 	CostTextRenderer;
	Render2DSentenceClass 	CountTextRenderer;
	Render2DClass				ControlRenderer;
	Render2DClass				TextureRenderer;
	Render2DClass				HilightRenderer;
	Render2DClass				ButtonRenderer;

	RectClass					TextureRect;
	RectClass					TextRect;
	RectClass					CostRect;
	RectClass					CountRect;

	DWORD							UserData;
	int							Cost;
	int							Count;

	DynamicVectorClass<StringClass>	TextureNameList;
	int										CurrentTextureIndex;
};
