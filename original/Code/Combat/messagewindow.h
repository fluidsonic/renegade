
#ifndef __MESSAGE_WINDOW_H
#define __MESSAGE_WINDOW_H

#include "wwstring.h"
#include "vector.h"
#include "vector3.h"
#include "textwindow.h"
#include "render2d.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class SmartGameObj;
class RenderObjClass;

class	SimpleSceneClass;
class	CameraClass;

////////////////////////////////////////////////////////////////
//
//	MessageWindowClass
//
////////////////////////////////////////////////////////////////
class MessageWindowClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	MessageWindowClass (void);
	~MessageWindowClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	void				Initialize (void);
	void				Shutdown (void);

	//
	//	Content control
	//
	void				Add_Message (const WideStringClass &message, const Vector3 &color = Vector3 (0, 0.9F, 0.2F), SmartGameObj *game_obj = NULL, float decay_time = 0);
	void				Clear (void);

	//
	//	Render methods
	//
	void				On_Frame_Update (void);
	void				Render (void);

	//
	//	Visibility control
	//
	bool				Has_Data (void) const;
	void				Force_Display (bool onoff);
	
	//
	//	Display rectangle control
	//
	void				Update_Window_Rectangle (void);
	void				Reset_Current_Rect (void);
	void				Set_Window_Dirty (bool onoff)		{ IsRectangleDirty = onoff; }

	//
	//	Decay control
	//
	uint32_t			Get_Decay_Time (void) const	{ return DecayTime; }
	void				Set_Decay_Time (uint32_t time)	{ DecayTime = time; }

	//
	//	Message log support
	//
	void					Clear_Log (void)				{ MessageLog.Reset_Active (); MessageLogColor.Reset_Active (); }
	int					Get_Log_Count (void)			{ return MessageLog.Count (); }
	const WCHAR *		Get_Log_Entry (int index)	{ return MessageLog[index]; }
	const Vector3 &	Get_Log_Color (int index)	{ return MessageLogColor[index]; }

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////	

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	uint32_t				DecayTime;
	TextWindowClass *	TextWindow;
	Render2DClass		IconRenderer;
	RectClass			CurrentRect;
	RenderObjClass *	HeadModel;
	SimpleSceneClass  * Scene;
	CameraClass			* Camera;
	bool					IsRectangleDirty;
	
	DynamicVectorClass<WideStringClass>	MessageLog;
	DynamicVectorClass<Vector3> MessageLogColor;
};

#endif //__MESSAGE_WINDOW_H

