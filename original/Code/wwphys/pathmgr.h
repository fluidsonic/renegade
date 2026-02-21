
#ifndef __PATHMGR_H
#define __PATHMGR_H

#include "vector.h"
#include "vector3.h"
#include "bittype.h"

/////////////////////////////////////////////////////////////////////////
// Forward declarations
/////////////////////////////////////////////////////////////////////////
class PathSolveClass;
class ChunkSaveClass;
class ChunkLoadClass;

/////////////////////////////////////////////////////////////////////////
//
//	PathMgrClass
//
/////////////////////////////////////////////////////////////////////////
class PathMgrClass
{
public:

	/////////////////////////////////////////////////////////////////////////
	// Public methods
	/////////////////////////////////////////////////////////////////////////		
	
	//
	//	Initialization
	//
	static void						Initialize (void);
	static void						Shutdown (void);

	//
	//	Path resolution
	//
	static void						Resolve_Paths (const Vector3 &camera_pos, uint32 milliseconds = 5);
	static PathSolveClass *		Peek_Active_Path (void)	{ return ActivePath; }

	//
	//	Save/Load
	//
	static void						Save (ChunkSaveClass &csave);
	static void						Load (ChunkLoadClass &cload);

	//
	//	Path management
	//
	static PathSolveClass *		Request_Path_Object (void);
	static void						Return_Path_Object (PathSolveClass *path);

private:

	/////////////////////////////////////////////////////////////////////////
	// Private methods
	/////////////////////////////////////////////////////////////////////////
	static void						Allocate_Objects (void);
	static void						Free_Objects (void);
	static void						Activate_New_Priority_Path (const Vector3 &camera_pos);

	/////////////////////////////////////////////////////////////////////////
	// Private member data
	/////////////////////////////////////////////////////////////////////////
	static DynamicVectorClass<PathSolveClass *>	AvailablePathList;
	static DynamicVectorClass<PathSolveClass *>	UsedPathList;
	static PathSolveClass *								ActivePath;
	static __int64											TicksPerMilliSec;
};

#endif //__PATHMGR_H
