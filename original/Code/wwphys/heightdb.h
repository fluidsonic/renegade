
#ifndef __HEIGHT_DB_H
#define __HEIGHT_DB_H

#include "vector.h"
#include "vector3.h"


/////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////
class MeshClass;
class RenderObjClass;
class ChunkSaveClass;
class ChunkLoadClass;

/////////////////////////////////////////////////////////////////////////
//
//	HeightDBClass
//
/////////////////////////////////////////////////////////////////////////
class HeightDBClass
{
public:

	/////////////////////////////////////////////////////////////////////////
	// Public constructors/destructors
	/////////////////////////////////////////////////////////////////////////
	HeightDBClass (void);
	virtual ~HeightDBClass (void);

	/////////////////////////////////////////////////////////////////////////
	// Public methods
	/////////////////////////////////////////////////////////////////////////
	
	//
	//	Initialization
	//
	static void			Initialize (void);
	static void			Shutdown (void);

	//
	//	Data access
	//
	static float		Get_Height (const Vector3 &pos);

	//
	//	Generation
	//
	static void			Generate (void);

	//
	// Save/load stuff
	//
	static bool			Save (ChunkSaveClass &csave);
	static bool			Load (ChunkLoadClass &cload);

protected:

	/////////////////////////////////////////////////////////////////////////
	// Protected methods
	/////////////////////////////////////////////////////////////////////////
	static bool			Load_Variables (ChunkLoadClass &cload);
	static void			Free_Data (void);
	static float *		Get_Height_Entry (int row, int col);

	static void			Process_Render_Obj (RenderObjClass *render_obj);
	static void			Submit_Mesh (MeshClass &mesh);
	static void			Examine_Level_Geometry (void);


	/////////////////////////////////////////////////////////////////////////
	// Protected member data
	/////////////////////////////////////////////////////////////////////////
	static float *		m_HeightArray;
	static int			m_NumPointsX;
	static int			m_NumPointsY;
	static float		m_PatchSize;
	static Vector3		m_LevelMin;
	static Vector3		m_LevelMax;
};


/////////////////////////////////////////////////////////////////////////
// Get_Height_Entry
/////////////////////////////////////////////////////////////////////////
inline float *
HeightDBClass::Get_Height_Entry (int row, int col)
{
	float *retval = NULL;

	//
	//	If the row and column are valid, then return the address of the
	// entry at this location.
	//
	if (row < m_NumPointsY && col < m_NumPointsX) {
		retval = &m_HeightArray[(row * m_NumPointsX) + col];
	}

	return retval;
}


#endif //__HEIGHT_DB_H
