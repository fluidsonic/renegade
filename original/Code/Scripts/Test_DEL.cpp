#include "scripts.h"
#include "common.h"
#include "dprint.h"
#include <string.h>
#include <assert.h>

#if 0
DECLARE_SCRIPT(DEL_VehicleEnterTest, "X:float,Y:float,Z:float")
	{
	void Created(GameObject* object)
		{
		float x = Get_Float_Parameter("X");
		float y = Get_Float_Parameter("Y");
		float z = Get_Float_Parameter("Z");
		DebugPrint("Enter Location %.2f,%.2f,%.2f\n", x, y, z);

		Commands->Action_Movement_Goto_Location(object, Vector3(4.88f, -66.22f, 0.0f), 1.0f);
		}
	};
#endif