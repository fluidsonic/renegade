#include "wheelvehicle.h"
#include "rendobj.h"
#include "lineseg.h"
#include "physcoltest.h"
#include "pscene.h"
#include "physcontrol.h"
#include "wwphysids.h"
#include "persistfactory.h"
#include "simpledefinitionfactory.h"
#include "wheel.h"
#include <string.h>

												
/*
** Declare a PersistFactory for WheeledVehicleClasses
*/
SimplePersistFactoryClass<WheeledVehicleClass,PHYSICS_CHUNKID_WHEELEDVEHICLE>	_WheeledVehicleFactory;

/*
** Chunk-ID's used by wheeled vehicle class
*/
enum 
{
	WHEELEDVEHICLE_CHUNK_MOTORVEHICLE		= 0x00119801,
	WHEELEDVEHICLE_CHUNK_VARIABLES,
	WV_VARIABLE_STEERINGANGLE					= 0x00,
	OBSOLETE_WV_VARIABLE_MAXSTEERINGANGLE,
	OBSOLETE_WV_VARIABLE_SPRINGCONSTANT,
	OBSOLETE_WV_VARIABLE_DAMPINGCONSTANT,
	OBSOLETE_WV_VARIABLE_SPRINGLENGTH,
};

// Steering constant defaults
const float		DEFAULT_MAX_STEER_ANGLE					= (float)WWMATH_PI/4.0f;

// Debugging colors
const Vector3	SPRING_COLOR(1.0f,0.5f,0.0f);
const Vector3	SUSPENSION_FORCE_COLOR(1.0f,0.0f,0.0f);

WheeledVehicleClass::WheeledVehicleClass(void) :
	SteeringAngle(0)
{
}
 
void WheeledVehicleClass::Init(const WheeledVehicleDefClass & def)
{
	MotorVehicleClass::Init(def);	
}

WheeledVehicleClass::~WheeledVehicleClass(void)
{
}

SuspensionElementClass * WheeledVehicleClass::Alloc_Suspension_Element(void)
{
	return new WVWheelClass;
}

void WheeledVehicleClass::Compute_Force_And_Torque(Vector3 * force,Vector3 * torque)
{
	{

		const WheeledVehicleDefClass * def = Get_WheeledVehicleDef();

		/*
		** Compute the steering angle for normal vehicles
		*/
		if (Controller) {
			SteeringAngle = Controller->Get_Turn_Left() * def->MaxSteeringAngle;
		} else {
			SteeringAngle = 0.0f;
		}

		/*
		** Compute the steering angle for "leaning" vehicles (motorcycles)
		** (gth) Tilt steering isn't working out very well, changing to normal steering
		*/
		float tilt_steer_angle = SteeringAngle;

		/*
		** Compute forces and torques for each wheel.
		*/
		for (int iwheel = 0; iwheel<Wheels.Length(); iwheel++) {
			
			/*
			** update the steering input to the wheel
			*/
			if (Wheels[iwheel]->Get_Flag(SuspensionElementClass::STEERING)) {
				Wheels[iwheel]->Set_Steering_Angle(SteeringAngle);
			} else if (Wheels[iwheel]->Get_Flag(SuspensionElementClass::INV_STEERING)) {
				Wheels[iwheel]->Set_Steering_Angle(-SteeringAngle);
			} else if (Wheels[iwheel]->Get_Flag(SuspensionElementClass::TILT_STEERING)) {
				Wheels[iwheel]->Set_Steering_Angle(tilt_steer_angle);
			}

			/*
			** update the braking state of the wheel
			*/
			Wheels[iwheel]->Set_Flag(SuspensionElementClass::BRAKING,Is_Braking());

			/*
			** update the engine torque
			*/
			if (Wheels[iwheel]->Get_Flag(SuspensionElementClass::ENGINE)) {
				((WVWheelClass *)Wheels[iwheel])->Set_Axle_Torque(Get_Axle_Torque() / Get_Drive_Wheel_Count() );
			}
		}	
		
		/*
		** Let base class add in its forces
		*/
	}

	MotorVehicleClass::Compute_Force_And_Torque(force,torque);
}

float WheeledVehicleClass::Get_Ideal_Drive_Axle_Angular_Velocity(void)
{
	// return the highest angular velocity of any of the drive wheels
	float avel = -10000.0f;
	for (int i=0; i<Wheels.Length(); i++) {
		avel = ((WVWheelClass *)Wheels[i])->Get_Ideal_Drive_Wheel_Angular_Velocity(avel);
	}
	return avel;
}

bool WheeledVehicleClass::Drive_Wheels_In_Contact(void)
{
	for (int i=0; i<Wheels.Length(); i++) {
		if (	Wheels[i]->Get_Flag(SuspensionElementClass::ENGINE) && Wheels[i]->Get_Flag(SuspensionElementClass::INCONTACT)) {
			return true;
		}
	}
	return false;
}

/*
** Save-Load System
*/
const PersistFactoryClass & WheeledVehicleClass::Get_Factory (void) const
{
	return _WheeledVehicleFactory;
}

bool WheeledVehicleClass::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk(WHEELEDVEHICLE_CHUNK_MOTORVEHICLE);
	MotorVehicleClass::Save(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(WHEELEDVEHICLE_CHUNK_VARIABLES);
	WRITE_MICRO_CHUNK(csave,WV_VARIABLE_STEERINGANGLE,SteeringAngle);
	csave.End_Chunk();
	return true;
}

bool WheeledVehicleClass::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk()) {
		
		switch(cload.Cur_Chunk_ID()) 
		{
			case WHEELEDVEHICLE_CHUNK_MOTORVEHICLE:
				MotorVehicleClass::Load(cload);
				break;

			case WHEELEDVEHICLE_CHUNK_VARIABLES:
			
				while (cload.Open_Micro_Chunk()) {
					switch(cload.Cur_Micro_Chunk_ID()) {
						READ_MICRO_CHUNK(cload,WV_VARIABLE_STEERINGANGLE,SteeringAngle);
						OBSOLETE_MICRO_CHUNK(OBSOLETE_WV_VARIABLE_MAXSTEERINGANGLE);
						OBSOLETE_MICRO_CHUNK(OBSOLETE_WV_VARIABLE_SPRINGCONSTANT);
						OBSOLETE_MICRO_CHUNK(OBSOLETE_WV_VARIABLE_DAMPINGCONSTANT);
						OBSOLETE_MICRO_CHUNK(OBSOLETE_WV_VARIABLE_SPRINGLENGTH);
					}
					cload.Close_Micro_Chunk();	
				}
				break;

			default:
				break;
		}
		cload.Close_Chunk();
	}

	return true;
}

/***********************************************************************************************
**
** WheeledVehicleDefClass Implementation
**
***********************************************************************************************/

SimplePersistFactoryClass<WheeledVehicleDefClass,PHYSICS_CHUNKID_WHEELEDVEHICLEDEF>	_WheeledVehicleDefFactory;
DECLARE_DEFINITION_FACTORY(WheeledVehicleDefClass, CLASSID_WHEELEDVEHICLEDEF, "WheeledVehicle") _WheeledVehicleDefDefFactory;

/*
** Chunk ID's used by WheeledVehicleDefClass
*/
enum 
{
	WHEELEDVEHICLEDEF_CHUNK_MOTORVEHICLEDEF				= 0x00990066,			// (parent class)
	WHEELEDVEHICLEDEF_CHUNK_VARIABLES,

	WHEELEDVEHICLEDEF_VARIABLE_MAXSTEERINGANGLE			= 0x00,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_SPRINGCONSTANT,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_DAMPINGCONSTANT,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_SPRINGLENGTH,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_TRACTIVEFRICTIONCURVEFILENAME,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_LATERALFRICTIONCURVEFILENAME,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_TRACTIONMULTIPLIER,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_LATERALMOMENTARM,
	OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_TRACTIVEMOMENTARM
};

WheeledVehicleDefClass::WheeledVehicleDefClass(void) :
	MaxSteeringAngle(DEFAULT_MAX_STEER_ANGLE)
{
	// make our parameters editable!
	ANGLE_EDITABLE_PARAM(WheeledVehicleDefClass, MaxSteeringAngle, DEG_TO_RADF(0.0f), DEG_TO_RADF(90.0f));
}

uint32_t WheeledVehicleDefClass::Get_Class_ID (void) const	
{ 
	return CLASSID_WHEELEDVEHICLEDEF; 
}

const PersistFactoryClass & WheeledVehicleDefClass::Get_Factory (void) const
{
	return _WheeledVehicleDefFactory;
}

PersistClass * WheeledVehicleDefClass::Create(void) const
{
	WheeledVehicleClass * obj = NEW_REF(WheeledVehicleClass,());
	obj->Init(*this);
	return obj;
}

bool WheeledVehicleDefClass::Save(ChunkSaveClass &csave)
{
	csave.Begin_Chunk(WHEELEDVEHICLEDEF_CHUNK_MOTORVEHICLEDEF);
	MotorVehicleDefClass::Save(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(WHEELEDVEHICLEDEF_CHUNK_VARIABLES);
	WRITE_MICRO_CHUNK(csave,WHEELEDVEHICLEDEF_VARIABLE_MAXSTEERINGANGLE,MaxSteeringAngle);
	csave.End_Chunk();

	return true;
}

bool WheeledVehicleDefClass::Load(ChunkLoadClass &cload)
{
	while (cload.Open_Chunk()) {

		switch(cload.Cur_Chunk_ID()) {

			case WHEELEDVEHICLEDEF_CHUNK_MOTORVEHICLEDEF:
				MotorVehicleDefClass::Load(cload);
				break;

			case WHEELEDVEHICLEDEF_CHUNK_VARIABLES:
				while (cload.Open_Micro_Chunk()) {
					switch(cload.Cur_Micro_Chunk_ID()) {
						
						READ_MICRO_CHUNK(cload,WHEELEDVEHICLEDEF_VARIABLE_MAXSTEERINGANGLE,MaxSteeringAngle);
						
						// NOTE: these variables are now in VehiclePhysDefClass...
						READ_MICRO_CHUNK(cload,OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_SPRINGCONSTANT,SpringConstant);
						READ_MICRO_CHUNK(cload,OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_DAMPINGCONSTANT,DampingConstant);
						READ_MICRO_CHUNK(cload,OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_SPRINGLENGTH,SpringLength);
						READ_MICRO_CHUNK(cload,OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_TRACTIONMULTIPLIER,TractionMultiplier);
						READ_MICRO_CHUNK(cload,OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_LATERALMOMENTARM,LateralMomentArm);
						READ_MICRO_CHUNK(cload,OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_TRACTIVEMOMENTARM,TractiveMomentArm);

						OBSOLETE_MICRO_CHUNK(OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_TRACTIVEFRICTIONCURVEFILENAME);
						OBSOLETE_MICRO_CHUNK(OBSOLETE_WHEELEDVEHICLEDEF_VARIABLE_LATERALFRICTIONCURVEFILENAME);
					}
					cload.Close_Micro_Chunk();
				}
				break;

			default:
				break;
		}

		cload.Close_Chunk();
	}

	return true;
}

bool WheeledVehicleDefClass::Is_Type(const char * type_name)
{
	if (stricmp(type_name,WheeledVehicleDefClass::Get_Type_Name()) == 0) {
		return true;
	} else {
		return MotorVehicleDefClass::Is_Type(type_name);
	}
}
