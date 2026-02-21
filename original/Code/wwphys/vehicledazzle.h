#ifndef VEHICLEDAZZLE_H
#define VEHICLEDAZZLE_H

#include "always.h"

class RenderObjClass;
class DazzleRenderObjClass;
class VehiclePhysClass;

/**
** VehicleDazzleClass
** This object controls a dazzle for a vehicle.  These are used to implement headlight dazzles,
** brakelight dazzles, and blinking dazzles on vehicles.
*/
class VehicleDazzleClass
{
public:

	VehicleDazzleClass(void);
	~VehicleDazzleClass(void);

	void								Set_Model(DazzleRenderObjClass * model);
	void								Set_Time_Of_Day(float time);
	void								Pre_Render_Update(VehiclePhysClass * parent);

	static bool						Is_Vehicle_Dazzle(RenderObjClass * model);

private:
		
	enum { HEADLIGHT_TYPE = 0, BRAKELIGHT_TYPE, BLINKLIGHT_TYPE, NONE = -1};
	static int						Determine_Type(RenderObjClass * model);

	int								Type;				
	DazzleRenderObjClass *		Model;
	float								BlinkRate;		//(blinks per minute)
	
	unsigned int					CreationTime;
	
	// Not Implemented
	VehicleDazzleClass(const VehicleDazzleClass & that);
	VehicleDazzleClass & operator = (const VehicleDazzleClass & that);
};


#endif // VEHICLEDAZZLE_H

