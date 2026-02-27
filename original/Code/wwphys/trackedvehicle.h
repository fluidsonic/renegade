#pragma once

#include "global.h"

#include "vehiclephys.h"

class TrackedVehicleDefClass;
class MeshClass;

/**
** TrackedVehicleClass
** This class is used for tanks, APCs, and other tracked vehicles.  Due to the way their 
** treads can run in opposite directions and other differences, I do not derive these from
** MotorVehicleClass
*/
class TrackedVehicleClass : public VehiclePhysClass
{
public:

	TrackedVehicleClass(void);
	virtual ~TrackedVehicleClass(void);
	virtual TrackedVehicleClass * As_TrackedVehicleClass(void) { return this; }
	const TrackedVehicleDefClass * Get_TrackedVehicleDef(void) { return (TrackedVehicleDefClass *)Definition; }

	void								Init(const TrackedVehicleDefClass & def);

	virtual void					Render(RenderInfoClass & rinfo);
	virtual void					Set_Model(RenderObjClass * model);

	/*
	** Tracked vehicles, left side and right side movement.  
	*/
	float								Get_Left_Track_Movement(void)		{ return LeftTrackMovement; }
	float								Get_Right_Track_Movement(void)	{ return RightTrackMovement; }

	/*
	** Save-Load System
	*/
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual void								On_Post_Load (void);

	enum { LEFT_TRACK = 0, RIGHT_TRACK = 1 };

protected:

	virtual SuspensionElementClass *	Alloc_Suspension_Element(void);
	virtual void					Compute_Force_And_Torque(Vector3 * force,Vector3 * torque);
	void								Update_Cached_Model_Parameters(void);
	void								Grab_Track_Mappers(RenderObjClass * model);
	void								Add_Track_Mappers(MeshClass * mesh,int track_type);

	float								LeftTrackTorque;
	float								RightTrackTorque;
	float								LeftTrackMovement;
	float								RightTrackMovement;
	Vector3							LeftTrackLastPosition;
	Vector3							RightTrackLastPosition;

	struct TrackMapperStruct
	{
		TrackMapperStruct(void) : Mapper(NULL), TrackType(LEFT_TRACK) { }
		LinearOffsetTextureMapperClass *		Mapper;
		int											TrackType;
	};
	
	SimpleDynVecClass<TrackMapperStruct>	TrackMappers;

private:

	// not implemented
	TrackedVehicleClass(const TrackedVehicleClass &);
	TrackedVehicleClass & operator = (const TrackedVehicleClass &);

};

/**
** TrackedVehicleDefClass
** Initialization/Editor Integration for TrackedVehicleClass
*/
class TrackedVehicleDefClass : public VehiclePhysDefClass
{
public:
	TrackedVehicleDefClass(void);
	
	// From DefinitionClass
	virtual uint32_t								Get_Class_ID (void) const;
	virtual PersistClass *					Create(void) const;

	// From PhysDefClass
	virtual const char *						Get_Type_Name(void)				{ return "TrackedVehicleDef"; }
	virtual bool								Is_Type(const char *);

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save(ChunkSaveClass &csave);
	virtual bool								Load(ChunkLoadClass &cload);

	// Read access to the constants
	float											Get_Max_Engine_Torque(void) const { return MaxEngineTorque; }
	float											Get_Track_U_Scale_Factor(void) const { return TrackUScaleFactor; }
	float											Get_Track_V_Scale_Factor(void) const { return TrackVScaleFactor; }
	float											Get_Turn_Torque_Scale_Factor(void) const { return TurnTorqueScaleFactor; }

	// Write access (DEBUGGING/TESTING ONLY)
	void											Set_Max_Engine_Torque(float t) { MaxEngineTorque = t; }
	void											Set_Track_U_Scale_Factor(float k) { TrackUScaleFactor = k; }
	void											Set_Track_V_Scale_Factor(float k) { TrackVScaleFactor = k; }
	void											Set_Turn_Torque_Scale_Factor(float k) { TurnTorqueScaleFactor = k; }

	//	Editable interface requirements

protected:

	float											MaxEngineTorque;
	float											TrackUScaleFactor;
	float											TrackVScaleFactor;
	float											TurnTorqueScaleFactor;

	friend class TrackedVehicleClass;
};
