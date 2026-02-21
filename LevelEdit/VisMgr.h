#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __VISMGR_H
#define __VISMGR_H

#include "vector.h"


/////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////
class ChunkLoadClass;
class ChunkSaveClass;
class StaticPhysClass;
class Matrix3D;
class NodeClass;


/////////////////////////////////////////////////////////////////////////
//	Typedefs
/////////////////////////////////////////////////////////////////////////
typedef bool (*VIS_POINT_RENDERED_CALLBACK) (DWORD milliseconds, DWORD param);


/////////////////////////////////////////////////////////////////////////
//
//	VisMgrClass
//
/////////////////////////////////////////////////////////////////////////
class VisMgrClass
{
	public:

		//////////////////////////////////////////////////////////
		//	Public methods
		//////////////////////////////////////////////////////////

		//
		//	Vis remapping (exporting vis from one level and importing
		// it into another).
		//
		static void		Import_Remap_Data (ChunkLoadClass &cload);
		static void		Export_Remap_Data (ChunkSaveClass &csave);

		//
		//	Vis generation
		//
		static void		Build_Node_List (DynamicVectorClass<NodeClass *> &node_list, bool selection_only = false);
		static void		Render_Manual_Vis_Points (bool farm_mode = false, int processor_index = 0, int total_processors = 1, VIS_POINT_RENDERED_CALLBACK callback = NULL, DWORD param = 0);

	protected:

		//////////////////////////////////////////////////////////
		//	Protected methods
		//////////////////////////////////////////////////////////
		
		//
		//	Utility methods
		//
		static StaticPhysClass *	Find_Static_Phys_Object (LPCTSTR mesh_name, const Matrix3D &tm);
		static void						Add_Nodes_To_List (DynamicVectorClass<NodeClass *> &node_list, NodeClass *node);


	private:

		//////////////////////////////////////////////////////////
		//	Private member data
		//////////////////////////////////////////////////////////
};


#endif //__VISMGR_H
