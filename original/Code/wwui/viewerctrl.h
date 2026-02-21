
#ifndef __VIEWER_CTRL_H
#define __VIEWER_CTRL_H

#include "dialogcontrol.h"
#include "vector3.h"
#include "render2d.h"
#include "aabox.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class SimpleSceneClass;
class CameraClass;
class RenderObjClass;
class LightClass;

////////////////////////////////////////////////////////////////
//
//	ViewerCtrlClass
//
////////////////////////////////////////////////////////////////
class ViewerCtrlClass : public DialogControlClass
{
public:

	enum InterfaceModeEnum {
		Z_ROTATION,				// Automatic rotation of camera around Z-axis.
		VIRTUAL_TRACKBALL		// Camera rotation with virtual trackball under user control.
	};
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	ViewerCtrlClass (void);
	virtual ~ViewerCtrlClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	// RTTI.
	ViewerCtrlClass *As_ViewerCtrlClass (void)		{ return this; }	

	//
	//	From DialogControlClass
	//
	void				Render (void);

	//
	// Configuation
	//
	void				Set_Background_Visible (bool isvisible)		{IsBackgroundVisible = isvisible;}
	void				Set_Rotation_Rate (float r)						{RotationRate = DEG_TO_RADF (r);}
	void				Set_Interface_Mode (InterfaceModeEnum mode, float rotationrate = 0.0f);
	void				Set_Camera_Min_Dist (float dist)				{ MinCameraDist = dist; }

	//
	//	Selection management
	//
	SimpleSceneClass *Peek_Scene()									{ return (Scene); }
	CameraClass		  *Peek_Camera()									{ return (Camera); }	
	void					Set_Model (const char *model_name);
	void					Set_Model (RenderObjClass *new_model);
	RenderObjClass	  *Peek_Model()									{ return (Model); }
	void					Set_Animation (const char *anim_name);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void				On_LButton_Down (const Vector2 &mouse_pos);
	void				On_LButton_Up (const Vector2 &mouse_pos);
	void				On_Set_Cursor (const Vector2 &mouse_pos);
	void				On_Frame_Update (void);
	void				Update_Client_Rect (void);

	void				Create_Control_Renderer (void);
	void				Free_Model (void);
	void				Get_Visible_Bounding_Box (AABoxClass *box, RenderObjClass *render_obj, bool &is_first);
	void				Calculate_Camera_Position (void);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	SimpleSceneClass *	Scene;
	CameraClass	*			Camera;
	LightClass *			Light;					// Light source.
	RenderObjClass *		Model;
	Render2DClass			ControlRenderer;
	AABoxClass				BoundingBox;
	float						Distance;
	float						ZRotation;
	float						RotationRate;
	float						MinCameraDist;
	bool						IsCameraDirty;
	bool						IsBackgroundVisible;
	InterfaceModeEnum		InterfaceMode;
	Vector3					LastMousePosition;
};

#endif //__VIEWER_CTRL_H
