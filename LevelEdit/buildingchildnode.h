#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __BUILDINGCHILDNODE_H
#define __BUILDINGCHILDNODE_H

#include "node.h"
#include "vector.h"
#include "icons.h"
#include "decophys.h"
#include "buildingnode.h"


////////////////////////////////////////////////////////////////////////////
// Forward declarations
////////////////////////////////////////////////////////////////////////////
class PresetClass;


////////////////////////////////////////////////////////////////////////////
//
//	BuildingChildNodeClass
//
////////////////////////////////////////////////////////////////////////////
class BuildingChildNodeClass : public NodeClass
{
public:
	
	//////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////
	BuildingChildNodeClass (PresetClass *preset = NULL);
	BuildingChildNodeClass (const BuildingChildNodeClass &src);
	~BuildingChildNodeClass (void);

	//////////////////////////////////////////////////////////////
	//	Public operators
	//////////////////////////////////////////////////////////////
	const BuildingChildNodeClass &operator= (const BuildingChildNodeClass &src);

	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////

	//
	// BuildingChildNodeClass specific
	//
	BuildingNodeClass *	Peek_Building (void) const					{ return Building; }
	void						Set_Building (BuildingNodeClass *node) { Building = node; }

	//
	// From PersistClass
	//
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	
	//
	// From NodeClass
	//
	void			Initialize (void);
	NodeClass *	Clone (void)								{ return new BuildingChildNodeClass (*this); }
	NODE_TYPE	Get_Type (void) const					{ return NODE_TYPE_BUILDING_CHILD_NODE; }
	int			Get_Icon_Index (void) const			{ return BUILDING_ICON; }
	PhysClass *	Peek_Physics_Obj (void)	const			{ return PhysObj; }
	bool			Is_Static (void) const					{ return false; }
	bool			Show_Settings_Dialog (void)			{ return true; }
	bool			Can_Be_Rotated_Freely (void) const	{ return true; }
	void			On_Delete (void);
	NodeClass *	Get_Parent_Node (void) const			{ return Building; }

	//
	//	Notifications
	//
	void			On_Rotate (void);
	void			On_Translate (void);
	void			On_Transform (void);
	//
	//	Export methods
	//
	void			Pre_Export (void);
	void			Post_Export (void);


	//	From PersistClass
	bool			Save (ChunkSaveClass &csave);
	bool			Load (ChunkLoadClass &cload);

protected:

	//////////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////////
	bool			Load_Variables (ChunkLoadClass &cload);

	//////////////////////////////////////////////////////////////////
	//	Protected member data
	//////////////////////////////////////////////////////////////////
	DecorationPhysClass *	PhysObj;
	BuildingNodeClass *		Building;
};


//////////////////////////////////////////////////////////////////
//	On_Rotate
//////////////////////////////////////////////////////////////////
inline void
BuildingChildNodeClass::On_Rotate (void)
{	
	if (Building != NULL) {
		Building->Update_Lines ();
	}

	NodeClass::On_Rotate ();
	return ;
}


//////////////////////////////////////////////////////////////////
//	On_Translate
//////////////////////////////////////////////////////////////////
inline void
BuildingChildNodeClass::On_Translate (void)
{
	if (Building != NULL) {
		Building->Update_Lines ();
	}

	NodeClass::On_Translate ();
	return ;
}


//////////////////////////////////////////////////////////////////
//	On_Transform
//////////////////////////////////////////////////////////////////
inline void
BuildingChildNodeClass::On_Transform (void)
{
	if (Building != NULL) {
		Building->Update_Lines ();
	}

	NodeClass::On_Transform ();
	return ;
}


#endif //__BUILDINGCHILDNODE_H

