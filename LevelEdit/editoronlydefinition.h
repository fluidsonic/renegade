#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __EDITORONLYDEFINITION_H
#define __EDITORONLYDEFINITION_H


#include "definition.h"
#include "vector.h"
#include "wwstring.h"
#include "editorchunkids.h"


//////////////////////////////////////////////////////////////////////
//
//	EditorOnlyDefinitionClass
//
//////////////////////////////////////////////////////////////////////
class EditorOnlyDefinitionClass  : public DefinitionClass
{
public:

	/////////////////////////////////////////////////////////////////////
	//	Editable interface requirements
	/////////////////////////////////////////////////////////////////////
	DECLARE_EDITABLE(EditorOnlyDefinitionClass, DefinitionClass);

	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	EditorOnlyDefinitionClass (void);
	virtual ~EditorOnlyDefinitionClass (void);

	//
	// Inherited
	//
	virtual uint32								Get_Class_ID (void) const { return CLASSID_EDITOR_ONLY_OBJECTS; }
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual PersistClass *					Create (void) const;

	//
	// Accessors
	//
	virtual const char *						Get_Model_Name (void) const	{ return ModelName; }

private:

	/////////////////////////////////////////////////////////////////////
	//	Private methods
	/////////////////////////////////////////////////////////////////////
	bool											Save_Variables (ChunkSaveClass &csave);
	bool											Load_Variables (ChunkLoadClass &cload);

	//////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////
	StringClass									ModelName;
};


#endif //__EDITORONLYDEFINITION_H
