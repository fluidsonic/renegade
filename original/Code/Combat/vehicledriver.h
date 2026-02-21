#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __VEHICLEDRIVER_H
#define __VEHICLEDRIVER_H

#include "vector3.h"


////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class PathClass;
class SmartGameObj;
class ChunkSaveClass;
class ChunkLoadClass;
class Matrix3D;


////////////////////////////////////////////////////////////////
//
//	VehicleDriverClass
//
////////////////////////////////////////////////////////////////
class VehicleDriverClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	VehicleDriverClass (void);
	virtual ~VehicleDriverClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	void					Initialize (SmartGameObj *game_obj, PathClass *path);
	void					Reset (void);
	
	void					Set_Dest_Position (const Vector3 &pos)	{ m_FinalDest = pos; }
	void					Set_Next_Position (const Vector3 &pos)	{ m_CurrentDest = pos; }
	bool					Drive (void);

	void					Force_Backup (bool onoff);
	bool					Is_Backup_Forced (void) const;

	void					Get_Velocity (const Matrix3D &tm, Vector3 &vel_vector);

	void					Set_Max_Speed (float max_speed)	{ m_MaxSpeed = max_speed; }
	float					Get_Max_Speed (void) const			{ return m_MaxSpeed;}

	void					Set_Speed_Factor (float factor)	{ m_SpeedFactor = factor; }
	float					Get_Speed_Factor (void) const		{ return m_SpeedFactor; }

	void					Set_Arrived_Dist (float dist)		{ m_ArrivedDist = dist; }
	float					Get_Arrived_Dist (void) const		{ return m_ArrivedDist; }

	//
	//	Save/load support
	//
	void					Save (ChunkSaveClass &csave);
	void					Load (ChunkLoadClass &cload);	


protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void					Free () { }
	bool					Are_We_Stuck (const Vector3 &curr_pos);
	void					Apply_Controls (float forward_accel, float left_accel);
	void					Load_Variables (ChunkLoadClass &cload);

	bool					Drive_Tracked (void);
	bool					Drive_Wheeled (void);

	float					Calculate_Brake (float dist_to_goal, float expected_velocity, float curr_speed, float max_speed);

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	Vector3				m_CurrentDest;
	Vector3				m_FinalDest;
	PathClass *			m_CurrentPath;
	SmartGameObj *		m_GameObj;
	float					m_MaxSpeed;
	float					m_SpeedFactor;
	float					m_ArrivedDist;

	Vector3				m_LastPos;
	int					m_BadProgressCount;

	bool					m_IsBackingUp;
	bool					m_IsBackupLocked;
	bool					m_TurnOffEngineWhenDone;
	//bool					m_IsBrakingForEnd;
	float					m_BrakingDist;

	float					m_LastFrameExpectedVelocity;
};


#endif //__VEHICLEDRIVER_H

