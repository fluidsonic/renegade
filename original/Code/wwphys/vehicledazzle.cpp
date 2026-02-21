#include "vehicledazzle.h"
#include "dazzle.h"
#include "ww3d.h"
#include "vehiclephys.h"
#include "physcontrol.h"

/*
** Dazzle parsing constants
*/
const char *	HEADLIGHT_NAME_PREFIX					= "REN_HEADLIGHT";
const char *	BRAKELIGHT_NAME_PREFIX					= "REN_BRAKELIGHT";
const char *	BLINKLIGHT_NAME_PREFIX					= "REN_BLINKLIGHT";

/***********************************************************************************************
 * VehicleDazzleClass::VehicleDazzleClass -- Constructor                                       *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   7/27/2001  gth : Created.                                                                 *
 *=============================================================================================*/
VehicleDazzleClass::VehicleDazzleClass(void) :
	Type(HEADLIGHT_TYPE),
	Model(NULL),
	BlinkRate(0.0f),
	CreationTime(0)
{
}

/***********************************************************************************************
 * VehicleDazzleClass::~VehicleDazzleClass -- Destructor                                       *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   7/27/2001  gth : Created.                                                                 *
 *=============================================================================================*/
VehicleDazzleClass::~VehicleDazzleClass(void)
{
	REF_PTR_RELEASE(Model);
}

/***********************************************************************************************
 * VehicleDazzleClass::Set_Model -- sets the dazzle model for this object to control           *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   7/27/2001  gth : Created.                                                                 *
 *=============================================================================================*/
void VehicleDazzleClass::Set_Model(DazzleRenderObjClass * model)
{
	REF_PTR_SET(Model,model);
	if (Model != NULL) {
		Type = Determine_Type(model);
		CreationTime = WW3D::Get_Sync_Time();
	}
}

/***********************************************************************************************
 * VehicleDazzleClass::Set_Time_Of_Day -- updates any parameters which depend on time of day   *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   7/27/2001  gth : Created.                                                                 *
 *=============================================================================================*/
void VehicleDazzleClass::Set_Time_Of_Day(float time)
{
}

/***********************************************************************************************
 * VehicleDazzleClass::Pre_Render_Update -- Update dazzle state prior to rendering             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   7/27/2001  gth : Created.                                                                 *
 *=============================================================================================*/
void VehicleDazzleClass::Pre_Render_Update(VehiclePhysClass * parent)
{
	if (Model != NULL) {
		
		switch (Type) 
		{
			case HEADLIGHT_TYPE: 
			{
				/*
				** Turn on the headlights if the engine is on
				*/
				float intensity = (parent->Is_Engine_Enabled() ? 1.0f : 0.0f);
				Model->Set_Dazzle_Color(Vector3(intensity,intensity,intensity));
				break;
			}
		
			case BRAKELIGHT_TYPE:
			{
				/*
				** Turn on the brakelights if the engine is on
				** Make them bright if the vehicle has forward velocity but the controller is pointing backwards
				*/
				Vector3 velocity;
				parent->Get_Velocity(&velocity);
				float forward_vel = Vector3::Dot_Product(parent->Get_Transform().Get_X_Vector(),velocity);
				bool controller_back = ((parent->Get_Controller() != NULL) && (parent->Get_Controller()->Get_Move_Forward() < 0.0f));
				bool is_braking = ((forward_vel > 0.0f) && (controller_back));

				float intensity = (parent->Is_Engine_Enabled() ? 1.0f : 0.0f);
				intensity *= (is_braking ? 1.0f : 0.25f);

				Model->Set_Dazzle_Color(Vector3(intensity,intensity,intensity));
				break;
			}

			case BLINKLIGHT_TYPE:
			{
				unsigned int elapsed_time = (WW3D::Get_Sync_Time() - CreationTime) & 0x000003FF;
				Model->Set_Hidden(elapsed_time > 0x0000001FF);
//Model->Set_Hidden(true);
				break;
			}
		}
	}
}

/***********************************************************************************************
 * VehicleDazzleClass::Is_Vehicle_Dazzle -- static function for filtering vehicle dazzles      *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   7/27/2001  gth : Created.                                                                 *
 *=============================================================================================*/
bool VehicleDazzleClass::Is_Vehicle_Dazzle(RenderObjClass * obj)
{
	int type = Determine_Type(obj);
	return type != NONE;
}

/***********************************************************************************************
 * VehicleDazzleClass::Determine_Type -- Determine the type of vehicle dazzle this is          *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   7/30/2001  gth : Created.                                                                 *
 *=============================================================================================*/
int VehicleDazzleClass::Determine_Type(RenderObjClass * obj)
{
	if ((obj != NULL) && (obj->Class_ID() == RenderObjClass::CLASSID_DAZZLE)) {
		
		DazzleRenderObjClass * dazzle = (DazzleRenderObjClass *)obj;
		const char * type_name = DazzleRenderObjClass::Get_Type_Name(dazzle->Get_Dazzle_Type());
		
		if (strnicmp(type_name,HEADLIGHT_NAME_PREFIX,strlen(HEADLIGHT_NAME_PREFIX)) == 0) {
			return HEADLIGHT_TYPE;
		}
		
		if (strnicmp(type_name,BRAKELIGHT_NAME_PREFIX,strlen(BRAKELIGHT_NAME_PREFIX)) == 0) {
			return BRAKELIGHT_TYPE;
		}

		if (strnicmp(type_name,BLINKLIGHT_NAME_PREFIX,strlen(BLINKLIGHT_NAME_PREFIX)) == 0) {
			return BLINKLIGHT_TYPE;
		}

	}

	return NONE;
}
