#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __VIS_POINT_DEFINITION_H
#define __VIS_POINT_DEFINITION_H

#include "definition.h"
#include "vector.h"
#include "wwstring.h"
#include "editorchunkids.h"


//////////////////////////////////////////////////////////////////////////////////
//
//	VisPointDefinitionClass
//
//////////////////////////////////////////////////////////////////////////////////
class VisPointDefinitionClass : public DefinitionClass
{

public:

	/////////////////////////////////////////////////////////////////////
	//	Editable interface requirements
	/////////////////////////////////////////////////////////////////////
	DECLARE_EDITABLE(VisPointDefinitionClass, DefinitionClass);

	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	VisPointDefinitionClass (void);
	virtual ~VisPointDefinitionClass (void);

	// From DefinitionClass
	virtual uint32								Get_Class_ID (void) const { return CLASSID_VIS_POINT_DEF; }

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual PersistClass *					Create (void) const;

private:

};



#endif //__VIS_POINT_DEFINITION_H

