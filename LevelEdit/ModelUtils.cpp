#include "stdafx.h"
#include "modelutils.h"
#include "rendobj.h"
#include "utils.h"


/////////////////////////////////////////////////////////////////////////
//
//	Set_Model_Collision_Type
//
/////////////////////////////////////////////////////////////////////////
void
Set_Model_Collision_Type (RenderObjClass *model, int collision_type)
{
	if (model == NULL) {
		return ;
	}
		
	//
	//	Loop over all the sub-objects
	//
	int count = model->Get_Num_Sub_Objects ();
	for (int index = 0; index < count; index ++) {
		
		//
		// Get a pointer to this subobject
		//
		RenderObjClass *sub_object = model->Get_Sub_Object (index);
		if (sub_object != NULL) {
			
			//
			//	Set the collision bits for this sub-object
			//
			::Set_Model_Collision_Type (sub_object, collision_type);
			MEMBER_RELEASE (sub_object);
		}
	}	

	//
	//	Set the collision type for this model
	//
	if (	model->Is_Not_Hidden_At_All () &&
			model->Class_ID () != RenderObjClass::CLASSID_AABOX &&
			model->Class_ID () != RenderObjClass::CLASSID_OBBOX)
	{
		model->Set_Collision_Type (collision_type);
	}

	return ;
}
