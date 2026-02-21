#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __DUMMY_OBJ_DEFINITION_H
#define __DUMMY_OBJ_DEFINITION_H

#include "definition.h"
#include "vector.h"
#include "wwstring.h"
#include "editorchunkids.h"


//////////////////////////////////////////////////////////////////////////////////
//
//	DummyObjectDefinitionClass
//
//////////////////////////////////////////////////////////////////////////////////
class DummyObjectDefinitionClass : public DefinitionClass
{

public:

	/////////////////////////////////////////////////////////////////////
	//	Editable interface requirements
	/////////////////////////////////////////////////////////////////////
	DECLARE_EDITABLE(DummyObjectDefinitionClass, DefinitionClass);

	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	DummyObjectDefinitionClass (void);
	virtual ~DummyObjectDefinitionClass (void);

	// From DefinitionClass
	virtual uint32								Get_Class_ID (void) const { return CLASSID_DUMMY_OBJECTS; }

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual PersistClass *					Create (void) const;

	// Terrain definition specific
	virtual const char *						Get_Model_Name (void) const;

private:

	/////////////////////////////////////////////////////////////////////
	//	Private methods
	/////////////////////////////////////////////////////////////////////
	bool											Save_Variables (ChunkSaveClass &csave);
	bool											Load_Variables (ChunkLoadClass &cload);

	//////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////
	StringClass									m_ModelName;
};


/////////////////////////////////////////////////////////////////////
//	Get_Model_Name
/////////////////////////////////////////////////////////////////////
inline const char *
DummyObjectDefinitionClass::Get_Model_Name (void) const
{
	return m_ModelName;
}


#endif //__DUMMY_OBJ_DEFINITION_H

