#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __PATH_OBJECT_H
#define __PATH_OBJECT_H


#include "aabox.h"
#include "obbox.h"

// Forward declarations
class PhysClass;


///////////////////////////////////////////////////////////////////////////
//
//	PathObjectClass
//
///////////////////////////////////////////////////////////////////////////
class PathObjectClass
{

public:

	//////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////
	PathObjectClass (void);
	~PathObjectClass (void);


	//////////////////////////////////////////////////////////////////
	//	Public data types
	//////////////////////////////////////////////////////////////////
	typedef enum
	{
		CAN_BACKUP				= 1,
		CAN_BOX_ROTATE			= 2,
		CAN_USE_EQUIPMENT		= 4,
		IS_VEHICLE				= 8
	} Flags;


	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////
	void			Initialize (PhysClass &phys_obj);
	void			Init_Human (void);

	void			Set_Turn_Radius (float radius);
	float			Get_Turn_Radius (void) const;

	void			Set_Wheel_Offset (float offset);
	float			Get_Wheel_Offset (void) const;

	void			Set_Max_Speed (float speed);
	float			Get_Max_Speed (void) const;

	void			Set_Collision_Box (const OBBoxClass &box);
	void			Get_Collision_Box (OBBoxClass &box) const;
	void			Get_Collision_Box (AABoxClass &box) const;
	float			Get_Width (void) const;

	void			Set_Flags (int flag_mask);
	int			Get_Flags (void) const;
	bool			Is_Flag_Set (Flags flag) const;
	void			Set_Flag (Flags flag, bool onoff);

	int			Get_Key_Ring (void) const	{ return m_KeyRing; }
	void			Set_Key_Ring (int keys)		{ m_KeyRing = keys; }

private:

	//////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////
	float			m_TurnRadius;
	float			m_MaxSpeed;
	float			m_WheelOffset;
	OBBoxClass	m_CollisionBox;
	int			m_Flags;
	int			m_KeyRing;
};


//////////////////////////////////////////////////////////////////
//	Inlines
//////////////////////////////////////////////////////////////////

inline void PathObjectClass::Set_Flags (int flag_mask)
{
	m_Flags = flag_mask;
	return ;
}

inline int PathObjectClass::Get_Flags (void) const
{
	return m_Flags;
}

inline bool PathObjectClass::Is_Flag_Set (Flags flag) const
{
	return bool((m_Flags & flag) == flag);
}

inline void PathObjectClass::Set_Flag (Flags flag, bool onoff)
{
	if (onoff) {
		m_Flags |= flag;
	} else {
		m_Flags &= (~flag);
	}

	return ;
}

inline void PathObjectClass::Set_Max_Speed (float speed)
{
	m_MaxSpeed = speed;
	return ;
}

inline float PathObjectClass::Get_Max_Speed (void) const
{
	return m_MaxSpeed;
}

inline void PathObjectClass::Set_Turn_Radius (float radius)
{
	m_TurnRadius = radius;
	return ;
}

inline float PathObjectClass::Get_Turn_Radius (void) const
{
	return m_TurnRadius;
}

inline void PathObjectClass::Set_Wheel_Offset (float offset)
{
	m_WheelOffset = offset;
	return ;
}

inline float PathObjectClass::Get_Wheel_Offset (void) const
{
	return m_WheelOffset;
}

inline void PathObjectClass::Set_Collision_Box (const OBBoxClass &box)
{
	m_CollisionBox = box;
	return ;
}

inline void PathObjectClass::Get_Collision_Box (OBBoxClass &box) const
{
	box = m_CollisionBox;
	return ;
}

inline void PathObjectClass::Get_Collision_Box (AABoxClass &box) const
{
	box.Center = m_CollisionBox.Center;
	box.Extent = m_CollisionBox.Extent;
	return ;
}

inline float PathObjectClass::Get_Width (void) const
{
	return m_CollisionBox.Extent.Y;
}


#endif //__PATH_OBJECT_H
