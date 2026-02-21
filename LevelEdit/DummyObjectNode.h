#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DUMMY_OBJECT_NODE_H
#define __DUMMY_OBJECT_NODE_H

#include "node.h"
#include "icons.h"
#include "decophys.h"


// Forward declarations
class PresetClass;


////////////////////////////////////////////////////////////////////////////
//
//	DummyObjectNodeClass
//
////////////////////////////////////////////////////////////////////////////
class DummyObjectNodeClass : public NodeClass
{
public:
	
	//////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////
	DummyObjectNodeClass (PresetClass *preset = NULL);
	DummyObjectNodeClass (const DummyObjectNodeClass &src);
	~DummyObjectNodeClass (void);

	//////////////////////////////////////////////////////////////
	//	Public operators
	//////////////////////////////////////////////////////////////
	const DummyObjectNodeClass &operator= (const DummyObjectNodeClass &src);

	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////

	//
	// From PersistClass
	//
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	
	//
	// From NodeClass
	//
	NodeClass *	Clone (void)							{ return new DummyObjectNodeClass (*this); }
	void			Initialize (void);
	NODE_TYPE	Get_Type (void) const				{ return NODE_TYPE_DUMMY_OBJECT; }
	int			Get_Icon_Index (void) const		{ return OBJECT_ICON; }
	PhysClass *	Peek_Physics_Obj (void)	const;
	bool			Is_Static (void) const				{ return false; }
	void			Add_To_Scene (void);
	void			Remove_From_Scene (void);

	void			Handle_Emitter_Transform (void);

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

protected:

	//////////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////////

private:

	//////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////
	DecorationPhysClass *		m_DisplayObj;
	DecorationPhysClass *		m_RealObj;
};


//////////////////////////////////////////////////////////////////
//	Peek_Physics_Obj
//////////////////////////////////////////////////////////////////
inline PhysClass *
DummyObjectNodeClass::Peek_Physics_Obj (void) const
{
	return m_DisplayObj;
}


//////////////////////////////////////////////////////////////////
//	On_Rotate
//////////////////////////////////////////////////////////////////
inline void
DummyObjectNodeClass::On_Rotate (void)
{
	if (m_RealObj != NULL) {
		m_RealObj->Set_Transform (m_Transform);
		Handle_Emitter_Transform ();
	}
	NodeClass::On_Rotate ();
	return ;
}


//////////////////////////////////////////////////////////////////
//	On_Translate
//////////////////////////////////////////////////////////////////
inline void
DummyObjectNodeClass::On_Translate (void)
{
	if (m_RealObj != NULL) {
		m_RealObj->Set_Transform (m_Transform);
		Handle_Emitter_Transform ();
	}
	NodeClass::On_Translate ();
	return ;
}


//////////////////////////////////////////////////////////////////
//	On_Transform
//////////////////////////////////////////////////////////////////
inline void
DummyObjectNodeClass::On_Transform (void)
{
	if (m_RealObj != NULL) {
		m_RealObj->Set_Transform (m_Transform);
		Handle_Emitter_Transform ();
	}
	NodeClass::On_Transform ();
	return ;
}

#endif //__DUMMY_OBJECT_NODE_H

