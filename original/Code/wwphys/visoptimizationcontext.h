#pragma once

#include "global.h"

#include "vector.h"

class PhysicsSceneClass;
class VisTableMgrClass;
class VisTableClass;
class VisOptProgressClass;
class DynamicAABTreeCullClass;

/**
** VisOptimizationContextClass
** This class encapsulates information needed to optimize the precalculated visibility data
** for a level.  It is passed into the dynamic culling system for pruning the useless leaf
** nodes.  It also performs merging of both sector and object ids.
*/
class VisOptimizationContextClass
{	
public:

	VisOptimizationContextClass(PhysicsSceneClass * scene,VisOptProgressClass & stats);
	~VisOptimizationContextClass(void);

	void							Optimize(VisTableMgrClass * vismgr,DynamicAABTreeCullClass * dyn_obj_tree);
	VisOptProgressClass &	Get_Progress_Object(void);

	float							Compute_Sector_Table_Match_Fraction(int sector_id_0,int sector_id_1);
	float							Compute_Object_Table_Match_Fraction(int object_id_0,int object_id_1);

	void							Combine_Sector_Tables(int sector_id_0,int sector_id_1);
	void							Combine_Object_Tables(int object_id_0,int object_id_1);

	int							Get_Vis_Sector_Count(void);
	int							Get_Vis_Object_Count(void);

	void							Set_Dyn_Cell_Prune_Match_Fraction(float frac)		{ if ((frac >= 0.0f) && (frac <= 1.0f)) MinDynCellPruneMatchFraction = frac; }
	void							Set_Vis_Object_Match_Fraction(float frac)				{ if ((frac >= 0.0f) && (frac <= 1.0f)) MinVisObjectMatchFraction = frac; }
	void							Set_Vis_Sector_Match_Fraction(float frac)				{ if ((frac >= 0.0f) && (frac <= 1.0f)) MinVisSectorMatchFraction = frac; }

	float							Get_Dyn_Cell_Prune_Match_Fraction(void)			{ return MinDynCellPruneMatchFraction; }
	float							Get_Vis_Object_Match_Fraction(void)					{ return MinVisObjectMatchFraction; }
	float							Get_Vis_Sector_Match_Fraction(void)					{ return MinVisSectorMatchFraction; }

protected:

	void							Build_Object_Tables(VisTableMgrClass * vismgr);
	void							Combine_Redundant_Objects(void);
	void							Build_Sector_Tables_From_Object_Tables(VisTableMgrClass * vismgr);
	void							Combine_Redundant_Sectors(void);
	void							Install_Results(VisTableMgrClass * vismgr);

	PhysicsSceneClass *		Scene;

	struct PVSInfoStruct
	{
		PVSInfoStruct(void);
		~PVSInfoStruct(void);
		const PVSInfoStruct & operator = (const PVSInfoStruct & that);

		bool operator == (const PVSInfoStruct & that)				{ return false; }
		bool operator != (const PVSInfoStruct & that)				{ return true; }

		VisTableClass *		Table;
		bool						UnUsed;
	};

	float												MinDynCellPruneMatchFraction;
	float												MinVisObjectMatchFraction;
	float												MinVisSectorMatchFraction;

	DynamicVectorClass<PVSInfoStruct>		SectorTables;		// PVS and info for each sector
	DynamicVectorClass<PVSInfoStruct>		ObjectTables;		// Sector visibility and info for each object

	VisOptProgressClass &						Stats;				// Progress and statistics tracker object.
};
