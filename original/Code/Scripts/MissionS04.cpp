#include "global.h"
#include "scripts.h"
#include "toolkit.h"
#include "Mission3.h"


DECLARE_SCRIPT(MS04_Gunboat_Waypath_Movement, "")
{
	void Created( GameObject * obj ) 
	{
		ActionParamsStruct params;
		params.Set_Basic( this, 45, 999 );
		params.Set_Movement( Vector3(0,0,0), 1.0f, 3.0f );
		params.WaypathID = 100033;
		Commands->Action_Goto( obj, params );
	}
};
