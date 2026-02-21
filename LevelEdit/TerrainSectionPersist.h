#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __TERRAIN_SECTION_PERSIST_H
#define __TERRAIN_SECTION_PERSIST_H


#include "bittype.h"
#include "vector3.h"
#include "wwstring.h"
#include "vector.h"


////////////////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////////////////
class NodeClass;
class ChunkSaveClass;
class ChunkLoadClass;
class TerrainSectionPersistClass;
class TerrainNodeClass;

////////////////////////////////////////////////////////////////////////////
//
//	TerrainSectionPersistListClass
//
////////////////////////////////////////////////////////////////////////////
class TerrainSectionPersistListClass : public DynamicVectorClass<TerrainSectionPersistClass *>
{
public:

	//////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////
	virtual ~TerrainSectionPersistListClass (void) { Free_List (); }

	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////
	void		Build_List (DynamicVectorClass<NodeClass *> &node_list);
	void		Assign_Section_IDs (TerrainNodeClass *node);
	void		Initialize_Virgin_Sections (void);
	void		Free_List (void);	

	void		Save (ChunkSaveClass &csave);
	void		Load (ChunkLoadClass &cload);

private:

	//////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////
	DynamicVectorClass<NodeClass *>	m_VirginSections;
};


////////////////////////////////////////////////////////////////////////////
//
//	TerrainSectionPersistClass
//
////////////////////////////////////////////////////////////////////////////
class TerrainSectionPersistClass
{

public:
	
	//////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////////
	TerrainSectionPersistClass (void);
	virtual ~TerrainSectionPersistClass (void);

	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////
	
	//
	// Initialization
	//
	void			Initialize (NodeClass *node);
	void			Apply (NodeClass *node);
	void			Initialize_Virgin_Sections (void)			{ m_TerrainSectionInfo.Initialize_Virgin_Sections (); }

	//
	//	Save/load stuff
	//
	bool			Save (ChunkSaveClass &csave);
	bool			Load (ChunkLoadClass &cload);

	//
	//	Accessors
	//
	const Vector3 &		Get_Position (void) const			{ return m_Position; }
	uint32					Get_Def_ID (void) const				{ return m_DefinitionID; }
	int						Get_Instance_ID (void) const		{ return m_InstanceID; }
	uint32					Get_Vis_Obj_ID (void) const		{ return m_VisObjectID; }
	uint32					Get_Vis_Sector_ID (void) const	{ return m_VisSectorID; }
	uint32					Get_Cull_Link (void) const			{ return m_CullLink; }
	const StringClass &	Get_Name (void) const				{ return m_Name; }

protected:

	//////////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////////
	bool			Save_Variables (ChunkSaveClass &csave);
	bool			Load_Variables (ChunkLoadClass &cload);

private:

	//////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////
	Vector3									m_Position;
	uint32									m_DefinitionID;
	int										m_InstanceID;
	uint32									m_VisObjectID;
	uint32									m_VisSectorID;
	uint32									m_CullLink;
	StringClass								m_Name;
	TerrainSectionPersistListClass	m_TerrainSectionInfo;	
};


#endif //__TERRAIN_SECTION_PERSIST_H

