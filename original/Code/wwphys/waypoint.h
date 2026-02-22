#pragma once

#include "global.h"

#include "persist.h"
#include "vector3.h"
#include "refcount.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class PathfindActionPortalClass;

////////////////////////////////////////////////////////////////
//
//	WaypointClass
//
////////////////////////////////////////////////////////////////
class WaypointClass : public PersistClass, public RefCountClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public flags
	////////////////////////////////////////////////////////////////	
	typedef enum
	{
		FLAG_REQUIRES_JUMP	= 0x00000001,
		FLAG_REQUIRES_ACTION

	} FLAGS;

	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////
	WaypointClass (void);
	WaypointClass (const WaypointClass &src);
	WaypointClass (const Vector3 &position);
	virtual ~WaypointClass (void);

	////////////////////////////////////////////////////////////////
	//	Public operators
	////////////////////////////////////////////////////////////////
	const WaypointClass &	operator= (const WaypointClass &src);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Identifaction methods
	//
	int						Get_ID (void) const						{ return m_ID; }
	void						Set_ID (int id)							{ m_ID = id; }

	//
	//	Position methods
	//
	const Vector3 &		Get_Position (void) const				{ return m_Position; }
	void						Set_Position (const Vector3 &pos)	{ m_Position = pos; }

	//
	//	Flags
	//
	int						Get_Flags (void) const					{ return m_Flags; }
	void						Set_Flags (int flags)					{ m_Flags = flags; }
	bool						Get_Flag (int flag)						{ return bool((m_Flags & flag) == flag); }
	void						Set_Flag (int flag, bool onoff);	

	//
	//	Action access
	//
	PathfindActionPortalClass *	Get_Action_Portal (void);
	void									Set_Action_Portal (PathfindActionPortalClass *portal);

	//
	//	Serialization methods (from PersistClass)
	//
	const PersistFactoryClass &	Get_Factory (void) const;
	bool									Save (ChunkSaveClass &csave);
	bool									Load (ChunkLoadClass &cload);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void						Free (void) {}
	bool						Load_Variables (ChunkLoadClass &cload);

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	int						m_ID;
	Vector3					m_Position;
	int						m_Flags;
	int						m_ActionPortalID;
};

//////////////////////////////////////////////////////////////////////////////
//	Set_Flag
//////////////////////////////////////////////////////////////////////////////
inline void
WaypointClass::Set_Flag (int flag, bool onoff)
{
	m_Flags &= ~flag;
	if (onoff) {
		m_Flags |= flag;
	}

	return ;
}
