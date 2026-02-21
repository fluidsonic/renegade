
#ifndef __WAYPATH_H
#define __WAYPATH_H

#include "persist.h"
#include "refcount.h"
#include "vector.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class WaypointClass;
class WaypathPositionClass;
class Vector3;

////////////////////////////////////////////////////////////////
//
//	WaypathClass
//
////////////////////////////////////////////////////////////////
class WaypathClass : public PersistClass, public RefCountClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public flags
	////////////////////////////////////////////////////////////////
	typedef enum
	{
		FLAG_TWO_WAY				= 0x00000001,
		FLAG_LOOPING				= 0x00000002,
		FLAG_HUMAN					= 0x00000004,
		FLAG_GROUND_VEHICLE		= 0x00000008,
		FLAG_FLYING_VEHICLE		= 0x00000010,
		FLAG_INNATE_PATHFIND		= 0x00000020
		
	} FLAGS;

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	WaypathClass (void);
	WaypathClass (const WaypathPositionClass &start, const WaypathPositionClass &end);
	virtual ~WaypathClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Identifaction methods
	//
	int						Get_ID (void) const						{ return m_ID; }
	void						Set_ID (int id)							{ m_ID = id; }

	//
	//	Waypoint methods
	//
	int						Get_Point_Count (void) const			{ return m_Waypoints.Count (); }
	WaypointClass *		Get_Point (int index) const;
	void						Add_Point (const WaypointClass &point);
	void						Add_Point (const Vector3 &point);

	//
	//	Flags
	//
	int						Get_Flags (void) const					{ return m_Flags; }
	void						Set_Flags (int flags)					{ m_Flags = flags; }
	bool						Get_Flag (int flag)						{ return bool((m_Flags & flag) == flag); }
	void						Set_Flag (int flag, bool onoff);

	//
	//	Position methods
	//
	bool						Evaluate_Position (const WaypathPositionClass &pos, Vector3 *position);
	
	//
	//	Serialization methods (from PersistClass)
	//
	const PersistFactoryClass &	Get_Factory (void) const;
	bool						Save (ChunkSaveClass &csave);
	bool						Load (ChunkLoadClass &cload);
	void						On_Post_Load (void);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void						Free (void);
	bool						Load_Variables (ChunkLoadClass &cload);		

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	int						m_ID;
	int						m_Flags;
	DynamicVectorClass<WaypointClass *>	m_Waypoints;

};

//////////////////////////////////////////////////////////////////////////////
//	Get_Point
//////////////////////////////////////////////////////////////////////////////
inline WaypointClass *
WaypathClass::Get_Point (int index) const
{
	WaypointClass *waypoint = NULL;

	//
	//	Check to ensure the requested index is valid
	//
	if (index >= 0 && index < m_Waypoints.Count ()) {
		waypoint = m_Waypoints[index];
	}

	return waypoint;
}

#endif //__WAYPATH_H
