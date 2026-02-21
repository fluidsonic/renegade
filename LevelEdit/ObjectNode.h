#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __OBJECT_NODE_H
#define __OBJECT_NODE_H

#include "node.h"
#include "icons.h"
#include "physicalgameobj.h"
#include "listtypes.h"

////////////////////////////////////////////////////////////////////////////
// Forward declarations
////////////////////////////////////////////////////////////////////////////
class PresetClass;


////////////////////////////////////////////////////////////////////////////
//
//	ObjectNodeClass
//
////////////////////////////////////////////////////////////////////////////
class ObjectNodeClass : public NodeClass
{
public:
	
	//////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////
	ObjectNodeClass (PresetClass *preset = NULL);
	ObjectNodeClass (const ObjectNodeClass &src);
	~ObjectNodeClass (void);

	//////////////////////////////////////////////////////////////
	//	Public operators
	//////////////////////////////////////////////////////////////
	const ObjectNodeClass &operator= (const ObjectNodeClass &src);

	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////

	//
	// From PersistClass
	//
	virtual const PersistFactoryClass &	Get_Factory (void) const;

	//
	//	RTTI
	//
	ObjectNodeClass *		As_ObjectNodeClass (void)		{ return this; }
	
	//
	// From NodeClass
	//	
	void			Initialize (void);
	NodeClass *	Clone (void)							{ return new ObjectNodeClass (*this); }
	NODE_TYPE	Get_Type (void) const				{ return NODE_TYPE_OBJECT; }	
	int			Get_Icon_Index (void) const		{ return OBJECT_ICON; }
	PhysClass *	Peek_Physics_Obj (void)	const;
	bool			Is_Static (void) const				{ return false; }
	bool			Show_Settings_Dialog (void);
	void			Add_To_Scene (void);
	void			Remove_From_Scene (void);
	void			Set_ID (uint32 id);	

	//
	//	From PersistClass
	//
	bool			Save (ChunkSaveClass &csave);
	bool			Load (ChunkLoadClass &cload);

	//
	// ObjectNode specific	
	//
	void					Copy_Scripts (const ObjectNodeClass &src);
	SCRIPT_LIST &		Get_Scripts (void)				{ return m_Scripts; }	
	PhysicalGameObj *	Peek_Game_Obj (void) const;

protected:

	//////////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////////
	void			Create_Game_Obj (void);
	void			Destroy_Game_Obj (void);
	void			Free_Scripts (void);
	void			Assign_Scripts (void);

	//////////////////////////////////////////////////////////////////
	//	Protected member data
	//////////////////////////////////////////////////////////////////
	ScriptableGameObj *	m_GameObj;
	SCRIPT_LIST				m_Scripts;
};


//////////////////////////////////////////////////////////////////
//	Peek_Game_Obj
//////////////////////////////////////////////////////////////////
inline PhysicalGameObj *	
ObjectNodeClass::Peek_Game_Obj (void) const
{
	PhysicalGameObj *game_obj = NULL;

	if (m_GameObj != NULL) {
		game_obj = m_GameObj->As_PhysicalGameObj ();
	}

	return game_obj;
}

//////////////////////////////////////////////////////////////////
//	Peek_Physics_Obj
//////////////////////////////////////////////////////////////////
inline PhysClass *
ObjectNodeClass::Peek_Physics_Obj (void) const
{
	PhysClass *phys_obj = NULL;

	//
	//	If our game object has a phys object, then return it to the caller.
	//
	PhysicalGameObj *phys_game_obj = Peek_Game_Obj ();
	if (phys_game_obj != NULL) {
		phys_obj = phys_game_obj->Peek_Physical_Object ();
	}

	return phys_obj;
}


#endif //__OBJECT_NODE_H

