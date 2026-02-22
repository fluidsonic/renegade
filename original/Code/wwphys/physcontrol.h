#pragma once

#include "global.h"

#include "vector3.h"

class	ChunkSaveClass;
class	ChunkLoadClass;

class ChunkLoadClass;
class ChunkSaveClass;

/*

	PhysControllerClass

	This is an object which abstractly describes the control state
	for a physics object.  

	PhysControllers are not persistant objects on their own but they
	do provide a save and load method so that you can embed them in
	another object if you want to.

*/

class PhysControllerClass 
{
public:

	PhysControllerClass(void) : MoveVector(0,0,0), TurnLeft(0) { }

	void					Reset(void)									{ Reset_Move(); Reset_Turn(); }

	void					Set_Move_Forward(float scl)			{ MoveVector.X = scl; }
	void					Set_Move_Left(float scl)				{ MoveVector.Y = scl; }
	void					Set_Move_Up(float scl)					{ MoveVector.Z = scl; }
	void					Set_Turn_Left(float scl)				{ TurnLeft = scl; }

	float					Get_Move_Forward(void)					{ return MoveVector.X; }
	float					Get_Move_Left(void)						{ return MoveVector.Y; }
	float					Get_Move_Up(void)							{ return MoveVector.Z; }
	float					Get_Turn_Left(void)						{ return TurnLeft; }

	void					Reset_Move(void)							{ MoveVector.Set(0,0,0); }
	const Vector3	&	Get_Move_Vector(void)					{ return MoveVector; }	
	void					Reset_Turn(void)							{ TurnLeft = 0.0f; }

	bool					Is_Inactive(void);

	bool					Save(ChunkSaveClass & csave);
	bool					Load(ChunkLoadClass & cload);

private:

	Vector3				MoveVector;
	float					TurnLeft;
};

inline bool PhysControllerClass::Is_Inactive(void)
{
	return ((TurnLeft == 0.0f) && (MoveVector.Length2() == 0.0f));
}
