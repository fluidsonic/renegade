#include "global.h"
#include "toolkit.h"

DECLARE_SCRIPT(M00_Spawner_Controller_RMV, "Spawner_ID:int, Custom_Type_To_Enable:int, Custom_Type_To_Trigger:int, On_When_Created=0:int")
{
	int id, enable, trigger;
	bool start_on;
	
	void Created(GameObject * obj)
	{
		id = Get_Int_Parameter("Spawner_ID");
		enable = Get_Int_Parameter("Custom_Type_To_Enable");
		trigger = Get_Int_Parameter("Custom_Type_To_Trigger");
		start_on = (Get_Int_Parameter("On_When_Created") == 1) ? true : false;
		if (Commands->Find_Object(id) != NULL)
			Commands->Enable_Spawner(id, start_on);
	}
	void Custom(GameObject * obj, int type, int param, GameObject * sender)
	{
		if (type == enable)
		{
			bool toggle = (param == 1) ? true : false;
			if (Commands->Find_Object(id) != NULL)
				Commands->Enable_Spawner(id, toggle);
		}
		if (type == trigger)
		{
//			bool force = (param == 1) ? true : false;
			if (Commands->Find_Object(id) != NULL)
				Commands->Trigger_Spawner(id);
		}
	}
};
